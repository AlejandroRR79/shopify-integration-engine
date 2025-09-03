package com.creditienda.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono ;

@Service
public class ShopifyService {
    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager
            .getLogger(ShopifyService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    private final WebClient webClient = WebClient.builder().build();

    public String getShopDomain() {
        return this.shopDomain;
    }

    public String getApiVersion() {
        return this.apiVersion;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public WebClient getWebClient() {
        return this.webClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerProductos() {
        String url = String.format("https://%s/admin/api/%s/products.json", shopDomain, apiVersion);

        Mono<Map> response = webClient.get()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(Map.class);

        return response.block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerProductoPorId(Long id) {
        String url = String.format("https://%s/admin/api/%s/products/%d.json", shopDomain, apiVersion, id);

        return webClient.get()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> crearProductoDesdeExistente(Long idProductoOriginal) {
        Map<String, Object> original = obtenerProductoPorId(idProductoOriginal);
        Map<String, Object> producto = (Map<String, Object>) original.get("product");

        // Construimos el nuevo producto
        Map<String, Object> nuevoProducto = new HashMap<>();
        nuevoProducto.put("title", producto.get("title") + " (copia)");
        nuevoProducto.put("body_html", "Creado desde un REST");
        nuevoProducto.put("vendor", producto.get("vendor"));
        nuevoProducto.put("product_type", producto.get("product_type"));
        nuevoProducto.put("tags", producto.get("tags"));

        // Copiamos variantes si existen
        if (producto.containsKey("variants")) {
            nuevoProducto.put("variants", producto.get("variants"));
        }

        // Empaquetamos en el formato que Shopify espera
        Map<String, Object> payload = Map.of("product", nuevoProducto);

        String url = String.format("https://%s/admin/api/%s/products.json", shopDomain, apiVersion);

        return webClient.post()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarOrdenPorId(String orderId) {
        String url = String.format("https://%s/admin/api/%s/orders/%s.json", shopDomain, apiVersion, orderId);

        Mono<Map> response = webClient.get()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(Map.class);

        return response.block();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarOrdenPorIdYPreparar(String orderId) {
        String url = String.format("https://%s/admin/api/%s/orders/%s.json", shopDomain, apiVersion, orderId);

        Map<String, Object> respuesta = webClient.get()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> orden = (Map<String, Object>) respuesta.get("order");
        if (orden == null) {
            logger.warn("No se encontró la orden con ID: {}", orderId);
            return Map.of("error", "Orden no encontrada.");
        }

        // 🔧 Modificación local (sin enviar a Shopify)
        orden.put("note", "Preparada para integración externa");
        orden.put("tags", "procesada, validada");

        // Puedes agregar más lógica aquí si necesitas preparar campos específicos

        return Map.of("orden", orden);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> actualizarOrdenConNotaYTags(String orderId, Map<String, Object> respuestaOriginal) {
        Map<String, Object> orden = (Map<String, Object>) respuestaOriginal.get("order");
        if (orden == null) {
            logger.warn("No se encontró la orden con ID: {}", orderId);
            return Map.of("error", "Orden no encontrada.");
        }

        // 🔧 Modificar atributos
        orden.put("note", "Actualizada automáticamente desde sistema");
        orden.put("tags", "procesada, validada");

        // 📤 Enviar actualización a Shopify
        String url = String.format("https://%s/admin/api/%s/orders/%s.json", shopDomain, apiVersion, orderId);
        Map<String, Object> payload = Map.of("order", orden);

        Map<String, Object> respuestaActualizada = webClient.put()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        logger.info("Respuesta de actualización: {}", respuestaActualizada);
        return respuestaActualizada;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> crearFulfillmentConValidacion(String orderId, String trackingNumber,
            String trackingCompany, boolean notifyCustomer) {
        // 1. Verificar si ya existe un fulfillment
        String urlFulfillments = String.format("https://%s/admin/api/%s/orders/%s/fulfillments.json", shopDomain,
                apiVersion, orderId);
        Map<String, Object> respuestaFulfillments = webClient.get()
                .uri(urlFulfillments)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> fulfillments = (List<Map<String, Object>>) respuestaFulfillments.get("fulfillments");
        if (fulfillments != null && !fulfillments.isEmpty()) {
            logger.warn("Ya existe un fulfillment para la orden {}", orderId);
            return Map.of("error", "Ya existe un fulfillment para esta orden.");
        }

        // 2. Obtener fulfillment_order_id y validar estado
        String urlFulfillmentOrders = String.format("https://%s/admin/api/%s/orders/%s/fulfillment_orders.json",
                shopDomain, apiVersion, orderId);
        Map<String, Object> respuestaFulfillmentOrders = webClient.get()
                .uri(urlFulfillmentOrders)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> fulfillmentOrders = (List<Map<String, Object>>) respuestaFulfillmentOrders
                .get("fulfillment_orders");
        if (fulfillmentOrders == null || fulfillmentOrders.isEmpty()) {
            logger.warn("No se encontró fulfillment_order para la orden {}", orderId);
            return Map.of("error", "No se encontró fulfillment_order para esta orden.");
        }

        Map<String, Object> fulfillmentOrder = fulfillmentOrders.get(0);
        String status = (String) fulfillmentOrder.get("status");
        if (!"open".equalsIgnoreCase(status)) {
            logger.warn("Fulfillment order no está en estado 'open': {}", status);
            return Map.of("error", "El fulfillment_order no está en estado válido para ser cumplido.");
        }

        // 3. Validar si ya tiene tracking asignado
        if (fulfillmentOrder.containsKey("tracking_info")) {
            Map<String, Object> trackingInfo = (Map<String, Object>) fulfillmentOrder.get("tracking_info");
            if (trackingInfo != null && trackingInfo.get("number") != null) {
                logger.warn("La orden ya tiene número de guía asignado: {}", trackingInfo.get("number"));
                return Map.of("error", "La orden ya tiene guía de embarque asignada.");
            }
        }

        // 4. Crear fulfillment
        String fulfillmentOrderId = fulfillmentOrder.get("id").toString();
        Map<String, Object> payload = Map.of(
                "fulfillment", Map.of(
                        "message", "Logística actualizada desde sistema externo",
                        "notify_customer", notifyCustomer,
                        "tracking_info", Map.of(
                                "number", trackingNumber,
                                "company", trackingCompany),
                        "line_items_by_fulfillment_order", List.of(
                                Map.of("fulfillment_order_id", fulfillmentOrderId))));

        String url = String.format("https://%s/admin/api/%s/fulfillments.json", shopDomain, apiVersion);
        Map<String, Object> respuesta = webClient.post()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        logger.info("Fulfillment creado con logística: {}", respuesta);
        return respuesta;
    }

}