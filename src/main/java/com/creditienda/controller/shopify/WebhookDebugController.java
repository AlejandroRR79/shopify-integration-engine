package com.creditienda.controller.shopify;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
public class WebhookDebugController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDebugController.class);

    @PostMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String rawBody) {

        int bodyLength = rawBody == null ? 0 : rawBody.getBytes(StandardCharsets.UTF_8).length;

        logger.info("[WEBHOOK-DEBUG] headers={}", headers);

        if (rawBody == null || rawBody.isEmpty()) {
            logger.info("[WEBHOOK-DEBUG] body vacio");
        } else {
            logger.info("[WEBHOOK-DEBUG] body={}", rawBody);
        }

        logger.info("[WEBHOOK-DEBUG] bodyLengthBytes={}", bodyLength);

        return ResponseEntity.ok(Map.of(
                "received", true,
                "bodyLength", bodyLength));
    }
}
