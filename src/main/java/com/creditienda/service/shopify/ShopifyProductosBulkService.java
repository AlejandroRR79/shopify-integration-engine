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

        for (List<ProductoActualizarDTO> chunk : partition(productos, 20)) {
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
                .map(p -> "handle:" + p.getHandle())
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
            String handle = node.get("handle").toString();

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
            ShopifyIds s = ids.get(dto.getHandle());
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

        StringBuilder mutation = new StringBuilder("mutation {");

        Map<String, String> aliasToHandle = new HashMap<>();
        Set<String> exitosos = new HashSet<>();
        Set<String> fallidos = new HashSet<>();

        int i = 1;
        for (ProductoActualizarDTO dto : chunk) {
            ShopifyIds s = ids.get(dto.getHandle());
            if (s == null) {
                fallidos.add(dto.getHandle());
                continue;
            }

            String alias = "i" + i++;
            aliasToHandle.put(alias, dto.getHandle());

            mutation.append("""
                    %s: inventorySetQuantities(input: {
                      reason: "correction",
                      name: "available",
                      ignoreCompareQuantity: true,
                      quantities: [{
                        inventoryItemId: "%s",
                        locationId: "%s",
                        quantity: %d
                      }]
                    }) {
                      inventoryAdjustmentGroup {
                        reason
                        changes { name delta }
                      }
                      userErrors { field message }
                    }
                    """.formatted(
                    alias,
                    s.inventoryItemId,
                    locationId,
                    dto.getCantidad()));
        }
        mutation.append("}");

        if (aliasToHandle.isEmpty()) {
            log.warn("⚠️ PRICE: ningún producto válido para actualizar en este chunk");
            return new ResultadoOperacion(exitosos, fallidos);
        }

        // 🔎 LOG para Postman
        log.info("📤 SHOPIFY INVENTORY BULK MUTATION ↓↓↓");
        log.info("📤 SHOPIFY INVENTORY BULK MUTATION: {}",
                compactarGraphQL(mutation.toString()));

        Map<String, Object> response = ejecutarGraphQL(Map.of("query", mutation.toString()));

        log.info("📥 SHOPIFY INVENTORY BULK RESPONSE ↓↓↓");
        // log.info(response.toString());

        // ❌ Error global GraphQL
        if (response.containsKey("errors")) {
            log.error("❌ GRAPHQL INVENTORY ERRORS → {}", response.get("errors"));
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        if (data == null) {
            log.error("❌ GraphQL response sin data");
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }

        data.forEach((alias, value) -> {
            Map<String, Object> r = (Map<String, Object>) value;
            List<Map<String, Object>> errors = (List<Map<String, Object>>) r.get("userErrors");

            Object adj = r.get("inventoryAdjustmentGroup");
            String handle = aliasToHandle.get(alias);

            if (errors != null && !errors.isEmpty()) {
                log.error("❌ INVENTORY ERROR → handle={} errors={}", handle, errors);
                fallidos.add(handle);
            } else if (adj == null) {
                // NOOP = ya estaba correcto → ÉXITO
                exitosos.add(handle);
            } else {
                exitosos.add(handle);
            }
        });

        return new ResultadoOperacion(exitosos, fallidos);
    }

    // ================= EJECUTOR GRAPHQL =================

    private Map<String, Object> ejecutarGraphQL(
            Map<String, Object> body) {

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
        return response.getBody();
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
