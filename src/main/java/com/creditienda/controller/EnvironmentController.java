package com.creditienda.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopify/secure")
public class EnvironmentController {

    private final Environment environment;

    public EnvironmentController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/env")
    public ResponseEntity<Map<String, String>> getProperties() {
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
                "estafeta.cobertura.make.apikey",
                "estafeta.guia.token.url",
                "estafeta.guia.client.id",
                "estafeta.guia.client.secret",
                "estafeta.guia.scope",
                "estafeta.guia.grant.type",
                "estafeta.guia.api.url",
                "estafeta.guia.api.query",
                "estafeta.guia.apikey",
                "estafeta.guia.make.apikey",
                "estafeta.make.apikey",
                "estafeta.guia.effective-date.offset-days",
                "b2b.auth.url",
                "b2b.auth.idEmpresa",
                "b2b.auth.clave",
                "b2b.auth.password",
                "b2b.endpoint.timbre",
                "b2b.endpoint.timbreIntegrador",
                "b2b.endpoint.descargaPdf",
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
                "b2b.oc.auth.url",
                "b2b.oc.auth.usuario",
                "b2b.oc.auth.empresa",
                "b2b.oc.auth.password",
                "b2b.order.url"
        };

        for (String key : keys) {
            String value = environment.getProperty(key);
            masked.put(key, maskValue(key, value));
        }

        return ResponseEntity.ok(masked);
    }

    private String maskValue(String key, String value) {
        if (value == null)
            return null;

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