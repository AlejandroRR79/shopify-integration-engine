package com.creditienda.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private String cachedToken;
    private long tokenExpirationTime; // epoch millis

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

    @Value("${estafeta.search.type}")
    private int searchType;

    @Value("${estafeta.client.number}")
    private String clientNumber;

    @Value("${estafeta.reference.code}")
    private String referenceCode;

    @Value("${estafeta.items.search}")
    private String itemsSearch;

    private final RestTemplate restTemplate = new RestTemplate();

    public String consultarHistorial() {
        logger.info("🔄 Iniciando consulta a Estafeta...");

        String token = obtenerToken();
        logger.info("🔐 Token obtenido correctamente");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("apikey", apiKey);

        String body = String.format("""
                {
                  "inputType": %d,
                  "itemReference": {
                    "clientNumber": "%s",
                    "referenceCode": ["%s"]
                  },
                  "itemsSearch": ["%s"],
                  "searchType": %d
                }
                """, inputType, clientNumber, referenceCode, itemsSearch, searchType);

        logger.info("📤 Enviando solicitud a Estafeta con cuerpo: {}", body);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        logger.info("📥 Respuesta recibida de Estafeta: {}", response.getBody());
        logger.info("✅ Consulta a Estafeta finalizada");
        return response.getBody(); //
    }

    private String obtenerToken() {
        long now = System.currentTimeMillis();

        if (cachedToken != null && now < tokenExpirationTime) {
            logger.info("🔁 Usando token en caché");
            return cachedToken;
        }

        logger.info("🔐 Solicitando nuevo token OAuth2...");

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

            // Establecer expiración (ej. 55 minutos desde ahora)
            Integer expiresIn = (Integer) response.getBody().get("expires_in");
            tokenExpirationTime = now + (expiresIn - 60) * 1000; // restamos 60s por seguridad

            logger.info("🔑 Nuevo token recibido y almacenado en caché");
            return cachedToken;
        } else {
            logger.error("❌ Error al obtener token: {}", response.getStatusCode());
            throw new RuntimeException("Error al obtener token");
        }
    }
}