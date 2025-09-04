package com.creditienda.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Service
public class OAuthTokenService {

    @Value("${oauth.token.url}")
    private String tokenUrl;

    @Value("${oauth.client.id}")
    private String clientId;

    @Value("${oauth.client.secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.builder().build();

    @PostConstruct
    public void validarVariables() {
        System.out.println("🔍 tokenUrl: " + tokenUrl);
        System.out.println("🔍 clientId: " + clientId);
        System.out.println("🔍 clientSecret: " + clientSecret);
    }

    @SuppressWarnings("unchecked")
    public String obtenerToken() {
        Mono<Map> response = webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
                .retrieve()
                .bodyToMono(Map.class);

        Map<String, Object> tokenMap = response.block();
        return tokenMap != null ? tokenMap.get("access_token").toString() : null;
    }
}