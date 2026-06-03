package com.creditienda.service.skydropx;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.skydropx.SkyDropXShipmentResponseDTO;
import com.creditienda.service.skydropx.constants.SkyDropXProcessSupersededException;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;

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
        @Value("${skydropx.shipment.polling.max-attempts}")
        private int maxAttempts;

        /**
         * Espera entre intentos.
         */
        @Value("${skydropx.shipment.polling.delay-seconds}")
        private int waitSeconds;

        private final SkyDropXShipmentClientService skyDropXShipmentClientService;
        private final SkyDropXProcessDAO skyDropXProcessDAO;

        public SkyDropXShipmentPollingService(
                        SkyDropXShipmentClientService skyDropXShipmentClientService,
                        SkyDropXProcessDAO skyDropXProcessDAO) {

                this.skyDropXShipmentClientService = skyDropXShipmentClientService;
                this.skyDropXProcessDAO = skyDropXProcessDAO;
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
                        String shipmentId,
                        String quotationId) {

                try {

                        log.info(
                                        "[SKYDROPX-SHIPMENT-POLLING] iniciando polling shipmentId={} maxAttempts={} waitSeconds={}",
                                        shipmentId,
                                        maxAttempts,
                                        waitSeconds);

                        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                                SkyDropXShipmentResponseDTO shipmentResponse = skyDropXShipmentClientService
                                                .getShipment(shipmentId);

                                String workflowStatus = shipmentResponse
                                                .getData()
                                                .getAttributes()
                                                .getWorkflowStatus();

                                String labelUrl = shipmentResponse.getLabelUrl();

                                log.info(
                                                "[SKYDROPX-SHIPMENT-POLLING] shipmentId={} attempt={} workflowStatus={}",
                                                shipmentId,
                                                attempt,
                                                workflowStatus);

                                if ("success".equalsIgnoreCase(workflowStatus)
                                                && labelUrl != null
                                                && !labelUrl.isBlank()) {

                                        log.info(
                                                        "[SKYDROPX-SHIPMENT-POLLING] shipment listo shipmentId={}",
                                                        shipmentId);

                                        return shipmentResponse;
                                }

                                if (attempt == maxAttempts) {
                                        break;
                                }

                                verificarActivo(quotationId);

                                Thread.sleep(waitSeconds * 1000L);
                        }

                        log.error(
                                        "[SKYDROPX-SHIPMENT-POLLING] shipment no completado shipmentId={} attempts={}",
                                        shipmentId,
                                        maxAttempts);

                        throw new RuntimeException(
                                        "Shipment no completado después de polling");

                } catch (SkyDropXProcessSupersededException pse) {

                        throw pse;

                } catch (InterruptedException ie) {

                        Thread.currentThread().interrupt();

                        log.warn(
                                        "[SKYDROPX-SHIPMENT-POLLING] polling interrumpido shipmentId={}",
                                        shipmentId);

                        throw new RuntimeException("Polling shipment interrumpido");

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-SHIPMENT-POLLING] error shipmentId={}",
                                        shipmentId,
                                        ex);

                        throw new RuntimeException("Error polling shipment");
                }
        }

        private void verificarActivo(String quotationId) {

                Optional<SkyDropXProcessRecord> record =
                                skyDropXProcessDAO.findByQuotationId(quotationId);

                if (record.isEmpty() || !Boolean.TRUE.equals(record.get().getIsActive())) {

                        log.info(
                                        "[SKYDROPX-SHIPMENT-POLLING] proceso supersedido, abortando polling quotationId={}",
                                        quotationId);

                        throw new SkyDropXProcessSupersededException(quotationId);
                }
        }
}