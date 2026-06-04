package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;
import com.creditienda.service.skydropx.constants.SkyDropXProcessStep;
import com.creditienda.service.skydropx.constants.SkyDropXProcessSupersededException;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio encargado de procesar
 * quotations en background.
 *
 * IMPORTANTE:
 * Este flujo reemplaza dependencia
 * principal del webhook.
 */
@Service
public class SkyDropXProcessOrchestratorService {

        private static final Logger log = LogManager.getLogger(
                        SkyDropXProcessOrchestratorService.class);

        private final SkyDropXRateSelectionService skyDropXRateSelectionService;

        private final SkyDropXShipmentService skyDropXShipmentService;

        private final SkyDropXProcessDAO skyDropXProcessDAO;

        private final ObjectMapper objectMapper;

        public SkyDropXProcessOrchestratorService(
                        SkyDropXRateSelectionService skyDropXRateSelectionService,
                        SkyDropXShipmentService skyDropXShipmentService,
                        SkyDropXProcessDAO skyDropXProcessDAO,
                        ObjectMapper objectMapper) {

                this.skyDropXRateSelectionService = skyDropXRateSelectionService;

                this.skyDropXShipmentService = skyDropXShipmentService;

                this.skyDropXProcessDAO = skyDropXProcessDAO;

                this.objectMapper = objectMapper;
        }

        /**
         * Procesar quotation async.
         *
         * Flujo:
         * 1. Polling GET quotation.
         * 2. Esperar is_completed=true.
         * 3. Filtrar tarifas válidas.
         * 4. Seleccionar mejor tarifa.
         * 5. Generar shipment.
         *
         * @param quotationId quotation generado
         * @param request     payload original Estafeta
         */
        @Async("skydropxExecutor")
        public void processQuotationAsync(
                        String quotationId,
                        WayBillRequestDTO request) {

                try {

                        log.info(
                                        "[SKYDROPX-ASYNC] iniciando processing quotationId={}",
                                        quotationId);

                        Rate selectedRate = skyDropXRateSelectionService
                                        .processQuotation(
                                                        quotationId);

                        /**
                         * Validar tarifa seleccionada.
                         */
                        if (selectedRate == null) {

                                log.error(
                                                "[SKYDROPX-ASYNC] no fue posible seleccionar tarifa quotationId={}",
                                                quotationId);

                                skyDropXProcessDAO.markFailed(
                                                quotationId,
                                                "NO_VALID_RATES");

                                return;
                        }

                        log.info(
                                        "[SKYDROPX-ASYNC] selectedRateId={}",
                                        selectedRate.getId());

                        String selectedRateJson = objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(selectedRate);

                        skyDropXProcessDAO.updateSelectedRate(
                                        quotationId,
                                        selectedRate.getId(),
                                        selectedRateJson);

                        log.info(
                                        "[SKYDROPX-PROCESS] RATE_SELECTED persistido quotationId={}, selectedRateId={}",
                                        quotationId,
                                        selectedRate.getId());
                        /**
                         * Generar shipment real.
                         */
                        skyDropXShipmentService
                                        .generateShipment(quotationId,
                                                        request,
                                                        selectedRate);
                        log.info(
                                        "[SKYDROPX-ASYNC] processing completado quotationId={}",
                                        quotationId);

                } catch (SkyDropXProcessSupersededException pse) {

                        log.info(
                                        "[SKYDROPX-ASYNC] proceso supersedido, abortando quotationId={}",
                                        quotationId);

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-ASYNC] error processing quotationId={}",
                                        quotationId,
                                        ex);

                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.length() > 500) {
                                errorMsg = errorMsg.substring(0, 500);
                        }

                        skyDropXProcessDAO.markFailed(
                                        quotationId,
                                        errorMsg);
                }
        }

        /**
         * Retoma desde SHIPMENT_COMPLETED.
         * El shipment ya existe en SkyDropX — solo persiste en BD.
         * No llama la API de SkyDropX para evitar crear un shipment duplicado.
         */
        @Async("skydropxExecutor")
        public void recoverFromShipmentCompleted(
                        SkyDropXProcessRecord record) {

                String quotationId = record.getQuotationId();

                try {

                        log.info(
                                        "[SKYDROPX-RECOVERY] retomando desde {} quotationId={}",
                                        SkyDropXProcessStep.SHIPMENT_COMPLETED,
                                        quotationId);

                        skyDropXProcessDAO.completeShipmentAndOrder(
                                        quotationId,
                                        record.getShipmentId(),
                                        record.getShipmentRawJson(),
                                        record.getTrackingNumber(),
                                        record.getLabelUrl());

                        log.info(
                                        "[SKYDROPX-RECOVERY] SHIPMENT_COMPLETED recuperado quotationId={}",
                                        quotationId);

                } catch (SkyDropXProcessSupersededException pse) {

                        log.info(
                                        "[SKYDROPX-RECOVERY] proceso supersedido, abortando quotationId={}",
                                        quotationId);

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-RECOVERY] error en recovery SHIPMENT_COMPLETED quotationId={}",
                                        quotationId,
                                        ex);

                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.length() > 500) {
                                errorMsg = errorMsg.substring(0, 500);
                        }

                        skyDropXProcessDAO.markFailed(quotationId, errorMsg);
                }
        }

        /**
         * Retoma el flujo desde RATE_SELECTED.
         * Se usa cuando el proceso murió después de seleccionar
         * la tarifa pero antes de crear el shipment.
         */
        @Async("skydropxExecutor")
        public void recoverFromRateSelected(
                        String quotationId,
                        WayBillRequestDTO request,
                        Rate selectedRate) {

                try {

                        log.info(
                                        "[SKYDROPX-RECOVERY] retomando desde {} quotationId={}",
                                        SkyDropXProcessStep.RATE_SELECTED,
                                        quotationId);

                        skyDropXShipmentService.generateShipment(
                                        quotationId,
                                        request,
                                        selectedRate);

                        log.info(
                                        "[SKYDROPX-RECOVERY] completado quotationId={}",
                                        quotationId);

                } catch (SkyDropXProcessSupersededException pse) {

                        log.info(
                                        "[SKYDROPX-RECOVERY] proceso supersedido, abortando quotationId={}",
                                        quotationId);

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-RECOVERY] error en recovery quotationId={}",
                                        quotationId,
                                        ex);

                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.length() > 500) {
                                errorMsg = errorMsg.substring(0, 500);
                        }

                        skyDropXProcessDAO.markFailed(quotationId, errorMsg);
                }
        }
}