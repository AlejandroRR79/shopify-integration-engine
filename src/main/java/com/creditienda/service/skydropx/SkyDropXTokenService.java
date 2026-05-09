package com.creditienda.service.skydropx;

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

    private static final Logger log = LogManager.getLogger(SkyDropXTokenService.class);

    @Value("${skydropx.auth-url}")
    private String authUrl;

    @Value("${skydropx.client-id}")
    private String clientId;

    @Value("${skydropx.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public SkyDropXTokenService(
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {

        try {

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

            String accessToken = responseBody.get("access_token")
                    .asText();

            log.info(
                    "[SKYDROPX-AUTH] token obtenido correctamente");

            return accessToken;

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-AUTH] error obteniendo token",
                    ex);

            throw new RuntimeException(
                    "Error obteniendo token SkyDropX");
        }
    }
}