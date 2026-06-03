package com.creditienda.service.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrackingOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(TrackingOrchestratorService.class);

    @Value("${tracking.mode:b2b}")
    private String trackingMode;

    private final DeliveryTrackingService b2bService;
    private final DeliveryTrackingDAOService daoService;

    public TrackingOrchestratorService(
            DeliveryTrackingService b2bService,
            DeliveryTrackingDAOService daoService) {
        this.b2bService = b2bService;
        this.daoService = daoService;
    }

    public void ejecutarSincronizacion() {

        String mode = trackingMode != null ? trackingMode.trim().toLowerCase() : "b2b";

        log.info("🔀 tracking.mode=[{}]", mode);

        if ("dao".equals(mode)) {
            log.info("➡ Ejecutando pipeline DAO");
            daoService.sincronizarEstatusEntregas();
        } else {
            log.info("➡ Ejecutando pipeline B2B");
            b2bService.sincronizarEstatusEntregas();
        }
    }
}