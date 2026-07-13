package com.creditienda.controller.estafeta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.estafeta.seguro.EstafetaSeguroResponseDTO;
import com.creditienda.service.EstafetaSeguroClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Estafeta - Seguro", description = "Consulta de seguro asociado a una guia Estafeta. Requiere JWT.")
@RestController
@RequestMapping("/api/estafeta/seguro")
public class SeguroEstafetaController {

    private static final Logger logger = LoggerFactory.getLogger(SeguroEstafetaController.class);

    private final EstafetaSeguroClient seguroClient;

    public SeguroEstafetaController(EstafetaSeguroClient seguroClient) {
        this.seguroClient = seguroClient;
    }

    @Operation(summary = "Consultar seguro de guia", description = "Retorna informacion del seguro asociado al codigo de guia Estafeta. GET /api/estafeta/seguro/{code}")
    @GetMapping("/{code}")
    public ResponseEntity<?> consultarSeguro(
            @PathVariable String code,
            Authentication authentication) {

        if (authentication == null) {
            logger.warn("🚫 No autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No autenticado");
        }

        logger.info("🔎 Usuario={} consulta seguro code={}",
                authentication.getName(), code);

        EstafetaSeguroResponseDTO.Item result = seguroClient.consultarSeguro(code);

        if (result == null) {
            logger.info("🛡️ Sin seguro | code={}", code);
            return ResponseEntity.ok("Guía sin seguro");
        }

        logger.info("🛡️ Con seguro | code={}", code);
        return ResponseEntity.ok(result);
    }
}
