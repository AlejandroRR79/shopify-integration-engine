package com.creditienda.controller.skydropx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.skydropx.SkyDropXWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhook/skydropx")
public class SkyDropXWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SkyDropXWebhookController.class);

    private final SkyDropXWebhookService webhookService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SkyDropXWebhookController(
            SkyDropXWebhookService webhookService) {

        this.webhookService = webhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,

            @RequestBody String rawBody) {

        try {

            log.info("📥 Webhook SkyDropX recibido");

            String maskedAuth = (authorization != null && authorization.length() > 12)
                    ? authorization.substring(0, 6) + "****" + authorization.substring(authorization.length() - 4)
                    : "****";
            log.info("Authorization={}", maskedAuth);

            JsonNode payload = objectMapper.readTree(rawBody);

            webhookService.processWebhook(
                    authorization,
                    rawBody,
                    payload);

            log.info("Payload={}", payload.toPrettyString());

            return ResponseEntity.ok(
                    "Webhook procesado correctamente");

        } catch (Exception e) {

            log.error(
                    "❌ Error procesando webhook SkyDropX",
                    e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando webhook");
        }
    }
}