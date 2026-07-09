package com.creditienda.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopify/secure")
public class EnvironmentController {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentController.class);
    private final Environment environment;

    public EnvironmentController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/env")
    public ResponseEntity<Map<String, String>> getProperties(
            @RequestParam(required = false, defaultValue = "false") boolean reveal) {
        Map<String, String> masked = new LinkedHashMap<>();

        // Lista de claves que quieres exponer
        String[] keys = {
                "shopify.api.version",
                "shopify.shop.domain",
                "shopify.access.token",
                "shopify.app.id",
                "shopify.app.secret",
                "shopify.webhook.secret",
                "shopify.webhook.enabled",
                "api.access.token",
                "estafeta.token.url",
                "estafeta.client.id",
                "estafeta.client.secret",
                "estafeta.scope",
                "estafeta.grant.type",
                "estafeta.api.url",
                "estafeta.apikey",
                "estafeta.input.type",
                "estafeta.cobertura.token.url",
                "estafeta.cobertura.client.id",
                "estafeta.cobertura.client.secret",
                "estafeta.cobertura.scope",
                "estafeta.cobertura.grant.type",
                "estafeta.cobertura.api.url",
                "estafeta.cobertura.apikey",
                "estafeta.guia.token.url",
                "estafeta.guia.client.id",
                "estafeta.guia.client.secret",
                "estafeta.guia.scope",
                "estafeta.guia.grant.type",
                "estafeta.guia.api.url",
                "estafeta.guia.api.query",
                "estafeta.guia.apikey",
                "estafeta.guia.effective-date.offset-days",
                "spring.mail.host",
                "spring.mail.port",
                "spring.mail.username",
                "spring.mail.password",
                "spring.mail.protocol",
                "spring.mail.properties.mail.smtp.auth",
                "spring.mail.properties.mail.smtp.starttls.enable",
                "spring.mail.properties.mail.smtp.starttls.required",
                "spring.mail.properties.mail.smtp.connectiontimeout",
                "spring.mail.properties.mail.smtp.timeout",
                "spring.mail.properties.mail.smtp.writetimeout",
                "app.mail.notificacion.operacion",
                "app.mail.error.operacion",
                "jwt.secret",
                "auth.users.admin.username",
                "auth.users.admin.password",
                "auth.rate-limit.max-attempts",
                "auth.rate-limit.window-minutes",
                "auth.rate-limit.block-minutes",
                "b2b.oc.auth.url",
                "b2b.oc.auth.usuario",
                "b2b.oc.auth.empresa",
                "b2b.oc.auth.password",
                "b2b.order.url",
                "spring.datasource.url",
                "spring.datasource.username",
                "spring.datasource.password",
                "spring.datasource.driver-class-name",

                "spring.jpa.hibernate.ddl-auto",
                "spring.jpa.show-sql",
                "spring.jpa.database-platform",

                "b2b.delivery.base.url",
                "b2b.delivery.usuario",
                "b2b.delivery.cve-estatus-odc",
                "b2b.delivery.id-sucursal-cliente",
                "b2b.delivery.endpoint.seguimiento",
                "b2b.delivery.endpoint.actualizar",
                "b2b.seguimiento.estatus", "estafeta.cron.exp",

                "estafeta.seguro.token.url",
                "estafeta.seguro.client.id",
                "estafeta.seguro.client.secret",
                "estafeta.seguro.scope",
                "estafeta.seguro.grant.type",
                "estafeta.seguro.api.url",
                "estafeta.seguro.apikey",
                "shopify.bulk.chunk-size",

                // ───────── Build info ─────────
                "app.build.name",
                "app.build.version",
                "app.build.date",
                "tracking.mode",

                // ───────── SkyDropX ─────────
                "skydropx.base-url",
                "skydropx.auth-url",
                "skydropx.client-id",
                "skydropx.client-secret",
                "skydropx.grant-type",
                "skydropx.requested-carriers",
                "skydropx.timeout-ms",
                "skydropx.token-refresh-buffer",
                "skydropx.auth.token-expiration-seconds",
                "skydropx.shipment.polling.max-attempts",
                "skydropx.shipment.polling.delay-seconds",
                "skydropx.quotation.polling.max-attempts",
                "skydropx.quotation.polling.delay-seconds",
                "skydropx.recovery.cron.exp",
                "skydropx.recovery.stuck-minutes",
                "skydropx.recovery.max-retries",
                "skydropx.recovery.batch-size",
                "skydropx.selection.priority",
                "skydropx.selection.allowed-service-types",
                "skydropx.shipment.printing-format",
                "skydropx.shipment.package-type",
                "skydropx.shipment.country-code",
                "guia.prelacion",

                // ───────── Multi-tienda Shopify ─────────
                "shopify.stores[0].alias",
                "shopify.stores[0].domain",
                "shopify.stores[0].api-version",
                "shopify.stores[0].auth-type",
                "shopify.stores[0].access-token",
                "shopify.stores[0].bulk-chunk-size",
                "shopify.stores[1].alias",
                "shopify.stores[1].domain",
                "shopify.stores[1].api-version",
                "shopify.stores[1].auth-type",
                "shopify.stores[1].client-id",
                "shopify.stores[1].client-secret",
                "shopify.stores[1].token-url",
                "shopify.stores[1].bulk-chunk-size",
                "shopify.stores[1].update-price"

        };

        try {
            for (String key : keys) {
                String value = environment.getProperty(key);

                if ("app.build.date".equals(key) && value != null) {
                    LocalDateTime utc = LocalDateTime.parse(value,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    ZonedDateTime cdmx = utc.atZone(ZoneId.of("UTC"))
                            .withZoneSameInstant(ZoneId.of("America/Mexico_City"));

                    value = cdmx.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }

                masked.put(key, maskValue(key, value != null ? value : "NO_EXISTE", reveal));
            }
        } catch (Exception e) {
            log.error("Error al obtener la configuracion", e);

            // ❌ Error explícito
            masked.clear();
            masked.put("status", "ERROR");
            masked.put("message", "Error al obtener la configuración: " + (e != null ? e.getMessage() : ""));
            masked.put("timestamp", LocalDateTime.now().toString());
        }

        return ResponseEntity.ok(masked);
    }

    private String maskValue(String key, String value, boolean reveal) {

        if (value == null)
            return null;

        // 🔓 si reveal=true mostrar todo
        if (reveal) {
            return value;
        }

        boolean sensitive = isSensitive(key);

        if (!sensitive) {
            return value;
        }

        int length = value.length();
        if (length <= 8) {
            return "****";
        }

        String firstPart = value.substring(0, 4);
        String lastPart = value.substring(length - 4);
        return firstPart + "****" + lastPart;
    }

    private boolean isSensitive(String key) {

        String upperKey = key.toUpperCase();

        return upperKey.contains("SECRET") ||
                upperKey.contains("TOKEN") ||
                upperKey.contains("PASSWORD") ||
                upperKey.contains("KEY") ||
                upperKey.contains("USERNAME") ||
                upperKey.contains("USER") ||
                upperKey.contains("MAIL");
    }
}