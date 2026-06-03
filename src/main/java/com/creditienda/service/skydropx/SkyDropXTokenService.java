package com.creditienda.service.skydropx;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class SkyDropXTokenService {

        private static final Logger log = LogManager.getLogger(
                        SkyDropXTokenService.class);

        @Value("${skydropx.auth-url}")
        private String authUrl;

        @Value("${skydropx.client-id}")
        private String clientId;

        @Value("${skydropx.client-secret}")
        private String clientSecret;

        /**
         * Tiempo cache token.
         *
         * Recomendado:
         * 3300 segundos (55 min)
         */
        @Value("${skydropx.auth.token-expiration-seconds}")
        private long tokenExpirationSeconds;

        private final RestTemplate restTemplate;

        /**
         * Token cache memoria.
         */
        private volatile String cachedAccessToken;

        /**
         * Fecha expiración token.
         */
        private volatile LocalDateTime tokenExpirationTime;

        public SkyDropXTokenService(
                        RestTemplate restTemplate) {

                this.restTemplate = restTemplate;
        }

        /**
         * Obtener token reutilizable.
         *
         * Si token sigue vigente:
         * reutiliza cache.
         *
         * Si expiró:
         * solicita nuevo token OAuth.
         */
        public synchronized String getAccessToken() {

                /**
                 * Reutilizar token vigente.
                 */
                if (cachedAccessToken != null
                                && tokenExpirationTime != null
                                && LocalDateTime.now().isBefore(
                                                tokenExpirationTime)) {

                        log.debug(
                                        "[SKYDROPX-AUTH] reutilizando token cache");

                        return cachedAccessToken;
                }

                try {

                        log.info(
                                        "[SKYDROPX-AUTH] solicitando nuevo token OAuth");

                        HttpHeaders headers = new HttpHeaders();

                        headers.setContentType(
                                        MediaType.APPLICATION_JSON);

                        Map<String, Object> body = new HashMap<>();

                        body.put("client_id", clientId);
                        body.put("client_secret", clientSecret);
                        body.put("grant_type", "client_credentials");

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                        ResponseEntity<JsonNode> response = restTemplate.exchange(
                                        authUrl,
                                        HttpMethod.POST,
                                        entity,
                                        JsonNode.class);

                        JsonNode responseBody = response.getBody();

                        if (responseBody == null) {

                                throw new RuntimeException(
                                                "Respuesta OAuth vacia");
                        }

                        String accessToken = responseBody
                                        .get("access_token")
                                        .asText();

                        /**
                         * Guardar token cache.
                         */
                        cachedAccessToken = accessToken;

                        /**
                         * Calcular expiración.
                         */
                        tokenExpirationTime = LocalDateTime.now()
                                        .plusSeconds(
                                                        tokenExpirationSeconds);

                        log.info(
                                        "[SKYDROPX-AUTH] token OAuth cacheado expiration={}",
                                        tokenExpirationTime);

                        return cachedAccessToken;

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-AUTH] error obteniendo token",
                                        ex);

                        /**
                         * Limpiar cache inválido.
                         */
                        cachedAccessToken = null;
                        tokenExpirationTime = null;

                        throw new RuntimeException(
                                        "Error obteniendo token SkyDropX");
                }
        }

        /**
         * Forzar invalidación token.
         *
         * Utilizado cuando SkyDropX
         * responde 401 Unauthorized.
         */
        public synchronized void invalidateToken() {

                log.warn(
                                "[SKYDROPX-AUTH] invalidando token cache");

                cachedAccessToken = null;
                tokenExpirationTime = null;
        }
}