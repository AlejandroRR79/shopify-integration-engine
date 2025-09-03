package com.creditienda.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.creditienda.service.OAuthTokenService;

@RestController
public class ServicioProtegidoController {

    @Autowired
    private OAuthTokenService tokenService;

    @Value("${api.protected.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    @SuppressWarnings("unchecked")
    @GetMapping("/consumir-api")
    public Map<String, Object> consumirApiProtegida() {
        String token = tokenService.obtenerToken();

        return webClient.get()
            .uri(apiUrl)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}