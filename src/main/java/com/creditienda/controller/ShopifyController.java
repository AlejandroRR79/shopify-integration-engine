package com.creditienda.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.ShopifyService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ShopifyController {

    private static final Logger logger = LogManager.getLogger(ShopifyController.class);

    @Autowired
    private ShopifyService shopifyService;

    @Value("${shopify.app.secret}")
    private String shopifyAppSecret;

    @Value("${shopify.webhook.secret}")
    private String shopifySecret;

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
        List<Map<String, Object>> fulfillments = (List<Map<String, Object>>) respuestaFulfillments
                .get("fulfillments");
        if (fulfillments != null && !fulfillments.isEmpty()) {
            logger.warn("Ya existe un fulfillment para la orden {}", orderId);
            return Map.of(
                    "orden_actualizada", respuestaActualizada,
                    "fulfillment_error", "Ya existe un fulfillment para esta orden.");
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
        List<Map<String, Object>> fulfillmentOrders = (List<Map<String, Object>>) respuestaFulfillmentOrders
                .get("fulfillment_orders");
        if (fulfillmentOrders == null || fulfillmentOrders.isEmpty()) {
            logger.warn("No se encontró fulfillment_order para la orden {}", orderId);
            return Map.of(
                    "orden_actualizada", respuestaActualizada,
                    "fulfillment_error", "No se encontró fulfillment_order para esta orden.");
        }

        Map<String, Object> fulfillmentOrder = fulfillmentOrders.get(0);
        String status = (String) fulfillmentOrder.get("status");
        if (!"open".equalsIgnoreCase(status)) {
            logger.warn("Fulfillment order no está en estado 'open': {}", status);
            return Map.of(
                    "orden_actualizada", respuestaActualizada,
                    "fulfillment_error",
                    "El fulfillment_order no está en estado válido para ser cumplido.");
        }

        // 6. Validar si ya tiene tracking asignado
        if (fulfillmentOrder.containsKey("tracking_info")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> trackingInfo = (Map<String, Object>) fulfillmentOrder.get("tracking_info");
            if (trackingInfo != null && trackingInfo.get("number") != null) {
                logger.warn("La orden ya tiene número de guía asignado: {}",
                        trackingInfo.get("number"));
                return Map.of(
                        "orden_actualizada", respuestaActualizada,
                        "fulfillment_error", "La orden ya tiene guía de embarque asignada.");
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
                                "company", trackingCompany),
                        "line_items_by_fulfillment_order", List.of(
                                Map.of("fulfillment_order_id", fulfillmentOrderId))));

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
                "fulfillment_creado", respuestaFulfillment);
    }

    @PostMapping("/shopify/webhook")
    public ResponseEntity<String> recibirWebhookSinFiltro(HttpServletRequest request) {
        try {
            // 🔍 Imprimir todos los headers para validar que llega la firma HMAC
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.info("Header recibido: {} = {}", headerName, headerValue);
            }

            // Leer el cuerpo crudo como bytes (sin convertir a String aún)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream inputStream = request.getInputStream();
            byte[] temp = new byte[1024];
            int read;
            while ((read = inputStream.read(temp)) != -1) {
                buffer.write(temp, 0, read);
            }
            byte[] rawBodyBytes = buffer.toByteArray();

            logger.info("📦 Cuerpo crudo en bytes:\n{}", new String(rawBodyBytes, StandardCharsets.UTF_8));
            // Recuperar la firma HMAC enviada por Shopify
            String hmacHeader = request.getHeader("X-Shopify-Hmac-Sha256");
        
            // Calcular HMAC localmente
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(shopifySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(keySpec);
            byte[] hmacCalculado = mac.doFinal(rawBodyBytes);
            String hmacCalculadoBase64 = Base64.getEncoder().encodeToString(hmacCalculado);

            // Comparar firmas
            boolean valido = MessageDigest.isEqual(Base64.getDecoder().decode(hmacHeader), hmacCalculado);

            // Logs de trazabilidad
            logger.info("🔐 HMAC recibido: {}", hmacHeader);
            logger.info("🔐 HMAC calculado localmente: {}", hmacCalculadoBase64);
            logger.info("🔍 ¿Firma válida?: {}", valido);

            // Convertir cuerpo a String y formatear como JSON solo si la firma es válida
            String rawBody = new String(rawBodyBytes, StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(rawBody, Object.class);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

            // logger.info("📦 Webhook recibido:\n{}", prettyJson);

            return ResponseEntity.ok("Webhook recibido correctamente");

        } catch (Exception e) {
            logger.error("Error procesando webhook sin filtro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno");
        }
    }

    private byte[] leerCuerpoCrudo(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException e) {
            logger.error("Error leyendo cuerpo crudo del webhook", e);
            return new byte[0];
        }
    }

    private boolean verificarHmac(String payload, String hmacHeader, String sharedSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] hmacCalculado = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] hmacRecibido = Base64.getDecoder().decode(hmacHeader);

            return MessageDigest.isEqual(hmacCalculado, hmacRecibido);
        } catch (Exception e) {
            logger.error("Error al validar HMAC", e);
            return false;
        }
    }


}