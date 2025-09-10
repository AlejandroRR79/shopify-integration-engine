package com.creditienda.service;

import java.util.Collections;
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

@Service
public class ShopifyProductService {

    private static final Logger logger = LogManager.getLogger(ShopifyProductService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> obtenerProductos() {
        String url = String.format("https://%s/admin/api/%s/products.json", shopDomain, apiVersion);
        logger.info("🔗 Consultando productos desde Shopify: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("❌ Error al consultar productos: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "No se pudo obtener el listado de productos desde Shopify.");
        }
    }
}