package com.creditienda.service.shopify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.config.ShopifyMultiStoreProperties;
import com.creditienda.config.ShopifyMultiStoreProperties.ShopifyStoreConfig;
import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.RespuestaLoteBulkDTO;
import com.creditienda.dto.shopify.RespuestaMultiTiendaDTO;

@Service
public class ShopifyMultiStoreBulkService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyMultiStoreBulkService.class);

    private final ShopifyMultiStoreProperties multiStoreProperties;
    private final ShopifyTokenResolverService tokenResolver;
    private final RestTemplate restTemplate;

    /** Cache de locationId por alias de tienda */
    private final Map<String, String> locationCache = new ConcurrentHashMap<>();

    public ShopifyMultiStoreBulkService(
            ShopifyMultiStoreProperties multiStoreProperties,
            ShopifyTokenResolverService tokenResolver,
            RestTemplate restTemplate) {

        this.multiStoreProperties = multiStoreProperties;
        this.tokenResolver = tokenResolver;
        this.restTemplate = restTemplate;
    }

    // ================= API PÚBLICA =================

    public RespuestaMultiTiendaDTO actualizarProductosBulk(List<ProductoActualizarDTO> productos) {

        String loteId = UUID.randomUUID().toString();
        Map<String, RespuestaLoteBulkDTO> resultados = new LinkedHashMap<>();

        for (ShopifyStoreConfig store : multiStoreProperties.getStores()) {

            log.info("[MULTI-STORE] procesando tienda={} loteId={} productos={}",
                    store.getAlias(), loteId, productos.size());

            try {
                String token = tokenResolver.resolverToken(store);
                String locationId = obtenerLocationId(store, token);

                Set<String> exitosos = new HashSet<>();
                Set<String> fallidosPrecio = new HashSet<>();
                Set<String> fallidosInventario = new HashSet<>();

                for (List<ProductoActualizarDTO> chunk : partition(productos, store.getBulkChunkSize())) {
                    try {
                        procesarChunk(chunk, store, token, locationId,
                                exitosos, fallidosPrecio, fallidosInventario);
                    } catch (Exception e) {
                        log.error("[MULTI-STORE] error en chunk tienda={}", store.getAlias(), e);
                        chunk.forEach(p -> {
                            fallidosPrecio.add(p.getHandle());
                            fallidosInventario.add(p.getHandle());
                        });
                    }
                }

                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        new ArrayList<>(exitosos),
                        new ArrayList<>(fallidosPrecio),
                        new ArrayList<>(fallidosInventario),
                        productos.size(),
                        String.format("✅ %d | ❌ precio: %d | ❌ inventario: %d",
                                exitosos.size(), fallidosPrecio.size(), fallidosInventario.size())));

            } catch (Exception e) {
                log.error("[MULTI-STORE] error general tienda={}", store.getAlias(), e);
                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        List.of(), List.of(), List.of(), productos.size(),
                        "❌ Error al procesar tienda: " + e.getMessage()));
            }
        }

        return new RespuestaMultiTiendaDTO(loteId, resultados);
    }

    public RespuestaMultiTiendaDTO actualizarPreciosBulk(List<ProductoActualizarDTO> productos) {

        String loteId = UUID.randomUUID().toString();
        Map<String, RespuestaLoteBulkDTO> resultados = new LinkedHashMap<>();

        for (ShopifyStoreConfig store : multiStoreProperties.getStores()) {

            log.info("[MULTI-STORE] PRECIO tienda={} loteId={}", store.getAlias(), loteId);

            try {
                String token = tokenResolver.resolverToken(store);

                Set<String> exitosos = new HashSet<>();
                Set<String> fallidosPrecio = new HashSet<>();

                for (List<ProductoActualizarDTO> chunk : partition(productos, store.getBulkChunkSize())) {
                    try {
                        Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk, store, token);
                        ResultadoOperacion precio = actualizarPrecios(chunk, ids, store, token);
                        exitosos.addAll(precio.exitosos);
                        fallidosPrecio.addAll(precio.fallidos);
                    } catch (Exception e) {
                        log.error("[MULTI-STORE] error chunk precio tienda={}", store.getAlias(), e);
                        chunk.forEach(p -> fallidosPrecio.add(p.getHandle()));
                    }
                }

                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        new ArrayList<>(exitosos),
                        new ArrayList<>(fallidosPrecio),
                        List.of(),
                        productos.size(),
                        String.format("✅ %d | ❌ precio: %d", exitosos.size(), fallidosPrecio.size())));

            } catch (Exception e) {
                log.error("[MULTI-STORE] error general precio tienda={}", store.getAlias(), e);
                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        List.of(), List.of(), List.of(), productos.size(),
                        "❌ Error al procesar tienda: " + e.getMessage()));
            }
        }

        return new RespuestaMultiTiendaDTO(loteId, resultados);
    }

    public RespuestaMultiTiendaDTO actualizarInventarioBulk(List<ProductoActualizarDTO> productos) {

        String loteId = UUID.randomUUID().toString();
        Map<String, RespuestaLoteBulkDTO> resultados = new LinkedHashMap<>();

        for (ShopifyStoreConfig store : multiStoreProperties.getStores()) {

            log.info("[MULTI-STORE] INVENTARIO tienda={} loteId={}", store.getAlias(), loteId);

            try {
                String token = tokenResolver.resolverToken(store);
                String locationId = obtenerLocationId(store, token);

                Set<String> exitosos = new HashSet<>();
                Set<String> fallidosInventario = new HashSet<>();

                for (List<ProductoActualizarDTO> chunk : partition(productos, store.getBulkChunkSize())) {
                    try {
                        Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk, store, token);
                        ResultadoOperacion inventario = actualizarInventario(chunk, ids, locationId, store, token);
                        exitosos.addAll(inventario.exitosos);
                        fallidosInventario.addAll(inventario.fallidos);
                    } catch (Exception e) {
                        log.error("[MULTI-STORE] error chunk inventario tienda={}", store.getAlias(), e);
                        chunk.forEach(p -> fallidosInventario.add(p.getHandle()));
                    }
                }

                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        new ArrayList<>(exitosos),
                        List.of(),
                        new ArrayList<>(fallidosInventario),
                        productos.size(),
                        String.format("✅ %d | ❌ inventario: %d", exitosos.size(), fallidosInventario.size())));

            } catch (Exception e) {
                log.error("[MULTI-STORE] error general inventario tienda={}", store.getAlias(), e);
                resultados.put(store.getDomain(), new RespuestaLoteBulkDTO(
                        List.of(), List.of(), List.of(), productos.size(),
                        "❌ Error al procesar tienda: " + e.getMessage()));
            }
        }

        return new RespuestaMultiTiendaDTO(loteId, resultados);
    }

    // ================= CORE =================

    private void procesarChunk(
            List<ProductoActualizarDTO> chunk,
            ShopifyStoreConfig store,
            String token,
            String locationId,
            Set<String> exitosos,
            Set<String> fallidosPrecio,
            Set<String> fallidosInventario) {

        Map<String, ShopifyIds> ids = obtenerIdsPorHandle(chunk, store, token);
        ResultadoOperacion precio = actualizarPrecios(chunk, ids, store, token);
        ResultadoOperacion inventario = actualizarInventario(chunk, ids, locationId, store, token);

        for (ProductoActualizarDTO dto : chunk) {
            String h = dto.getHandle();

            if (precio.exitosos.contains(h) && inventario.exitosos.contains(h)
                    && !precio.fallidos.contains(h) && !inventario.fallidos.contains(h)) {
                exitosos.add(h);
            }
            if (precio.fallidos.contains(h)) fallidosPrecio.add(h);
            if (inventario.fallidos.contains(h)) fallidosInventario.add(h);
        }
    }

    // ================= LOCATION =================

    private String obtenerLocationId(ShopifyStoreConfig store, String token) {

        return locationCache.computeIfAbsent(store.getAlias(), alias -> {

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", token);

            ResponseEntity<Map> response = restTemplate.exchange(
                    String.format("https://%s/admin/api/%s/locations.json",
                            store.getDomain(), store.getApiVersion()),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            List<Map<String, Object>> locations =
                    (List<Map<String, Object>>) response.getBody().get("locations");

            if (locations == null || locations.isEmpty()) {
                throw new RuntimeException("No se encontraron locations en tienda=" + alias);
            }

            Map<String, Object> location = locations.stream()
                    .filter(l -> Boolean.TRUE.equals(l.get("active"))
                            && !Boolean.TRUE.equals(l.get("legacy")))
                    .findFirst()
                    .orElse(locations.get(0));

            String locationId = "gid://shopify/Location/" + location.get("id").toString();

            log.info("[MULTI-STORE] locationId={} tienda={}", locationId, alias);

            return locationId;
        });
    }

    // ================= GRAPHQL 1: LOOKUP =================

    private Map<String, ShopifyIds> obtenerIdsPorHandle(
            List<ProductoActualizarDTO> chunk,
            ShopifyStoreConfig store,
            String token) {

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
                              inventoryItem {
                                id
                                inventoryLevels(first: 1) {
                                  edges {
                                    node {
                                      location { id }
                                    }
                                  }
                                }
                              }
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

        Map<String, Object> response = ejecutarGraphQL(body, store, token);
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

            Map inventoryItem = (Map) variantNode.get("inventoryItem");
            ids.inventoryItemId = inventoryItem.get("id").toString();

            // location ya asociado al inventory item en esta tienda
            try {
                List<Map> invLevels = (List<Map>) ((Map) inventoryItem.get("inventoryLevels")).get("edges");
                if (invLevels != null && !invLevels.isEmpty()) {
                    ids.locationId = ((Map) ((Map) invLevels.get(0).get("node")).get("location")).get("id").toString();
                }
            } catch (Exception e) {
                log.warn("[MULTI-STORE] no se pudo obtener locationId del inventoryItem handle={}", handle);
            }

            result.put(handle, ids);
        }

        return result;
    }

    // ================= GRAPHQL 2: PRECIOS =================

    private ResultadoOperacion actualizarPrecios(
            List<ProductoActualizarDTO> chunk,
            Map<String, ShopifyIds> ids,
            ShopifyStoreConfig store,
            String token) {

        StringBuilder mutation = new StringBuilder("mutation {");
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
                    """.formatted(alias, s.productId, s.variantId,
                    dto.getPrecio(), dto.getCompareAtPrice()));
            i++;
        }
        mutation.append("}");

        if (aliasToHandle.isEmpty()) {
            return new ResultadoOperacion(exitosos, fallidos);
        }

        Map<String, Object> response = ejecutarGraphQL(Map.of("query", mutation.toString()), store, token);

        if (response.containsKey("errors")) {
            log.error("[MULTI-STORE] PRICE ERRORS tienda={} → {}", store.getAlias(), response.get("errors"));
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            aliasToHandle.values().forEach(fallidos::add);
            return new ResultadoOperacion(exitosos, fallidos);
        }

        data.forEach((alias, value) -> {
            Map<String, Object> r = (Map<String, Object>) value;
            List<Map<String, Object>> errors = (List<Map<String, Object>>) r.get("userErrors");
            String handle = aliasToHandle.get(alias);
            if (errors != null && !errors.isEmpty()) {
                log.error("[MULTI-STORE] PRICE ERROR tienda={} handle={} errors={}", store.getAlias(), handle, errors);
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
            String locationId,
            ShopifyStoreConfig store,
            String token) {

        Set<String> exitosos = new HashSet<>();
        Set<String> fallidos = new HashSet<>();
        List<Map<String, Object>> quantities = new ArrayList<>();

        for (ProductoActualizarDTO dto : chunk) {
            ShopifyIds s = ids.get(dto.getHandle().toLowerCase());
            if (s == null) {
                fallidos.add(dto.getHandle());
                continue;
            }

            // usar el locationId propio del producto; fallback al de la tienda
            String itemLocationId = (s.locationId != null) ? s.locationId : locationId;

            if (s.locationId == null) {
                log.warn("[MULTI-STORE] handle={} sin locationId propio, usando location de tienda={}",
                        dto.getHandle(), locationId);
            }

            Map<String, Object> q = new HashMap<>();
            q.put("inventoryItemId", s.inventoryItemId);
            q.put("locationId", itemLocationId);
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

        Map<String, Object> body = Map.of(
                "query", mutation,
                "variables", Map.of("input", input));

        Map<String, Object> response = ejecutarGraphQL(body, store, token);

        if (response.containsKey("errors")) {
            log.error("[MULTI-STORE] INVENTORY ERRORS tienda={} → {}", store.getAlias(), response.get("errors"));
            chunk.forEach(p -> fallidos.add(p.getHandle()));
            return new ResultadoOperacion(exitosos, fallidos);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> result = (Map<String, Object>) data.get("inventorySetQuantities");
        List<Map<String, Object>> userErrors = (List<Map<String, Object>>) result.get("userErrors");

        if (userErrors != null && !userErrors.isEmpty()) {

            log.error("[MULTI-STORE] INVENTORY userErrors tienda={} → {}", store.getAlias(), userErrors);

            for (Map<String, Object> error : userErrors) {
                List<String> field = (List<String>) error.get("field");
                log.error("[MULTI-STORE] INVENTORY userError field={} message={}", field, error.get("message"));
                if (field != null && field.size() >= 3) {
                    try {
                        int index = Integer.parseInt(field.get(2));
                        fallidos.add(chunk.get(index).getHandle());
                    } catch (Exception ex) {
                        log.warn("[MULTI-STORE] no se pudo mapear error inventario por índice", ex);
                        chunk.forEach(p -> fallidos.add(p.getHandle()));
                    }
                } else {
                    chunk.forEach(p -> fallidos.add(p.getHandle()));
                }
            }
            chunk.forEach(p -> {
                if (!fallidos.contains(p.getHandle())) exitosos.add(p.getHandle());
            });

        } else {
            chunk.forEach(p -> exitosos.add(p.getHandle()));
        }

        return new ResultadoOperacion(exitosos, fallidos);
    }

    // ================= EJECUTOR GRAPHQL =================

    private Map<String, Object> ejecutarGraphQL(
            Map<String, Object> body,
            ShopifyStoreConfig store,
            String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", token);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                String.format("https://%s/admin/api/%s/graphql.json",
                        store.getDomain(), store.getApiVersion()),
                new HttpEntity<>(body, headers),
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error GraphQL Shopify tienda=" + store.getAlias());
        }

        Map<String, Object> responseBody = response.getBody();

        try {
            if (responseBody.containsKey("extensions")) {
                Map<String, Object> extensions = (Map<String, Object>) responseBody.get("extensions");
                Map<String, Object> cost = (Map<String, Object>) extensions.get("cost");
                Map<String, Object> throttle = (Map<String, Object>) cost.get("throttleStatus");
                Number available = (Number) throttle.get("currentlyAvailable");

                log.info("[MULTI-STORE] throttle tienda={} available={}", store.getAlias(), available);

                if (available != null && available.doubleValue() < 200) {
                    log.warn("[MULTI-STORE] throttle bajo tienda={}. Sleep 400ms", store.getAlias());
                    Thread.sleep(400);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("[MULTI-STORE] no se pudo leer throttleStatus tienda={}", store.getAlias());
        }

        return responseBody;
    }

    // ================= HELPERS =================

    private List<List<ProductoActualizarDTO>> partition(List<ProductoActualizarDTO> list, int size) {
        List<List<ProductoActualizarDTO>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    // ================= DTOs INTERNOS =================

    static class ShopifyIds {
        String productId;
        String variantId;
        String inventoryItemId;
        String locationId;
    }

    static class ResultadoOperacion {
        Set<String> exitosos;
        Set<String> fallidos;

        ResultadoOperacion(Set<String> exitosos, Set<String> fallidos) {
            this.exitosos = exitosos;
            this.fallidos = fallidos;
        }
    }
}
