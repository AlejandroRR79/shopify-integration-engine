package com.creditienda.service.shopify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.RespuestaLoteBulkDTO;

@Service
public class ShopifyProductosBulkService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyProductosBulkService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    @Value("${shopify.bulk.chunk-size:20}")
    private int bulkChunkSize;

    @Autowired
    private RestTemplate restTemplate;

    // 🔒 cache location
    private volatile String cachedLocationId;

    // ================= API PÚBLICA =================

    public RespuestaLoteBulkDTO actualizarProductosBulk(
            List<ProductoActualizarDTO> productos) {

        String locationId = obtenerLocationId();

        Set<String> exitosos = new HashSet<>();
        Set<String> fallidosPrecio = new HashSet<>();
        Set<String> fallidosInventario = new HashSet<>();

        for (List<ProductoActualizarDTO> chunk : partition(productos, bulkChunkSize)) {
            try {
                procesarChunk(
                        chunk,
                        locationId,
                        exitosos,
                        fallidosPrecio,
                        fallidosInventario);
            } catch (Exception e) {
                log.error("❌ Error en chunk completo", e);
                chunk.forEach(p -> {
                    fallidosPrecio.add(p.getHandle());
                    fallidosInventario.add(p.getHandle());
                });

            }
        }
        return new RespuestaLoteBulkDTO(
                new ArrayList<>(exitosos),
                new ArrayList<>(fallidosPrecio),
                new ArrayList<>(fallidosInventario),
                productos.size(),
                String.format(
                        "✅ %d | ❌ precio: %d | ❌ inventario: %d",
                        exitosos.size(),
                        fallidosPrecio.size(),
                        fallidosInventario.size()));

    }

    public RespuestaLoteBulkDTO actualizarPreciosBulk(
            List<ProductoActualizarDTO> productos) {

        Set<String> exitosos = new HashSet<>();
        Set<String> fallidosPrecio = new HashSet<>();

        for (List<ProductoActualizarDTO> chunk : partition(productos, bulkChunkSize)) {

            try {
                Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk);
                ResultadoOperacion precio = actualizarPrecios(chunk, ids);

                exitosos.addAll(precio.exitosos);
                fallidosPrecio.addAll(precio.fallidos);

            } catch (Exception e) {
                log.error("❌ Error en chunk precio", e);
                chunk.forEach(p -> fallidosPrecio.add(p.getHandle()));
            }
        }

        return new RespuestaLoteBulkDTO(
                new ArrayList<>(exitosos),
                new ArrayList<>(fallidosPrecio),
                new ArrayList<>(),
                productos.size(),
                String.format("✅ %d | ❌ precio: %d",
                        exitosos.size(),
                        fallidosPrecio.size()));
    }

    public RespuestaLoteBulkDTO actualizarInventarioBulk(
            List<ProductoActualizarDTO> productos) {

        String locationId = obtenerLocationId();

        Set<String> exitosos = new HashSet<>();
        Set<String> fallidosInventario = new HashSet<>();

        for (List<ProductoActualizarDTO> chunk : partition(productos, bulkChunkSize)) {

            try {
                Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk);
                ResultadoOperacion inventario = actualizarInventario(chunk, ids, locationId);

                exitosos.addAll(inventario.exitosos);
                fallidosInventario.addAll(inventario.fallidos);

            } catch (Exception e) {
                log.error("❌ Error en chunk inventario", e);
                chunk.forEach(p -> fallidosInventario.add(p.getHandle()));
            }
        }

        return new RespuestaLoteBulkDTO(
                new ArrayList<>(exitosos),
                new ArrayList<>(),
                new ArrayList<>(fallidosInventario),
                productos.size(),
                String.format("✅ %d | ❌ inventario: %d",
                        exitosos.size(),
                        fallidosInventario.size()));
    }

    // ================= CORE =================

    private void procesarChunk(
            List<ProductoActualizarDTO> chunk,
            String locationId,
            Set<String> exitosos,
            Set<String> fallidosPrecio,
            Set<String> fallidosInventario) {

        Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk);
        ResultadoOperacion precio = actualizarPrecios(chunk, ids);
        ResultadoOperacion inventario = actualizarInventario(chunk, ids, locationId);

        for (ProductoActualizarDTO dto : chunk) {
            String h = dto.getHandle();

            if (precio.exitosos.contains(h)
                    && inventario.exitosos.contains(h)
                    && !precio.fallidos.contains(h)
                    && !inventario.fallidos.contains(h)) {
                exitosos.add(h);
            }

            if (precio.fallidos.contains(h)) {
                fallidosPrecio.add(h);
            }

            if (inventario.fallidos.contains(h)) {
                fallidosInventario.add(h);
            }

        }

    }

    // ================= LOCATION =================

    private String obtenerLocationId() {
        if (cachedLocationId != null)
            return cachedLocationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                String.format(
                        "https://%s/admin/api/%s/locations.json",
                        shopDomain, apiVersion),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        List<Map<String, Object>> locations = (List<Map<String, Object>>) response.getBody().get("locations");
        if (locations == null || locations.isEmpty()) {
            throw new RuntimeException("No se encontraron locations en Shopify");
        }

        cachedLocationId = "gid://shopify/Location/" + locations.get(0).get("id").toString();

        log.info("📍 locationId={}", cachedLocationId);
        return cachedLocationId;
    }

    // ================= GRAPHQL 1: LOOKUP =================

    private Map<String, ShopifyIds> obtenerIdsPorHandle(
            List<ProductoActualizarDTO> chunk) {

        String handlesQuery = chunk.stream()
                .map(p -> "handle:" + p.getHandle().toLowerCase())
                .collect(Collectors.joining(" OR "));

        String query = """
                query getProducts($query: String!) {
                  products(first: 100, query: $query) {
                    edges {
                      node {
                        id
                        handle
                        variants(first: 1) {
                          edges {
                            node {
                              id
                              inventoryItem { id }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Map<String, Object> body = Map.of(
                "query", query,
                "variables", Map.of("query", handlesQuery));

        Map<String, Object> response = ejecutarGraphQL(body);

        Map<String, ShopifyIds> result = new HashMap<>();

        Map data = (Map) response.get("data");
        Map products = (Map) data.get("products");
        List<Map> edges = (List<Map>) products.get("edges");

        for (Map edge : edges) {
            Map node = (Map) edge.get("node");
            String handle = node.get("handle").toString().toLowerCase();

            Map variantEdge = (Map) ((List<Map>) ((Map) node.get("variants"))
                    .get("edges")).get(0);

            Map variantNode = (Map) variantEdge.get("node");

            ShopifyIds ids = new ShopifyIds();
            ids.productId = node.get("id").toString();
            ids.variantId = variantNode.get("id").toString();
            ids.inventoryItemId = ((Map) variantNode.get("inventoryItem"))
                    .get("id").toString();

            result.put(handle, ids);
        }
        return result;
    }

    // ================= GRAPHQL 2: PRECIOS =================

    private ResultadoOperacion actualizarPrecios(
            List<ProductoActualizarDTO> chunk,
            Map<String, ShopifyIds> ids) {

        StringBuilder mutation = new StringBuilder("mutation {");

        // Relación alias → handle
        Map<String, String> aliasToHandle = new HashMap<>();
        Set<String> exitosos = new HashSet<>();
        Set<String> fallidos = new HashSet<>();

        int i = 1;
        for (ProductoActualizarDTO dto : chunk) {
            ShopifyIds s = ids.get(dto.getHandle().toLowerCase());
            if (s == null) {
                fallidos.add(dto.getHandle());
                continue;
            }

            String alias = "p" + i;
            aliasToHandle.put(alias, dto.getHandle());

            mutation.append("""
                    %s: productVariantsBulkUpdate(
                      productId: "%s",
                      variants: [{
                        id: "%s",
                        price: "%s",
                        compareAtPrice: "%s"
                      }]
                    ) {
                      userErrors { field message }
                    }
                    """.formatted(
                    alias,
                    s.productId,
                    s.variantId,
                    dto.getPrecio(),
                    dto.getCompareAtPrice()));

            i++;
        }
        mutation.append("}");

        if (aliasToHandle.isEmpty()) {
            log.warn("⚠️ PRICE: ningún producto válido para actualizar en este chunk");
            return new ResultadoOperacion(exitosos, fallidos);
        }

        // 🔎 LOG — mutation exacta (para Postman)
        log.info("📤 SHOPIFY PRICE BULK MUTATION ↓↓↓");
        // log.info("📤 SHOPIFY PRICES BULK MUTATION:
        // {}",compactarGraphQL(mutation.toString()));

        Map<String, Object> response = ejecutarGraphQL(Map.of("query", mutation.toString()));

        // 🔎 LOG — response cruda
        log.info("📥 SHOPIFY PRICE BULK RESPONSE ↓↓↓");
        // log.info(response.toString());

        // ❌ Error global GraphQL
        if (response.containsKey("errors")) {
            log.error("❌ GRAPHQL PRICE ERRORS → {}", response.get("errors"));
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");

        if (data == null) {
            log.error("❌ GraphQL response sin data");
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }
        // ✅ Evaluación por item
        data.forEach((alias, value) -> {
            Map<String, Object> r = (Map<String, Object>) value;
            List<Map<String, Object>> errors = (List<Map<String, Object>>) r.get("userErrors");

            String handle = aliasToHandle.get(alias);

            if (errors != null && !errors.isEmpty()) {
                log.error("❌ PRICE ERROR → handle={} errors={}", handle, errors);
                fallidos.add(handle);
            } else {
                exitosos.add(handle);
            }
        });

        return new ResultadoOperacion(exitosos, fallidos);
    }

    // ================= GRAPHQL 3: INVENTARIO =================
    private ResultadoOperacion actualizarInventario(
            List<ProductoActualizarDTO> chunk,
            Map<String, ShopifyIds> ids,
            String locationId) {

        Set<String> exitosos = new HashSet<>();
        Set<String> fallidos = new HashSet<>();

        List<Map<String, Object>> quantities = new ArrayList<>();

        for (ProductoActualizarDTO dto : chunk) {
            ShopifyIds s = ids.get(dto.getHandle().toLowerCase());

            if (s == null) {
                fallidos.add(dto.getHandle());
                continue;
            }

            Map<String, Object> q = new HashMap<>();
            q.put("inventoryItemId", s.inventoryItemId);
            q.put("locationId", locationId);
            q.put("quantity", dto.getCantidad());

            quantities.add(q);
        }

        if (quantities.isEmpty()) {
            return new ResultadoOperacion(exitosos, fallidos);
        }

        String mutation = """
                mutation inventoryBulk($input: InventorySetQuantitiesInput!) {
                  inventorySetQuantities(input: $input) {
                    inventoryAdjustmentGroup {
                      changes { name delta }
                    }
                    userErrors { field message }
                  }
                }
                """;

        Map<String, Object> input = new HashMap<>();
        input.put("reason", "correction");
        input.put("name", "available");
        input.put("ignoreCompareQuantity", true);
        input.put("quantities", quantities);

        Map<String, Object> variables = Map.of("input", input);

        Map<String, Object> body = Map.of(
                "query", mutation,
                "variables", variables);

        log.info("📤 INVENTORY BULK OPTIMIZED ↓↓↓");
        log.info(compactarGraphQL(mutation));

        Map<String, Object> response = ejecutarGraphQL(body);

        if (response.containsKey("errors")) {
            log.error("❌ GRAPHQL INVENTORY ERRORS → {}", response.get("errors"));
            chunk.forEach(p -> fallidos.add(p.getHandle()));
            return new ResultadoOperacion(exitosos, fallidos);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> result = (Map<String, Object>) data.get("inventorySetQuantities");

        List<Map<String, Object>> userErrors = (List<Map<String, Object>>) result.get("userErrors");

        if (userErrors != null && !userErrors.isEmpty()) {

            for (Map<String, Object> error : userErrors) {

                List<String> field = (List<String>) error.get("field");

                if (field != null && field.size() >= 3) {

                    try {
                        int index = Integer.parseInt(field.get(2));
                        ProductoActualizarDTO dto = chunk.get(index);
                        fallidos.add(dto.getHandle());

                        log.error("❌ INVENTORY ERROR → handle={} msg={}",
                                dto.getHandle(),
                                error.get("message"));

                    } catch (Exception ex) {
                        log.warn("⚠ No se pudo mapear error por índice", ex);
                    }
                }
            }

            // Los que NO están en fallidos son exitosos
            for (ProductoActualizarDTO dto : chunk) {
                if (!fallidos.contains(dto.getHandle())) {
                    exitosos.add(dto.getHandle());
                }
            }

        } else {
            chunk.forEach(p -> exitosos.add(p.getHandle()));
        }

        return new ResultadoOperacion(exitosos, fallidos);
    }

    // ================= EJECUTOR GRAPHQL =================

    private Map<String, Object> ejecutarGraphQL(Map<String, Object> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                String.format(
                        "https://%s/admin/api/%s/graphql.json",
                        shopDomain, apiVersion),
                new HttpEntity<>(body, headers),
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            throw new RuntimeException("Error GraphQL Shopify");
        }

        Map<String, Object> responseBody = response.getBody();

        // 🔥 PROTECCIÓN RATE LIMIT
        try {
            if (responseBody.containsKey("extensions")) {

                Map<String, Object> extensions = (Map<String, Object>) responseBody.get("extensions");

                Map<String, Object> cost = (Map<String, Object>) extensions.get("cost");

                Map<String, Object> throttle = (Map<String, Object>) cost.get("throttleStatus");

                Number available = (Number) throttle.get("currentlyAvailable");

                Number restoreRate = (Number) throttle.get("restoreRate");

                log.info("📊 Shopify throttle → available={}, restoreRate={}",
                        available, restoreRate);

                // 🔒 Si el bucket baja demasiado, espera
                if (available != null && available.doubleValue() < 200) {

                    long sleepMs = 400; // puedes ajustar dinámicamente si quieres
                    log.warn("⚠ Throttle bajo ({}). Sleep {} ms",
                            available, sleepMs);

                    Thread.sleep(sleepMs);
                }
            }
        } catch (Exception e) {
            log.warn("⚠ No se pudo leer throttleStatus", e);
        }

        return responseBody;
    }

    // ================= HELPERS =================

    private List<List<ProductoActualizarDTO>> partition(
            List<ProductoActualizarDTO> list, int size) {

        List<List<ProductoActualizarDTO>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i,
                    Math.min(i + size, list.size())));
        }
        return parts;
    }

    // ================= DTO INTERNO =================

    static class ShopifyIds {
        String productId;
        String variantId;
        String inventoryItemId;
    }

    static class ResultadoOperacion {
        Set<String> exitosos;
        Set<String> fallidos;

        ResultadoOperacion(Set<String> exitosos, Set<String> fallidos) {
            this.exitosos = exitosos;
            this.fallidos = fallidos;
        }
    }

    private String compactarGraphQL(String gql) {
        return gql
                .replaceAll("[\\n\\r]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

}
