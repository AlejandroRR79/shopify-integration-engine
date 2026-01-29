package com.creditienda.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.estafeta.guia.ItemDescriptionDTO;
import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Service
public class EstafetaGuiaClient {

    @Value("${estafeta.guia.token.url}")
    private String tokenUrl;

    @Value("${estafeta.guia.client.id}")
    private String clientId;

    @Value("${estafeta.guia.client.secret}")
    private String clientSecret;

    @Value("${estafeta.guia.scope}")
    private String scope;

    @Value("${estafeta.guia.grant.type}")
    private String grantType;

    @Value("${estafeta.guia.api.url}")
    private String apiUrl;

    @Value("${estafeta.guia.api.query}")
    private String apiQuery;

    @Value("${estafeta.guia.apikey}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(EstafetaGuiaClient.class);

    @Autowired
    private ObjectMapper objectMapper;

    // 🔒 Cache token (thread-safe)
    private volatile String cachedToken;
    private volatile long tokenExpiration;

    // 🔒 Lock dedicado
    private final Object tokenLock = new Object();

    public String generarGuia(WayBillRequestDTO request) {

        String token = obtenerToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("apikey", apiKey);

        String fullUrl = apiUrl + "?" + apiQuery;

        try {

            if (request != null
                    && request.getLabelDefinition() != null
                    && request.getLabelDefinition().getItemDescription() != null) {

                ItemDescriptionDTO item = request.getLabelDefinition().getItemDescription();

                item.setHeight(normalizeIntegerDimension(item.getHeight()));
                item.setLength(normalizeIntegerDimension(item.getLength()));
                item.setWidth(normalizeIntegerDimension(item.getWidth()));
            }

            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            log.info("JSON enviado a Estafeta: {}",
                    objectMapper.writeValueAsString(request));
            HttpEntity<WayBillRequestDTO> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new RuntimeException(
                        "Respuesta vacía de Estafeta");
            }

            log.info("Se genera guia correctamente");
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 🔥 DEVOLVER EL ERROR EXACTO DE ESTAFETA (SIN MODIFICAR)
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar JSON de Estafeta", e);
        }
    }

    private String obtenerToken() {
        long now = System.currentTimeMillis();

        // 🚀 Fast path (sin lock)
        if (cachedToken != null && now < tokenExpiration) {
            return cachedToken;
        }

        synchronized (tokenLock) {
            now = System.currentTimeMillis();

            // 🔁 Double-check
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

                // 🔧 Soporta Integer / Long
                Number expiresIn = (Number) response.getBody().get("expires_in");

                // margen de seguridad 60s
                tokenExpiration = now + (expiresIn.longValue() - 60) * 1000;

                return cachedToken;
            }

            throw new RuntimeException("Error al obtener token de Estafeta");
        }
    }

    private String normalizeIntegerDimension(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value)
                .setScale(0, RoundingMode.DOWN)
                .toPlainString();
    }
}