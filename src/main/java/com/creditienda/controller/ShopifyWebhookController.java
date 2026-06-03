package com.creditienda.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RestController
@RequestMapping("/webhook")
public class ShopifyWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyWebhookController.class);

    @Value("${shopify.webhook.secret}")
    private String shopifyWebhookSecret;

    private final ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @PostMapping("/registrarOrden")
    public ResponseEntity<String> registrarOrdenWebhook(
            @RequestHeader("X-Shopify-Hmac-Sha256") String shopifyHmac,
            @RequestBody String requestBody) {

        logger.info("📩 Webhook recibido en /webhook/registrarOrden");
        logger.debug("🔐 HMAC recibido: {}", shopifyHmac);

        try {
            // 🧮 Validar HMAC
            String calculatedHmac = calculateHmac(shopifyWebhookSecret, requestBody);
            logger.debug("🧮 HMAC calculado: {}", calculatedHmac);

            boolean isValid = MessageDigest.isEqual(
                    Base64.getDecoder().decode(shopifyHmac),
                    Base64.getDecoder().decode(calculatedHmac));

            if (!isValid) {
                logger.warn("❌ HMAC inválido. Acceso denegado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("HMAC inválido");
            }

            // 🖨️ Imprimir JSON recibido de forma legible
            String prettyJson = prettyPrinter.writeValueAsString(new ObjectMapper().readTree(requestBody));
            logger.info("📦 Payload recibido:\n{}", prettyJson);

            // Aquí puedes procesar la orden como necesites
            logger.info("✅ HMAC válido. Orden procesada correctamente.");
            return ResponseEntity.ok("Orden recibida correctamente");

        } catch (IllegalArgumentException e) {
            logger.error("⚠️ Error al decodificar HMAC. Formato inválido", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("HMAC mal formado");

        } catch (Exception e) {
            logger.error("💥 Error inesperado al procesar el webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno");
        }
    }

    private String calculateHmac(String secret, String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            logger.error("💥 Error al calcular HMAC", e);
            throw new RuntimeException("Error al calcular HMAC", e);
        }
    }
}