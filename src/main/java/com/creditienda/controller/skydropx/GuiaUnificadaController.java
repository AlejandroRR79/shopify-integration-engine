package com.creditienda.controller.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.GuiaUnificadaResponseDTO;
import com.creditienda.service.skydropx.GuiaUnificadaService;
import com.creditienda.service.skydropx.GuiaUnificadaService.AllCarriersFailedException;

@RestController
@RequestMapping("/api/secure/skydropx")
public class GuiaUnificadaController {

    private static final Logger log = LogManager.getLogger(GuiaUnificadaController.class);

    private final GuiaUnificadaService guiaUnificadaService;

    public GuiaUnificadaController(GuiaUnificadaService guiaUnificadaService) {
        this.guiaUnificadaService = guiaUnificadaService;
    }

    @PostMapping("/guia")
    public ResponseEntity<?> generarGuia(
            @RequestBody WayBillRequestDTO request,
            Authentication authentication) {

        if (request == null) {
            log.warn("[GUIA-UNIFICADA] request nulo ({})", authentication.getName());
            return ResponseEntity.badRequest().body(Map.of("error", "Request vacío"));
        }

        try {
            log.info("[GUIA-UNIFICADA] generando guia ({})", authentication.getName());
            GuiaUnificadaResponseDTO response = guiaUnificadaService.generarGuia(request);
            return ResponseEntity.ok(response);

        } catch (AllCarriersFailedException ex) {
            log.error("[GUIA-UNIFICADA] todos los carriers fallaron");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getResponse());

        } catch (Exception ex) {
            log.error("[GUIA-UNIFICADA] error generando guia: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
