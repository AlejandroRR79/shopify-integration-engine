package com.creditienda.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class EstafetHistorialClient {

    private static final Logger logger = LoggerFactory.getLogger(EstafetHistorialClient.class);

    // 🔒 Ajuste leve: volatile para concurrencia
    private volatile String cachedToken;
    private volatile long tokenExpirationTime;

    @Value("${estafeta.token.url}")
    private String tokenUrl;

    @Value("${estafeta.client.id}")
    private String clientId;

    @Value("${estafeta.client.secret}")
    private String clientSecret;

    @Value("${estafeta.scope}")
    private String scope;

    @Value("${estafeta.grant.type}")
    private String grantType;

    @Value("${estafeta.api.url}")
    private String apiUrl;

    @Value("${estafeta.apikey}")
    private String apiKey;

    @Value("${estafeta.input.type}")
    private int inputType;

    @Autowired
    private RestTemplate restTemplate;

    // 🔹 Método 1: recibe JSON completo
    public String consultarHistorial(String jsonBody) {
        logger.info("🔄 Consulta con JSON personalizado");
        return ejecutarConsulta(jsonBody);
    }

    // 🔹 Método 2: recibe solo el número de referencia
    public String consultarHistorialNumReferencia(String itemsSearch) {
        logger.info("🔄 Consulta con número de referencia");

        String body = String.format("""
                {
                  "inputType": %d,
                  "itemsSearch": ["%s"],
                  "searchType": 0
                }
                """, inputType, itemsSearch);

        logger.info("📤 JSON generado:\n{}", body);
        return ejecutarConsulta(body);
    }

    // 🔒 Método privado común para ejecutar la consulta
    private String ejecutarConsulta(String body) {

        String token = obtenerToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("apikey", apiKey);

        // 🔎 LOG: URL destino
        logger.info("🌐 Estafeta URL: {}", apiUrl);
        logger.info("RestTemplate usado={}", restTemplate.getClass());

        // 🔎 LOG: Headers (sin exponer token completo)
        logger.info("📨 Headers enviados: Content-Type={}, apikey={}",
                headers.getContentType(),
                apiKey);

        // 🔎 LOG: Body enviado
        logger.info("📤 Body enviado a Estafeta:\n{}", body);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        // 🔎 LOG: HTTP status
        logger.info("📥 Estafeta HTTP Status: {}", response.getStatusCode());

        // 🔎 LOG: Respuesta
        logger.debug("📥 Respuesta Estafeta:\n{}", response.getBody());

        return response.getBody();
    }

    // 🔐 Obtención de token OAuth2 con caché (NO se pide hasta expirar)
    private String obtenerToken() {
        long now = System.currentTimeMillis();

        // ✅ Fast-path: no sincroniza si aún es válido
        if (cachedToken != null && now < tokenExpirationTime) {
            logger.debug("🔁 Usando token en caché");
            return cachedToken;
        }

        // 🔒 Ajuste leve: synchronized SOLO al renovar
        synchronized (this) {

            // Double-check
            if (cachedToken != null && now < tokenExpirationTime) {
                return cachedToken;
            }

            logger.info("🔐 Token expirado o inexistente, solicitando nuevo token OAuth2...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", grantType);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("scope", scope);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                cachedToken = (String) response.getBody().get("access_token");

                // 🔧 Ajuste leve: Number en vez de Integer
                Number expiresIn = (Number) response.getBody().get("expires_in");

                // margen de seguridad 60s
                tokenExpirationTime = now + (expiresIn.longValue() - 60) * 1000;

                logger.info("🔑 Nuevo token recibido y almacenado en caché");
                return cachedToken;
            }

            logger.error("❌ Error al obtener token OAuth2: {}", response.getStatusCode());
            throw new RuntimeException("Error al obtener token OAuth2");
        }
    }
}
