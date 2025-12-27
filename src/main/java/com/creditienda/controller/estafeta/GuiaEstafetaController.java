package com.creditienda.controller.estafeta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.EstafetaGuiaClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/estafeta/guia")
public class GuiaEstafetaController {

    private static final Logger logger = LoggerFactory.getLogger(GuiaEstafetaController.class);

    private final EstafetaGuiaClient estafetaGuiaClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${estafeta.guia.effective-date.offset-days}")
    private int effectiveDateOffsetDays;

    public GuiaEstafetaController(EstafetaGuiaClient estafetaGuiaClient) {
        this.estafetaGuiaClient = estafetaGuiaClient;
    }

    @PostMapping("/secure")
    public ResponseEntity<String> generarGuiaProtegida(
            @RequestBody String jsonBody,
            Authentication authentication) {

        return procesarGuia(jsonBody, authentication.getName());
    }

    // ===================== CORE =====================

    private ResponseEntity<String> procesarGuia(
            String jsonBody,
            String origenPeticion) {

        try {
            logger.info("Generando guía ({})", origenPeticion);

            String effectiveDate = LocalDate.now()
                    .plusDays(effectiveDateOffsetDays)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            jsonBody = jsonBody.replaceAll(
                    "\"effectiveDate\"\\s*:\\s*\"[^\"]*\"",
                    "\"effectiveDate\":\"" + effectiveDate + "\"");

            String respuesta = estafetaGuiaClient.generarGuia(jsonBody);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            logger.error("Error al generar guía", e);

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // ===================== ERROR JSON =====================

    private String buildErrorJson(String message) {
        try {
            return mapper.writeValueAsString(
                    new ErrorResponse(false, message));
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Error inesperado\"}";
        }
    }

    // DTO interno simple
    static class ErrorResponse {
        public boolean success;
        public String message;

        public ErrorResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}