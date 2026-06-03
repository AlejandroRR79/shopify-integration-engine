package com.creditienda.controller.estafeta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.EstafetHistorialClient;

@RestController
@RequestMapping("/api/estafeta/historial")
public class HistorialEstafetaController {

    private static final Logger logger = LoggerFactory.getLogger(HistorialEstafetaController.class);

    private final EstafetHistorialClient estafetHistorialClient;

    public HistorialEstafetaController(EstafetHistorialClient estafetHistorialClient) {
        this.estafetHistorialClient = estafetHistorialClient;
    }

    @PostMapping
    public ResponseEntity<String> consultarHistorial(
            @RequestBody(required = false) String jsonBody,
            Authentication authentication) {

        // 🔐 Seguridad: JWT obligatorio
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No autenticado");
        }

        try {
            logger.info("Consulta historial Estafeta solicitada por: {}", authentication.getName());

            if (jsonBody == null || jsonBody.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Error: JSON del cuerpo requerido");
            }

            // 🔁 Passthrough puro (no se manipula respuesta de Estafeta)
            String respuesta = estafetHistorialClient.consultarHistorial(jsonBody);

            logger.info("Respuesta de Estafeta (historial) recibida correctamente");
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            logger.error("Error al consultar historial Estafeta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
