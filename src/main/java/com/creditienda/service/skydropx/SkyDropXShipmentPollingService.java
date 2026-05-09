package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.creditienda.dto.skydropx.SkyDropXShipmentResponseDTO;

/**
 * Servicio encargado de esperar
 * hasta que el shipment quede listo.
 *
 * Se utiliza polling porque
 * SkyDropX es eventual consistency.
 */
@Service
public class SkyDropXShipmentPollingService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXShipmentPollingService.class);

    /**
     * Máximo intentos polling.
     */
    private static final int MAX_RETRIES = 6;

    /**
     * Espera entre intentos.
     */
    private static final int WAIT_SECONDS = 5;

    private final SkyDropXShipmentClientService skyDropXShipmentClientService;

    public SkyDropXShipmentPollingService(
            SkyDropXShipmentClientService skyDropXShipmentClientService) {

        this.skyDropXShipmentClientService = skyDropXShipmentClientService;
    }

    /**
     * Esperar shipment listo.
     *
     * Condiciones:
     * - workflow_status=success
     * - label_url != null
     *
     * @param shipmentId shipment generado
     * @return shipment completo
     */
    public SkyDropXShipmentResponseDTO waitForShipment(
            String shipmentId) {

        try {

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

                SkyDropXShipmentResponseDTO shipmentResponse = skyDropXShipmentClientService
                        .getShipment(
                                shipmentId);

                String workflowStatus = shipmentResponse
                        .getData()
                        .getAttributes()
                        .getWorkflowStatus();

                String labelUrl = shipmentResponse
                        .getLabelUrl();

                log.info(
                        "[SKYDROPX-SHIPMENT-POLLING] shipmentId={} attempt={} workflowStatus={}",
                        shipmentId,
                        attempt,
                        workflowStatus);

                /**
                 * Shipment listo.
                 */
                if ("success".equalsIgnoreCase(
                        workflowStatus)
                        && labelUrl != null
                        && !labelUrl.isBlank()) {

                    log.info(
                            "[SKYDROPX-SHIPMENT-POLLING] shipment listo shipmentId={}",
                            shipmentId);

                    return shipmentResponse;
                }

                /**
                 * Esperar siguiente intento.
                 */
                Thread.sleep(
                        WAIT_SECONDS * 1000L);
            }

            throw new RuntimeException(
                    "Shipment no completado después de polling");

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-SHIPMENT-POLLING] error shipmentId={}",
                    shipmentId,
                    ex);

            throw new RuntimeException(
                    "Error polling shipment");
        }
    }
}