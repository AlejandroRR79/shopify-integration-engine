package com.creditienda.service;

import java.util.List;
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

import com.creditienda.dto.estafeta.seguro.EstafetaSeguroResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EstafetaSeguroClient {

    private static final Logger logger = LoggerFactory.getLogger(EstafetaSeguroClient.class);

    private volatile String cachedToken;
    private volatile long tokenExpirationTime;

    @Value("${estafeta.seguro.token.url}")
    private String tokenUrl;

    @Value("${estafeta.seguro.client.id}")
    private String clientId;

    @Value("${estafeta.seguro.client.secret}")
    private String clientSecret;

    @Value("${estafeta.seguro.scope}")
    private String scope;

    @Value("${estafeta.seguro.grant.type}")
    private String grantType;

    @Value("${estafeta.seguro.api.url}")
    private String apiUrl;

    @Value("${estafeta.seguro.apikey}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public EstafetaSeguroClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public EstafetaSeguroResponseDTO.Item consultarSeguro(String code) {

        logger.info("==================================================");
        logger.info("🛡️ CONSULTA SEGURO ESTAFETA | code={}", code);

        try {

            String token = obtenerToken();
            logger.debug("🔑 Token parcial={}...",
                    token != null ? token.substring(0, 20) : "NULL");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            headers.set("apikey", apiKey);

            Map<String, Object> body = Map.of(
                    "Item", List.of(Map.of("Code", code)),
                    "Language", 1);

            logger.info("🌐 URL Seguro={}", apiUrl);
            logger.info("📤 Request Body={}", body);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            logger.info("📥 HTTP Status={}", rawResponse.getStatusCode());

            if (!rawResponse.getStatusCode().is2xxSuccessful()) {
                logger.error("❌ HTTP ERROR");
                return null;
            }

            EstafetaSeguroResponseDTO dto = mapper.readValue(
                    rawResponse.getBody(),
                    EstafetaSeguroResponseDTO.class);

            if (dto == null) {
                logger.warn("⚠ DTO null");
                return null;
            }

            if (dto.getResult() != null) {
                logger.info("📊 Result.Code={}", dto.getResult().getCode());
                logger.info("📊 Result.Description={}", dto.getResult().getDescription());
            }

            if (dto.getItem() == null || dto.getItem().isEmpty()) {
                logger.warn("⚠ SIN ITEMS EN RESPUESTA");
                return null;
            }

            EstafetaSeguroResponseDTO.Item item = dto.getItem().get(0);

            if (item.getError() != null) {
                logger.warn("⚠ Error interno Estafeta | code={} | desc={}",
                        item.getError().getCode(),
                        item.getError().getDescription());
            }

            if (item.getCIA() == null) {
                logger.warn("⚠ CIA null");
                return null;
            }

            String pdf = item.getCIA().getInsurancePDF();

            if (pdf == null) {
                logger.warn("⚠ InsurancePDF null");
                return null;
            }

            logger.info("📄 InsurancePDF length={}", pdf.length());

            if (pdf.isBlank()) {
                logger.warn("⚠ InsurancePDF vacío");
                return null;
            }

            logger.info("🛡️ GUÍA CON SEGURO CONFIRMADO");
            logger.info("==================================================");

            return item;

        } catch (Exception e) {

            logger.error("❌ EXCEPCIÓN EN CONSULTA SEGURO", e);
            logger.info("==================================================");
            return null;
        }
    }

    private String obtenerToken() {

        long now = System.currentTimeMillis();

        if (cachedToken != null && now < tokenExpirationTime) {
            logger.debug("🔁 Token en caché");
            return cachedToken;
        }

        synchronized (this) {

            if (cachedToken != null && now < tokenExpirationTime) {
                return cachedToken;
            }

            logger.info("🔐 Generando nuevo token SEGURO...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

            params.add("grant_type", grantType);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("scope", scope);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

                logger.error("❌ ERROR OBTENIENDO TOKEN | status={}",
                        response.getStatusCode());
                throw new RuntimeException("Error token seguro");
            }

            cachedToken = (String) response.getBody().get("access_token");

            Number expiresIn = (Number) response.getBody().get("expires_in");

            tokenExpirationTime = now + (expiresIn.longValue() - 60) * 1000;

            logger.info("🔑 Token nuevo generado");

            return cachedToken;
        }
    }
}
