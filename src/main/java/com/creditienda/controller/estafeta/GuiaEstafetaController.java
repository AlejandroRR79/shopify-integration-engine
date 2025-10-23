package com.creditienda.controller.estafeta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.EstafetaGuiaClient;

@RestController
@RequestMapping("/api/public/guia")
public class GuiaEstafetaController {

    private static final Logger logger = LoggerFactory.getLogger(GuiaEstafetaController.class);

    private final EstafetaGuiaClient estafetaGuiaClient;

    @Value("${estafeta.guia.make.apikey}")
    private String expectedApiKey;

    public GuiaEstafetaController(EstafetaGuiaClient estafetaGuiaClient) {
        this.estafetaGuiaClient = estafetaGuiaClient;
    }

    @PostMapping
    public ResponseEntity<String> generarGuia(
            @RequestHeader(value = "x-make-apikey", required = false) String apiKey,
            @RequestBody String jsonBody) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Header x-make-apikey no proporcionado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Header x-make-apikey requerido");
        }

        if (!expectedApiKey.equals(apiKey)) {
            logger.warn("API Key inválida: {}", apiKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: API Key no autorizada");
        }

        try {
            logger.info("Solicitud recibida para generar guía");
            String respuesta = estafetaGuiaClient.generarGuia(jsonBody);
            logger.info("Respuesta de Estafeta: {}", respuesta);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            logger.error("Error al generar guía: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}