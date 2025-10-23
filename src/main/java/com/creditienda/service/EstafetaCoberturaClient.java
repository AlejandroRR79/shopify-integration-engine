package com.creditienda.service;

import java.util.Map;

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
public class EstafetaCoberturaClient {

    @Value("${estafeta.cobertura.token.url}")
    private String tokenUrl;

    @Value("${estafeta.cobertura.client.id}")
    private String clientId;

    @Value("${estafeta.cobertura.client.secret}")
    private String clientSecret;

    @Value("${estafeta.cobertura.scope}")
    private String scope;

    @Value("${estafeta.cobertura.grant.type}")
    private String grantType;

    @Value("${estafeta.cobertura.api.url}")
    private String apiUrl;

    @Value("${estafeta.cobertura.apikey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedToken;
    private long tokenExpiration;

    public String consultarCobertura(String origen, String destino) {
        String token = obtenerToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("apikey", apiKey);

        String body = String.format("""
                {
                  "frequencies": [{
                    "origins": [{"postalCode": "%s"}],
                    "destinations": [{"postalCode": "%s"}]
                  }]
                }
                """, origen, destino);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    private String obtenerToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiration) {
            return cachedToken;
        }

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
            Integer expiresIn = (Integer) response.getBody().get("expires_in");
            tokenExpiration = now + (expiresIn - 60) * 1000; // restamos 60s por seguridad return cachedToken;
            return cachedToken;

        } else {
            throw new RuntimeException("Error al obtener token de Estafeta");
        }
    }
}