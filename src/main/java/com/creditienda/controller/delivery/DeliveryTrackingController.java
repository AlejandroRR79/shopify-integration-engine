package com.creditienda.controller.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.delivery.DeliveryTrackingDAOService;

@RestController

@RequestMapping("/api/secure/delivery")
public class DeliveryTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryTrackingController.class);

    private final DeliveryTrackingDAOService deliveryTrackingDAOService;

    public DeliveryTrackingController(DeliveryTrackingDAOService deliveryTrackingDAOService) {
        this.deliveryTrackingDAOService = deliveryTrackingDAOService;
    }

    /**
     * Dispara manualmente la sincronización de estatus de entrega vía DAO (Estafeta
     * → DB).
     * Requiere JWT Bearer token.
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<String> sincronizarEstatusEntregas(Authentication authentication) {

        logger.info("🚀 Sincronización manual solicitada por: {}", authentication.getName());

        try {
            deliveryTrackingDAOService.sincronizarEstatusEntregas();
            return ResponseEntity.ok("✅ Sincronización de estatus de entrega completada");
        } catch (Exception e) {
            logger.error("❌ Error en sincronización manual de entrega", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al sincronizar estatus de entrega: " + e.getMessage());
        }
    }
}
