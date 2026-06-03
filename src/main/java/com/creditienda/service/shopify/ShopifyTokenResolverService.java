package com.creditienda.service.shopify;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.config.ShopifyMultiStoreProperties.ShopifyStoreConfig;

/**
 * Resuelve el access token por tienda.
 *
 * TOKEN → devuelve el access-token directo del properties.
 * OAUTH → solicita token con client_credentials, lo cachea y lo renueva
 *         300 segundos antes de que expire (expires_in de Shopify ~86400s).
 */
@Service
public class ShopifyTokenResolverService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyTokenResolverService.class);

    /** Segundos de margen antes de expiración para renovar el token */
    private static final long REFRESH_BUFFER_SECONDS = 300;

    private final RestTemplate restTemplate;

    private final Map<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    public ShopifyTokenResolverService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String resolverToken(ShopifyStoreConfig store) {

        if ("TOKEN".equalsIgnoreCase(store.getAuthType())) {
            return store.getAccessToken();
        }

        if ("OAUTH".equalsIgnoreCase(store.getAuthType())) {
            TokenEntry entry = tokenCache.get(store.getAlias());

            if (entry == null || entry.isExpired()) {
                TokenEntry nuevo = obtenerTokenOAuth(store);
                tokenCache.put(store.getAlias(), nuevo);
                return nuevo.token;
            }

            return entry.token;
        }

        throw new IllegalArgumentException(
                "auth-type no soportado: " + store.getAuthType()
                        + " para tienda=" + store.getAlias());
    }

    public void invalidarToken(String alias) {
        tokenCache.remove(alias);
        log.info("[SHOPIFY-TOKEN] cache invalidado alias={}", alias);
    }

    @SuppressWarnings("unchecked")
    private TokenEntry obtenerTokenOAuth(ShopifyStoreConfig store) {

        log.info("[SHOPIFY-TOKEN] obteniendo token OAUTH alias={}", store.getAlias());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "client_id", store.getClientId(),
                "client_secret", store.getClientSecret(),
                "grant_type", "client_credentials");

        Map<String, Object> response = restTemplate.postForObject(
                store.getTokenUrl(),
                new HttpEntity<>(body, headers),
                Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException(
                    "No se obtuvo access_token para tienda=" + store.getAlias());
        }

        String token = response.get("access_token").toString();

        long expiresIn = response.containsKey("expires_in")
                ? ((Number) response.get("expires_in")).longValue()
                : 86400L;

        Instant expiry = Instant.now().plusSeconds(expiresIn - REFRESH_BUFFER_SECONDS);

        log.info("[SHOPIFY-TOKEN] token obtenido alias={} expira en {}s (refresh a los {}s)",
                store.getAlias(), expiresIn, expiresIn - REFRESH_BUFFER_SECONDS);

        return new TokenEntry(token, expiry);
    }

    private static class TokenEntry {
        final String token;
        final Instant expiry;

        TokenEntry(String token, Instant expiry) {
            this.token = token;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
