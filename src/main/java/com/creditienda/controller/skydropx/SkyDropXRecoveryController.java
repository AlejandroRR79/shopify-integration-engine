package com.creditienda.controller.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.skydropx.SkyDropXRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "SkyDropX Recovery", description = "Ejecucion manual del proceso de recovery de guias SkyDropX atascadas. Requiere JWT.")
@RestController
@RequestMapping("/api/secure/skydropx")
public class SkyDropXRecoveryController {

    private static final Logger log = LogManager.getLogger(SkyDropXRecoveryController.class);

    private final SkyDropXRecoveryService recoveryService;

    public SkyDropXRecoveryController(SkyDropXRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @Operation(summary = "Ejecutar recovery manual", description = "Reintenta guias SkyDropX en estado atascado (stuck processes)")
    @PostMapping("/recovery/run")
    public ResponseEntity<String> ejecutarRecovery() {
        try {
            log.info("[SKYDROPX-RECOVERY-CTRL] ejecucion manual iniciada");
            recoveryService.ejecutarRecovery();
            return ResponseEntity.ok("Recovery ejecutado correctamente");
        } catch (Exception ex) {
            log.error("[SKYDROPX-RECOVERY-CTRL] error en ejecucion manual", ex);
            return ResponseEntity.internalServerError()
                    .body("Error en recovery: " + ex.getMessage());
        }
    }
}
