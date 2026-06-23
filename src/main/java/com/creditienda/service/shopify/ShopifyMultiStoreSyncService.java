package com.creditienda.service.shopify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.config.ShopifyMultiStoreProperties;
import com.creditienda.config.ShopifyMultiStoreProperties.ShopifyStoreConfig;
import com.creditienda.service.b2b.B2BService;
import com.creditienda.service.b2b.B2BTokenService;
import com.creditienda.util.ShopifyOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ShopifyMultiStoreSyncService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyMultiStoreSyncService.class);

    private final ShopifyMultiStoreProperties multiStoreProperties;
    private final ShopifyTokenResolverService tokenResolver;
    private final RestTemplate restTemplate;
    private final B2BService b2bService;
    private final B2BTokenService b2bTokenService;
    private final ObjectMapper objectMapper;

    public ShopifyMultiStoreSyncService(
            ShopifyMultiStoreProperties multiStoreProperties,
            ShopifyTokenResolverService tokenResolver,
            RestTemplate restTemplate,
            B2BService b2bService,
            B2BTokenService b2bTokenService,
            ObjectMapper objectMapper) {

        this.multiStoreProperties = multiStoreProperties;
        this.tokenResolver = tokenResolver;
        this.restTemplate = restTemplate;
        this.b2bService = b2bService;
        this.b2bTokenService = b2bTokenService;
        this.objectMapper = objectMapper;
    }

    // ================= API PUBLICA =================

    public Map<String, Object> sincronizarPorFechas(String aliasTienda, LocalDate inicio, LocalDate fin) {
        return sincronizarPorFechas(aliasTienda, inicio, fin, false);
    }

    public Map<String, Object> sincronizarPorFechas(String aliasTienda, LocalDate inicio, LocalDate fin, boolean soloConsulta) {

        ShopifyStoreConfig store = resolverTienda(aliasTienda);
        String token = resolverTokenOrdenes(store);

        log.info("[MULTI-SYNC] tienda={} rango={} a {} soloConsulta={}", store.getDomain(), inicio, fin, soloConsulta);

        List<Map<String, Object>> ordenes = obtenerOrdenesEntreFechas(inicio, fin, store, token);

        if (soloConsulta) {
            List<String> numerosOrden = ordenes.stream()
                    .map(o -> String.valueOf(o.get("id")))
                    .collect(Collectors.toList());

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("tienda", store.getDomain());
            r.put("soloConsulta", true);
            r.put("totalOrdenes", ordenes.size());
            r.put("ordenes", numerosOrden);
            return r;
        }

        List<String> exitosas = new ArrayList<>();
        Map<String, String> fallidas = new LinkedHashMap<>();

        if (ordenes.isEmpty()) {
            return resultado(store.getDomain(), exitosas, fallidas, "No se encontraron ordenes en el rango indicado.");
        }

        String b2bToken;
        try {
            b2bToken = b2bTokenService.obtenerTokenOC();
        } catch (Exception e) {
            log.error("[MULTI-SYNC] Error al obtener token B2B: {}", e.getMessage());
            return resultado(store.getDomain(), exitosas, fallidas, "Error al obtener token B2B: " + e.getMessage());
        }

        for (Map<String, Object> orden : ordenes) {
            String numeroOC = String.valueOf(orden.get("id"));
            try {
                Map<String, Object> registro = ShopifyOrderMapper.transformar(orden);
                String jsonRegistro = objectMapper.writeValueAsString(registro);

                log.info("[MULTI-SYNC] procesando orden={} tienda={}", numeroOC, store.getDomain());

                boolean enviado = b2bService.enviarOrden(jsonRegistro, b2bToken, false);

                if (enviado) {
                    exitosas.add(numeroOC);
                } else {
                    fallidas.put(numeroOC, "Orden ya registrada");
                }
            } catch (Exception e) {
                log.error("[MULTI-SYNC] error orden={} tienda={} msg={}", numeroOC, store.getDomain(), e.getMessage());
                fallidas.put(numeroOC, e.getMessage() != null ? e.getMessage() : "Error desconocido");
            }
        }

        log.info("[MULTI-SYNC] completado tienda={} exitosas={} fallidas={}",
                store.getDomain(), exitosas.size(), fallidas.size());

        return resultado(store.getDomain(), exitosas, fallidas, null);
    }

    public Map<String, Object> sincronizarUnaOrden(String aliasTienda, String ordenId) {
        return sincronizarUnaOrden(aliasTienda, ordenId, false);
    }

    public Map<String, Object> sincronizarUnaOrden(String aliasTienda, String ordenId, boolean soloConsulta) {

        ShopifyStoreConfig store = resolverTienda(aliasTienda);
        String token = resolverTokenOrdenes(store);

        log.info("[MULTI-SYNC] orden unica tienda={} ordenId={} soloConsulta={}", store.getDomain(), ordenId, soloConsulta);

        List<String> exitosas = new ArrayList<>();
        Map<String, String> fallidas = new LinkedHashMap<>();

        try {
            Map<String, Object> orden = obtenerOrdenPorId(ordenId, store, token);

            if (orden == null) {
                fallidas.put(ordenId, "Orden no encontrada en Shopify");
                return resultado(store.getDomain(), exitosas, fallidas, null);
            }

            if (soloConsulta) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("tienda", store.getDomain());
                r.put("soloConsulta", true);
                r.put("orden", orden);
                return r;
            }

            String b2bToken = b2bTokenService.obtenerTokenOC();
            Map<String, Object> registro = ShopifyOrderMapper.transformar(orden);
            String jsonRegistro = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(registro);

            boolean enviado = b2bService.enviarOrden(jsonRegistro, b2bToken, true);

            if (enviado) {
                exitosas.add(ordenId);
            } else {
                fallidas.put(ordenId, "Orden ya registrada");
            }

        } catch (Exception e) {
            log.error("[MULTI-SYNC] error orden={} tienda={} msg={}", ordenId, store.getDomain(), e.getMessage());
            fallidas.put(ordenId, e.getMessage() != null ? e.getMessage() : "Error desconocido");
        }

        return resultado(store.getDomain(), exitosas, fallidas, null);
    }

    // ================= CONSULTAS SHOPIFY (GraphQL) =================

    private static final String ORDER_FIELDS =
            "{ id name number email phone createdAt processedAt updatedAt " +
            "cancelledAt cancelReason closedAt displayFinancialStatus displayFulfillmentStatus " +
            "tags note test confirmed taxesIncluded currencyCode presentmentCurrencyCode " +
            "totalPriceSet { shopMoney { amount currencyCode } presentmentMoney { amount currencyCode } } " +
            "subtotalPriceSet { shopMoney { amount currencyCode } presentmentMoney { amount currencyCode } } " +
            "totalTaxSet { shopMoney { amount currencyCode } } " +
            "totalDiscountsSet { shopMoney { amount currencyCode } } " +
            "totalShippingPriceSet { shopMoney { amount currencyCode } } " +
            "currentTotalPriceSet { shopMoney { amount currencyCode } } " +
            "currentSubtotalPriceSet { shopMoney { amount currencyCode } } " +
            "currentTotalTaxSet { shopMoney { amount currencyCode } } " +
            "currentTotalDiscountsSet { shopMoney { amount currencyCode } } " +
            "customer { id email phone firstName lastName note verifiedEmail taxExempt state createdAt updatedAt " +
            "  defaultAddress { address1 address2 city province country zip phone firstName lastName company } } " +
            "shippingAddress { address1 address2 city province country zip phone firstName lastName company latitude longitude } " +
            "billingAddress { address1 address2 city province country zip phone firstName lastName company } " +
            "lineItems(first: 100) { edges { node { " +
            "  id title quantity currentQuantity sku variantTitle vendor " +
            "  requiresShipping taxable " +
            "  originalUnitPriceSet { shopMoney { amount currencyCode } presentmentMoney { amount currencyCode } } " +
            "  totalDiscountSet { shopMoney { amount currencyCode } } " +
            "  product { id } variant { id } " +
            "} } } }";

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtenerOrdenesEntreFechas(
            LocalDate inicio, LocalDate fin,
            ShopifyStoreConfig store, String token) {

        List<Map<String, Object>> todas = new ArrayList<>();

        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {

            log.info("[MULTI-SYNC] GET ordenes dia={} tienda={}", fecha, store.getDomain());

            String cursor = null;
            boolean hasNextPage = true;

            while (hasNextPage) {
                String after = cursor != null ? ", after: \"" + cursor + "\"" : "";
                String query = "{ orders(first: 250" + after +
                        ", query: \"created_at:>=" + fecha + "T00:00:00-06:00" +
                        " AND created_at:<=" + fecha + "T23:59:59-06:00\") " +
                        "{ edges { node " + ORDER_FIELDS + " } pageInfo { hasNextPage endCursor } } }";

                try {
                    Map<String, Object> gqlResponse = ejecutarGraphQL(query, store, token);
                    Map<String, Object> data = (Map<String, Object>) gqlResponse.get("data");
                    if (data == null) break;

                    Map<String, Object> ordersNode = (Map<String, Object>) data.get("orders");
                    if (ordersNode == null) break;

                    List<Map<String, Object>> edges = (List<Map<String, Object>>) ordersNode.get("edges");
                    if (edges != null) {
                        for (Map<String, Object> edge : edges) {
                            Map<String, Object> node = (Map<String, Object>) edge.get("node");
                            if (node != null) {
                                todas.add(convertirNodoGraphQLaRest(node));
                            }
                        }
                    }

                    Map<String, Object> pageInfo = (Map<String, Object>) ordersNode.get("pageInfo");
                    if (pageInfo != null && Boolean.TRUE.equals(pageInfo.get("hasNextPage"))) {
                        hasNextPage = true;
                        cursor = (String) pageInfo.get("endCursor");
                    } else {
                        hasNextPage = false;
                    }

                    log.info("[MULTI-SYNC] {} ordenes acumuladas dia={} tienda={}", todas.size(), fecha, store.getDomain());

                } catch (Exception e) {
                    log.error("[MULTI-SYNC] error al consultar dia={} tienda={} msg={}", fecha, store.getDomain(), e.getMessage());
                    break;
                }
            }
        }

        return todas;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obtenerOrdenPorId(String ordenId, ShopifyStoreConfig store, String token) {

        String gid = "gid://shopify/Order/" + ordenId;
        String query = "{ order(id: \"" + gid + "\") " + ORDER_FIELDS + " }";

        try {
            Map<String, Object> gqlResponse = ejecutarGraphQL(query, store, token);

            List<Object> errors = (List<Object>) gqlResponse.get("errors");
            if (errors != null && !errors.isEmpty()) {
                log.error("[MULTI-SYNC] GraphQL errors ordenId={} tienda={}: {}", ordenId, store.getDomain(), errors);
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) gqlResponse.get("data");
            if (data == null) {
                log.error("[MULTI-SYNC] GraphQL data=null ordenId={} tienda={} response={}", ordenId, store.getDomain(), gqlResponse);
                return null;
            }

            Map<String, Object> orderNode = (Map<String, Object>) data.get("order");
            if (orderNode == null) {
                log.warn("[MULTI-SYNC] orden no encontrada en GraphQL ordenId={} tienda={}", ordenId, store.getDomain());
                return null;
            }

            return convertirNodoGraphQLaRest(orderNode);

        } catch (Exception e) {
            log.error("[MULTI-SYNC] error al obtener ordenId={} tienda={} msg={}", ordenId, store.getDomain(), e.getMessage());
            return null;
        }
    }

    private Map<String, Object> ejecutarGraphQL(String query, ShopifyStoreConfig store, String token) {
        String url = "https://" + store.getDomain() + "/admin/api/" + store.getApiVersion() + "/graphql.json";

        RestTemplate localTemplate = new RestTemplate(restTemplate.getRequestFactory());
        localTemplate.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        localTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);

        ResponseEntity<Map<String, Object>> response = localTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        return response.getBody();
    }

    // ================= Conversion GraphQL → REST format =================

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertirNodoGraphQLaRest(Map<String, Object> node) {
        Map<String, Object> r = new LinkedHashMap<>();

        String gid = strVal(node, "id");
        r.put("admin_graphql_api_id", gid);
        r.put("id", extraerIdNumerico(gid));

        r.put("name", node.get("name"));
        r.put("order_number", node.get("number"));
        r.put("number", node.get("number"));
        r.put("email", node.get("email"));
        r.put("contact_email", node.get("email"));
        r.put("phone", node.get("phone"));
        r.put("created_at", node.get("createdAt"));
        r.put("processed_at", node.get("processedAt"));
        r.put("updated_at", node.get("updatedAt"));
        r.put("cancelled_at", node.get("cancelledAt"));
        r.put("cancel_reason", node.get("cancelReason"));
        r.put("closed_at", node.get("closedAt"));
        r.put("financial_status", toLower(node.get("displayFinancialStatus")));
        r.put("fulfillment_status", toLower(node.get("displayFulfillmentStatus")));
        r.put("tags", node.get("tags"));
        r.put("note", node.get("note"));
        r.put("test", node.get("test"));
        r.put("confirmed", node.get("confirmed"));
        r.put("taxes_included", node.get("taxesIncluded"));
        r.put("currency", node.get("currencyCode"));
        r.put("presentment_currency", node.get("presentmentCurrencyCode"));

        r.put("total_price_set", convertirPriceSet(node, "totalPriceSet"));
        r.put("total_price", extraerMonto(node, "totalPriceSet"));
        r.put("subtotal_price_set", convertirPriceSet(node, "subtotalPriceSet"));
        r.put("subtotal_price", extraerMonto(node, "subtotalPriceSet"));
        r.put("total_tax_set", convertirPriceSet(node, "totalTaxSet"));
        r.put("total_tax", extraerMonto(node, "totalTaxSet"));
        r.put("total_discounts_set", convertirPriceSet(node, "totalDiscountsSet"));
        r.put("total_discounts", extraerMonto(node, "totalDiscountsSet"));
        r.put("total_shipping_price_set", convertirPriceSet(node, "totalShippingPriceSet"));
        r.put("current_total_price_set", convertirPriceSet(node, "currentTotalPriceSet"));
        r.put("current_total_price", extraerMonto(node, "currentTotalPriceSet"));
        r.put("current_subtotal_price_set", convertirPriceSet(node, "currentSubtotalPriceSet"));
        r.put("current_subtotal_price", extraerMonto(node, "currentSubtotalPriceSet"));
        r.put("current_total_tax_set", convertirPriceSet(node, "currentTotalTaxSet"));
        r.put("current_total_tax", extraerMonto(node, "currentTotalTaxSet"));
        r.put("current_total_discounts_set", convertirPriceSet(node, "currentTotalDiscountsSet"));
        r.put("current_total_discounts", extraerMonto(node, "currentTotalDiscountsSet"));
        r.put("total_line_items_price_set", convertirPriceSet(node, "lineItemsSubtotalPrice"));
        r.put("total_line_items_price", extraerMonto(node, "lineItemsSubtotalPrice"));

        r.put("customer", convertirCustomer((Map<String, Object>) node.get("customer")));
        r.put("shipping_address", convertirAddress((Map<String, Object>) node.get("shippingAddress")));
        r.put("billing_address", convertirAddress((Map<String, Object>) node.get("billingAddress")));
        r.put("line_items", convertirLineItems((Map<String, Object>) node.get("lineItems")));

        r.put("shipping_lines", new ArrayList<>());
        r.put("discount_codes", new ArrayList<>());
        r.put("note_attributes", new ArrayList<>());
        r.put("fulfillments", new ArrayList<>());
        r.put("refunds", new ArrayList<>());
        r.put("returns", new ArrayList<>());
        r.put("tax_lines", new ArrayList<>());
        r.put("discount_applications", new ArrayList<>());
        r.put("line_item_groups", new ArrayList<>());

        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertirCustomer(Map<String, Object> c) {
        if (c == null) return null;
        Map<String, Object> r = new LinkedHashMap<>();
        String gid = strVal(c, "id");
        r.put("id", extraerIdNumerico(gid));
        r.put("admin_graphql_api_id", gid);
        r.put("email", c.get("email"));
        r.put("phone", c.get("phone"));
        r.put("first_name", c.get("firstName"));
        r.put("last_name", c.get("lastName"));
        r.put("note", c.get("note"));
        r.put("verified_email", c.get("verifiedEmail"));
        r.put("tax_exempt", c.get("taxExempt"));
        r.put("state", toLower(c.get("state")));
        r.put("created_at", c.get("createdAt"));
        r.put("updated_at", c.get("updatedAt"));
        r.put("default_address", convertirAddress((Map<String, Object>) c.get("defaultAddress")));
        return r;
    }

    private Map<String, Object> convertirAddress(Map<String, Object> a) {
        if (a == null) return null;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("address1", a.get("address1"));
        r.put("address2", a.get("address2"));
        r.put("city", a.get("city"));
        r.put("province", a.get("province"));
        r.put("country", a.get("country"));
        r.put("zip", a.get("zip"));
        r.put("phone", a.get("phone"));
        r.put("first_name", a.get("firstName"));
        r.put("last_name", a.get("lastName"));
        r.put("company", a.get("company"));
        if (a.containsKey("latitude")) r.put("latitude", a.get("latitude"));
        if (a.containsKey("longitude")) r.put("longitude", a.get("longitude"));
        return r;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertirLineItems(Map<String, Object> lineItemsNode) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (lineItemsNode == null) return result;

        List<Map<String, Object>> edges = (List<Map<String, Object>>) lineItemsNode.get("edges");
        if (edges == null) return result;

        for (Map<String, Object> edge : edges) {
            Map<String, Object> node = (Map<String, Object>) edge.get("node");
            if (node == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            String gid = strVal(node, "id");
            item.put("id", extraerIdNumerico(gid));
            item.put("admin_graphql_api_id", gid);
            item.put("title", node.get("title"));
            item.put("quantity", node.get("quantity"));
            item.put("current_quantity", node.get("currentQuantity"));
            item.put("sku", node.get("sku"));
            item.put("variant_title", node.get("variantTitle"));
            item.put("vendor", node.get("vendor"));
            item.put("requires_shipping", node.get("requiresShipping"));
            item.put("taxable", node.get("taxable"));

            item.put("price_set", convertirPriceSet(node, "originalUnitPriceSet"));
            item.put("price", extraerMonto(node, "originalUnitPriceSet"));
            item.put("total_discount_set", convertirPriceSet(node, "totalDiscountSet"));
            item.put("total_discount", extraerMonto(node, "totalDiscountSet"));

            Map<String, Object> product = (Map<String, Object>) node.get("product");
            if (product != null) item.put("product_id", extraerIdNumerico(strVal(product, "id")));

            Map<String, Object> variant = (Map<String, Object>) node.get("variant");
            if (variant != null) item.put("variant_id", extraerIdNumerico(strVal(variant, "id")));

            result.add(item);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertirPriceSet(Map<String, Object> node, String field) {
        Map<String, Object> priceSet = (Map<String, Object>) node.get(field);
        if (priceSet == null) return null;

        Map<String, Object> r = new LinkedHashMap<>();

        Map<String, Object> shopMoney = (Map<String, Object>) priceSet.get("shopMoney");
        if (shopMoney != null) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("amount", shopMoney.get("amount"));
            sm.put("currency_code", shopMoney.get("currencyCode"));
            r.put("shop_money", sm);
        }

        Map<String, Object> presentmentMoney = (Map<String, Object>) priceSet.get("presentmentMoney");
        if (presentmentMoney != null) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("amount", presentmentMoney.get("amount"));
            pm.put("currency_code", presentmentMoney.get("currencyCode"));
            r.put("presentment_money", pm);
        }

        return r;
    }

    @SuppressWarnings("unchecked")
    private String extraerMonto(Map<String, Object> node, String field) {
        Map<String, Object> priceSet = (Map<String, Object>) node.get(field);
        if (priceSet == null) return null;
        Map<String, Object> shopMoney = (Map<String, Object>) priceSet.get("shopMoney");
        if (shopMoney == null) return null;
        Object amount = shopMoney.get("amount");
        return amount != null ? amount.toString() : null;
    }

    private Long extraerIdNumerico(String gid) {
        if (gid == null || gid.isBlank()) return null;
        try {
            int lastSlash = gid.lastIndexOf('/');
            String raw = lastSlash >= 0 ? gid.substring(lastSlash + 1) : gid;
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String strVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private String toLower(Object value) {
        return value != null ? value.toString().toLowerCase() : null;
    }

    // ================= HELPERS =================

    private String resolverTokenOrdenes(ShopifyStoreConfig store) {
        String ordersToken = store.getOrdersAccessToken();
        if (ordersToken != null && !ordersToken.isBlank()) {
            return ordersToken;
        }
        return tokenResolver.resolverToken(store);
    }

    private ShopifyStoreConfig resolverTienda(String dominio) {
        return multiStoreProperties.getStores().stream()
                .filter(s -> s.getDomain().equalsIgnoreCase(dominio))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tienda no encontrada en configuracion: " + dominio));
    }

    private Map<String, Object> resultado(
            String dominio,
            List<String> exitosas,
            Map<String, String> fallidas,
            String mensaje) {

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tienda", dominio);
        r.put("exitosas", exitosas);
        r.put("fallidas", fallidas);
        r.put("totalExitosas", exitosas.size());
        r.put("totalFallidas", fallidas.size());
        if (mensaje != null) {
            r.put("mensaje", mensaje);
        }
        return r;
    }
}
