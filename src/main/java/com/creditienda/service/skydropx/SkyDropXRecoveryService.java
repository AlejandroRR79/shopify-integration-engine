package com.creditienda.service.skydropx;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;
import com.creditienda.service.skydropx.constants.SkyDropXProcessStep;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SkyDropXRecoveryService {

    private static final Logger log = LogManager.getLogger(SkyDropXRecoveryService.class);

    @Value("${skydropx.recovery.stuck-minutes}")
    private int stuckMinutes;

    @Value("${skydropx.recovery.max-retries}")
    private int maxRetries;

    private final SkyDropXProcessDAO skyDropXProcessDAO;
    private final SkyDropXProcessOrchestratorService orchestratorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SkyDropXRecoveryService(
            SkyDropXProcessDAO skyDropXProcessDAO,
            SkyDropXProcessOrchestratorService orchestratorService) {

        this.skyDropXProcessDAO = skyDropXProcessDAO;
        this.orchestratorService = orchestratorService;
    }

    public void ejecutarRecovery() {

        log.info("[SKYDROPX-RECOVERY] iniciando ciclo de recovery stuckMinutes={} maxRetries={}",
                stuckMinutes, maxRetries);

        List<SkyDropXProcessRecord> stuckRecords =
                skyDropXProcessDAO.findStuckProcesses(stuckMinutes, maxRetries);

        if (stuckRecords.isEmpty()) {
            log.info("[SKYDROPX-RECOVERY] no hay procesos atorados");
            return;
        }

        log.info("[SKYDROPX-RECOVERY] procesos a recuperar={}", stuckRecords.size());

        for (SkyDropXProcessRecord record : stuckRecords) {
            try {
                recuperar(record);
            } catch (Exception ex) {
                log.error("[SKYDROPX-RECOVERY] error recuperando quotationId={} — se omite",
                        record.getQuotationId(), ex);
            }
        }

        log.info("[SKYDROPX-RECOVERY] ciclo finalizado");
    }

    private void recuperar(SkyDropXProcessRecord record) {

        String quotationId = record.getQuotationId();
        String processStep = record.getProcessStep();

        log.info("[SKYDROPX-RECOVERY] recuperando quotationId={} step={} retryCount={}",
                quotationId, processStep, record.getRetryCount());

        WayBillRequestDTO request = deserializarRequest(record.getRequestJson(), quotationId);
        if (request == null) {
            log.error("[SKYDROPX-RECOVERY] requestJson inválido — marcando FAILED quotationId={}", quotationId);
            skyDropXProcessDAO.markFailed(quotationId, "requestJson no deserializable en recovery");
            return;
        }

        skyDropXProcessDAO.incrementRetryCount(quotationId);

        if (SkyDropXProcessStep.RATE_SELECTED.equals(processStep)) {

            Rate selectedRate = deserializarRate(record.getSelectedRateJson(), quotationId);

            if (selectedRate == null) {
                log.warn("[SKYDROPX-RECOVERY] selectedRateJson inválido, reiniciando desde quotation quotationId={}",
                        quotationId);
                orchestratorService.processQuotationAsync(quotationId, request);
                return;
            }

            log.info("[SKYDROPX-RECOVERY] lanzando recovery RATE_SELECTED → generateShipment quotationId={}",
                    quotationId);
            orchestratorService.recoverFromRateSelected(quotationId, request, selectedRate);

        } else {

            log.info("[SKYDROPX-RECOVERY] lanzando recovery {} → processQuotationAsync quotationId={}",
                    processStep, quotationId);
            orchestratorService.processQuotationAsync(quotationId, request);
        }
    }

    private WayBillRequestDTO deserializarRequest(String requestJson, String quotationId) {
        try {
            if (requestJson == null || requestJson.isBlank()) return null;
            return objectMapper.readValue(requestJson, WayBillRequestDTO.class);
        } catch (Exception ex) {
            log.error("[SKYDROPX-RECOVERY] no se pudo deserializar requestJson quotationId={}", quotationId, ex);
            return null;
        }
    }

    private Rate deserializarRate(String selectedRateJson, String quotationId) {
        try {
            if (selectedRateJson == null || selectedRateJson.isBlank()) return null;
            return objectMapper.readValue(selectedRateJson, Rate.class);
        } catch (Exception ex) {
            log.error("[SKYDROPX-RECOVERY] no se pudo deserializar selectedRateJson quotationId={}", quotationId, ex);
            return null;
        }
    }
}
