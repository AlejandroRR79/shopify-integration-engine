package com.creditienda.controller.estafeta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.CoberturaRequest;
import com.creditienda.service.EstafetaCoberturaClient;

@RestController
@RequestMapping("/api/public/cobertura")
public class CoberturaEstafetaController {

    private static final Logger logger = LoggerFactory.getLogger(CoberturaEstafetaController.class);

    private final EstafetaCoberturaClient estafetaCoberturaClient;

    @Value("${estafeta.cobertura.make.apikey}")
    private String expectedApiKey;

    public CoberturaEstafetaController(EstafetaCoberturaClient estafetaCoberturaClient) {
        this.estafetaCoberturaClient = estafetaCoberturaClient;
    }

    // 🔓 Endpoint abierto con API Key
    @PostMapping
    public ResponseEntity<String> validarCobertura(
            @RequestHeader(value = "x-make-apikey", required = false) String apiKey,
            @RequestBody CoberturaRequest request) {

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Header x-make-apikey no proporcionado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Header x-make-apikey requerido");
        }

        if (!expectedApiKey.equals(apiKey)) {
            logger.warn("API Key inválida: {}", apiKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: API Key no autorizada");
        }

        return procesarCobertura(request, "APIKey");
    }

    // 🔐 Endpoint protegido con JWT
    @PostMapping("/secure")
    public ResponseEntity<String> validarCoberturaProtegida(
            @RequestBody CoberturaRequest request,
            Authentication authentication) {

        String usuario = authentication.getName(); // extraído del token
        logger.info("Petición autenticada por JWT: {}", usuario);

        return procesarCobertura(request, usuario);
    }

    // 🔁 Lógica compartida
    private ResponseEntity<String> procesarCobertura(CoberturaRequest request, String origenPeticion) {
        try {
            String origen = request.getFrequencies().get(0).getOrigins().get(0).getPostalCode();
            String destino = request.getFrequencies().get(0).getDestinations().get(0).getPostalCode();

            logger.info("Consultando cobertura ({}): origen={}, destino={}", origenPeticion, origen, destino);

            String respuesta = estafetaCoberturaClient.consultarCobertura(origen, destino);
            logger.info("Respuesta de Estafeta: {}", respuesta);

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            logger.error("Error al consultar cobertura: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}