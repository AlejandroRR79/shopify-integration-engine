package com.creditienda.controller.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.skydropx.SkyDropXRecoveryService;

@RestController
@RequestMapping("/api/secure/skydropx")
public class SkyDropXRecoveryController {

    private static final Logger log = LogManager.getLogger(SkyDropXRecoveryController.class);

    private final SkyDropXRecoveryService recoveryService;

    public SkyDropXRecoveryController(SkyDropXRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

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
