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
import com.creditienda.service.facturacion.DeliveryFacturacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Delivery", description = "Sincronizacion de estatus de entrega y facturacion. Requiere JWT.")
@RestController
@RequestMapping("/api/secure/delivery")
public class DeliveryTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryTrackingController.class);

    private final DeliveryTrackingDAOService deliveryTrackingDAOService;

    private final DeliveryFacturacionService deliveryFacturacionService;

    public DeliveryTrackingController(DeliveryTrackingDAOService deliveryTrackingDAOService,
            DeliveryFacturacionService deliveryFacturacionService) {
        this.deliveryTrackingDAOService = deliveryTrackingDAOService;
        this.deliveryFacturacionService = deliveryFacturacionService;

    }

    /**
     * Dispara manualmente la sincronización de estatus de entrega vía DAO (Estafeta
     * → DB).
     * Requiere JWT Bearer token.
     */
    @Operation(summary = "Sincronizar estatus de entregas", description = "Dispara manualmente la sincronizacion de estatus Estafeta hacia BD.")
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

    /**
     * Dispara manualmente la factruacion de entregas. Esto incluye la generación de
     * facturas y su timbrado.
     * Requiere JWT Bearer token.
     */

    @Operation(summary = "Ejecutar facturacion de entregas", description = "Dispara manualmente la generacion y timbrado de facturas de entregas completadas.")
    @PostMapping("/facturar")
    public ResponseEntity<String> ejecutarFacturacion(Authentication authentication) {

        logger.info("📄 Facturación manual solicitada por: {}", authentication.getName());

        try {

            deliveryFacturacionService.ejecutarFacturacion();

            return ResponseEntity.ok("✅ Facturación ejecutada correctamente");

        } catch (Exception e) {

            logger.error("❌ Error en facturación manual", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en facturación: " + e.getMessage());
        }
    }
}
