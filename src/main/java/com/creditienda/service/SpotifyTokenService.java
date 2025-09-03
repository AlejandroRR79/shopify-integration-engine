package com.creditienda.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Base64;

@Service
public class SpotifyTokenService {

    @Value("${spotify.token.url}")
    private String tokenUrl;

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;


    private final WebClient webClient = WebClient.builder().build();

    @SuppressWarnings("unchecked")
    public String obtenerToken() {
        String credentials = Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes());

        Mono<Map> response = webClient.post()
            .uri(tokenUrl)
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=client_credentials")
            .retrieve()
            .bodyToMono(Map.class);

        Map<String, Object> tokenMap = response.block();
        return tokenMap != null ? tokenMap.get("access_token").toString() : null;
    }

}