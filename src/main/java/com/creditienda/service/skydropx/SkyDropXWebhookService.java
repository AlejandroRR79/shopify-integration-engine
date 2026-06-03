package com.creditienda.service.skydropx;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class SkyDropXWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SkyDropXWebhookService.class);

    @Value("${skydropx.webhook.hmac-secret}")
    private String webhookSecret;

    public void processWebhook(
            String authorization,
            String rawBody,
            JsonNode payload) throws Exception {

        validateHmac(authorization, rawBody);

        log.info("✅ HMAC válido");

        String event = payload.path("event").asText();

        log.info("📦 Event={}", event);

        JsonNode data = payload.path("data");

        String shipmentId = data.path("id").asText();
        String trackingNumber = data.path("tracking_number").asText();
        String status = data.path("status").asText();

        log.info(
                "📦 shipmentId={} tracking={} status={}",
                shipmentId,
                trackingNumber,
                status);

        // TODO:
        // persistencia
        // update DB
        // auditoría
    }

    private void validateHmac(
            String authorization,
            String rawBody) throws Exception {

        if (authorization == null || authorization.isBlank()) {
            throw new RuntimeException("Authorization HMAC vacío");
        }

        Mac sha512Hmac = Mac.getInstance("HmacSHA512");

        SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512");

        sha512Hmac.init(secretKey);

        byte[] signedBytes = sha512Hmac.doFinal(
                rawBody.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();

        for (byte b : signedBytes) {
            sb.append(String.format("%02x", b));
        }

        String calculatedHmac = sb.toString();

        String receivedHmac = authorization
                .replace("HMAC ", "")
                .trim();

        if (!MessageDigest.isEqual(
                calculatedHmac.getBytes(StandardCharsets.UTF_8),
                receivedHmac.getBytes(StandardCharsets.UTF_8))) {

            log.error("❌ HMAC inválido");

            throw new RuntimeException("HMAC inválido");
        }
    }
}