package com.creditienda.controller;

import com.creditienda.service.ShopifyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ShopifyController {

    private static final Logger logger = LogManager.getLogger(ShopifyController.class);

    @Autowired
    private ShopifyService shopifyService;

    @GetMapping("/shopify/productos")
    public Map<String, Object> listarProductos() {
        logger.info("Consultando productos en Shopify...");
        Map<String, Object> productos = shopifyService.obtenerProductos();
        logger.info("Productos recibidos: {}", productos);
        return productos;
    }
@GetMapping("/shopify/orden/{orderId}")
public Map<String, Object> consultarYActualizarOrden(@PathVariable String orderId) {
    logger.info("Consultando orden en Shopify con ID: {}", orderId);

    // 1. Recuperar la orden
    Map<String, Object> respuestaOriginal = shopifyService.consultarOrdenPorId(orderId);
    Object rawOrder = respuestaOriginal.get("order");
    if (!(rawOrder instanceof Map)) {
        logger.warn("La respuesta no contiene una orden válida");
        return Map.of("error", "Orden no encontrada o formato inválido");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> orden = (Map<String, Object>) rawOrder;
    logger.info("Orden recibida: {}", orden);

    // 2. Modificar localmente los campos
    orden.put("note", "Actualizada automáticamente desde sistema");
    orden.put("tags", "procesada, validada");

    // 3. Enviar actualización a Shopify
    Map<String, Object> payloadActualizacion = Map.of("order", orden);
    String urlActualizacion = String.format("https://%s/admin/api/%s/orders/%s.json",
            shopifyService.getShopDomain(), shopifyService.getApiVersion(), orderId);

    Map<String, Object> respuestaActualizada = shopifyService.getWebClient().put()
            .uri(urlActualizacion)
            .header("X-Shopify-Access-Token", shopifyService.getAccessToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .bodyValue(payloadActualizacion)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    logger.info("Orden actualizada en Shopify: {}", respuestaActualizada);

    // 4. Verificar si ya existe un fulfillment
    String urlFulfillments = String.format("https://%s/admin/api/%s/orders/%s/fulfillments.json",
            shopifyService.getShopDomain(), shopifyService.getApiVersion(), orderId);

    Map<String, Object> respuestaFulfillments = shopifyService.getWebClient().get()
            .uri(urlFulfillments)
            .header("X-Shopify-Access-Token", shopifyService.getAccessToken())
            .header("Accept", "application/json")
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fulfillments = (List<Map<String, Object>>) respuestaFulfillments.get("fulfillments");
    if (fulfillments != null && !fulfillments.isEmpty()) {
        logger.warn("Ya existe un fulfillment para la orden {}", orderId);
        return Map.of(
                "orden_actualizada", respuestaActualizada,
                "fulfillment_error", "Ya existe un fulfillment para esta orden."
        );
    }

    // 5. Obtener fulfillment_order_id y validar estado
    String urlFulfillmentOrders = String.format("https://%s/admin/api/%s/orders/%s/fulfillment_orders.json",
            shopifyService.getShopDomain(), shopifyService.getApiVersion(), orderId);

    Map<String, Object> respuestaFulfillmentOrders = shopifyService.getWebClient().get()
            .uri(urlFulfillmentOrders)
            .header("X-Shopify-Access-Token", shopifyService.getAccessToken())
            .header("Accept", "application/json")
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fulfillmentOrders = (List<Map<String, Object>>) respuestaFulfillmentOrders.get("fulfillment_orders");
    if (fulfillmentOrders == null || fulfillmentOrders.isEmpty()) {
        logger.warn("No se encontró fulfillment_order para la orden {}", orderId);
        return Map.of(
                "orden_actualizada", respuestaActualizada,
                "fulfillment_error", "No se encontró fulfillment_order para esta orden."
        );
    }

    Map<String, Object> fulfillmentOrder = fulfillmentOrders.get(0);
    String status = (String) fulfillmentOrder.get("status");
    if (!"open".equalsIgnoreCase(status)) {
        logger.warn("Fulfillment order no está en estado 'open': {}", status);
        return Map.of(
                "orden_actualizada", respuestaActualizada,
                "fulfillment_error", "El fulfillment_order no está en estado válido para ser cumplido."
        );
    }

    // 6. Validar si ya tiene tracking asignado
    if (fulfillmentOrder.containsKey("tracking_info")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> trackingInfo = (Map<String, Object>) fulfillmentOrder.get("tracking_info");
        if (trackingInfo != null && trackingInfo.get("number") != null) {
            logger.warn("La orden ya tiene número de guía asignado: {}", trackingInfo.get("number"));
            return Map.of(
                    "orden_actualizada", respuestaActualizada,
                    "fulfillment_error", "La orden ya tiene guía de embarque asignada."
            );
        }
    }

    // 7. Crear fulfillment con guía de embarque
    String fulfillmentOrderId = fulfillmentOrder.get("id").toString();
    String trackingNumber = "NPX5T2YPLN";
    String trackingCompany = "FedEx";
    boolean notifyCustomer = true;

    Map<String, Object> payloadFulfillment = Map.of(
            "fulfillment", Map.of(
                    "message", "Logística actualizada desde sistema externo",
                    "notify_customer", notifyCustomer,
                    "tracking_info", Map.of(
                            "number", trackingNumber,
                            "company", trackingCompany
                    ),
                    "line_items_by_fulfillment_order", List.of(
                            Map.of("fulfillment_order_id", fulfillmentOrderId)
                    )
            )
    );

    String urlFulfillment = String.format("https://%s/admin/api/%s/fulfillments.json",
            shopifyService.getShopDomain(), shopifyService.getApiVersion());

    Map<String, Object> respuestaFulfillment = shopifyService.getWebClient().post()
            .uri(urlFulfillment)
            .header("X-Shopify-Access-Token", shopifyService.getAccessToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .bodyValue(payloadFulfillment)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    logger.info("Fulfillment creado con logística: {}", respuestaFulfillment);

    // 8. Retornar todo junto
    return Map.of(
            "orden_actualizada", respuestaActualizada,
            "fulfillment_creado", respuestaFulfillment
    );
}
    
}