package com.creditienda.controller;

import com.creditienda.service.SpotifyTokenService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
public class SpotifyController {

    private static final Logger logger = LogManager.getLogger(SpotifyController.class);

    @Autowired
    private SpotifyTokenService tokenService;

    @Value("${spotify.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    @SuppressWarnings("unchecked")
    @GetMapping("/spotify")
    public Map<String, Object> buscarArtista() {
        logger.info("Iniciando búsqueda de artista en Spotify...");

        String token = tokenService.obtenerToken();
        logger.debug("Token obtenido: {}", token);

        Map<String, Object> respuesta = webClient.get()
            .uri(apiUrl)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(Map.class)
            .doOnError(e -> logger.error("Error al consumir el API de Spotify", e))
            .block();

        logger.info("Respuesta recibida de Spotify: {}", respuesta);
        return respuesta;
    }
}