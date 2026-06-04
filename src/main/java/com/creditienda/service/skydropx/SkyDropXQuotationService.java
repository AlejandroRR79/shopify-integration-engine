package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationProcessResponseDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationRequestDTO;
import com.creditienda.service.notificacion.NotificacionService;
import com.creditienda.service.skydropx.constants.SkyDropXProcessStatus;
import com.creditienda.service.skydropx.constants.SkyDropXProcessStep;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.mapper.SkyDropXQuotationMapper;
import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio encargado de generar quotations
 * en SkyDropX.
 *
 * Flujo:
 * 1. Obtener token OAuth.
 * 2. Mapear payload Estafeta.
 * 3. Invocar POST quotations.
 * 4. Registrar quotationId.
 */
@Service
public class SkyDropXQuotationService {

        private static final Logger log = LogManager.getLogger(
                        SkyDropXQuotationService.class);

        @Value("${skydropx.base-url}")
        private String baseUrl;

        private final RestTemplate restTemplate;

        private final SkyDropXTokenService skyDropXTokenService;

        private final SkyDropXQuotationMapper skyDropXQuotationMapper;

        private final NotificacionService notificacionService;

        private final SkyDropXProcessOrchestratorService skyDropXProcessOrchestratorService;

        private final SkyDropXProcessDAO skyDropXProcessDAO;

        private final ObjectMapper objectMapper;

        public SkyDropXQuotationService(
                        RestTemplate restTemplate,
                        SkyDropXTokenService skyDropXTokenService,
                        SkyDropXQuotationMapper skyDropXQuotationMapper,
                        NotificacionService notificacionService,
                        SkyDropXProcessOrchestratorService skyDropXProcessOrchestratorService,
                        SkyDropXProcessDAO skyDropXProcessDAO,
                        ObjectMapper objectMapper) {

                this.restTemplate = restTemplate;
                this.skyDropXTokenService = skyDropXTokenService;
                this.skyDropXQuotationMapper = skyDropXQuotationMapper;
                this.notificacionService = notificacionService;
                this.skyDropXProcessOrchestratorService = skyDropXProcessOrchestratorService;
                this.skyDropXProcessDAO = skyDropXProcessDAO;
                this.objectMapper = objectMapper;
        }

        /**
         * Genera quotation en SkyDropX.
         *
         * Flujo:
         * 1. Obtener token OAuth.
         * 2. Transformar payload Estafeta.
         * 3. Invocar POST quotations.
         * 4. Obtener quotationId.
         * 5. Regresar PROCESSING inmediatamente.
         *
         * NOTA:
         * Todavía NO se realiza polling.
         * Todavía NO se usa async.
         * Todavía NO se persiste en BD.
         *
         * @param request payload original Estafeta
         * @return response con quotationId y status PROCESSING
         */
        public SkyDropXQuotationProcessResponseDTO generateQuotation(
                        WayBillRequestDTO request) {

                try {

                        /**
                         * Obtener token OAuth.
                         */
                        String token = skyDropXTokenService.getAccessToken();

                        /**
                         * Transformar payload Estafeta
                         * a quotation SkyDropX.
                         */
                        SkyDropXQuotationRequestDTO quotationRequest = skyDropXQuotationMapper.map(request);

                        HttpHeaders headers = new HttpHeaders();

                        headers.setContentType(
                                        MediaType.APPLICATION_JSON);

                        headers.setBearerAuth(token);

                        HttpEntity<SkyDropXQuotationRequestDTO> entity = new HttpEntity<>(
                                        quotationRequest,
                                        headers);

                        /**
                         * Serializar payload para logging.
                         */
                        String quotationRequestJson = objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(
                                                        quotationRequest);

                        String requestJson = objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(
                                                        request);
                        /**
                         * Log payload enviado.
                         */
                        log.info(
                                        "[SKYDROPX-QUOTATION] request=\n{}",
                                        quotationRequestJson);

                        String url = baseUrl + "/api/v1/quotations";

                        /**
                         * Invocar quotations SkyDropX.
                         */
                        log.info(
                                        "[SKYDROPX-QUOTATION] invocando url={}",
                                        url);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        url,
                                        HttpMethod.POST,
                                        entity,
                                        String.class);

                        /**
                         * Confirmar response recibido.
                         */
                        log.info(
                                        "[SKYDROPX-QUOTATION] response recibido");

                        String rawBody = response.getBody();

                        /**
                         * Parsear response JSON.
                         */
                        JsonNode root = objectMapper.readTree(rawBody);

                        /**
                         * Obtener quotationId.
                         */
                        String quotationId = root.path("id")
                                        .asText();

                        /**
                         * Validar quotationId.
                         */
                        if (quotationId == null
                                        || quotationId.isBlank()) {

                                log.warn(
                                                "[SKYDROPX-QUOTATION] quotationId vacío");

                                throw new RuntimeException(
                                                "SkyDropX quotationId vacío");
                        }

                        /**
                         * Log quotation generado.
                         */
                        log.info(
                                        "[SKYDROPX-QUOTATION] quotationId={}",
                                        quotationId);

                        String orderNumber = request.getLabelDefinition().getWayBillDocument()
                                        .getReferenceNumber();

                        if (orderNumber != null
                                        && orderNumber.startsWith("ENL")) {

                                orderNumber = orderNumber.substring(3);
                        }

                        SkyDropXProcessRecord processRecord = new SkyDropXProcessRecord();

                        processRecord.setOrderNumber(orderNumber);

                        processRecord.setQuotationId(quotationId);

                        processRecord.setRequestJson(requestJson);

                        processRecord.setStatusId(
                                        SkyDropXProcessStatus.PROCESSING);

                        processRecord.setProcessStep(
                                        SkyDropXProcessStep.QUOTATION_REQUESTED);

                        processRecord.setRetryCount(0);

                        processRecord.setIsRetryable(Boolean.TRUE);

                        processRecord.setIsActive(Boolean.TRUE);
                        processRecord.setHasFinalShipment(
                                        Boolean.FALSE);

                        skyDropXProcessDAO
                                        .supersedePreviousAndCreate(
                                                        orderNumber,
                                                        processRecord);

                        log.info(
                                        "[SKYDROPX-PROCESS] process persistido quotationId={}, orderNumber={}",
                                        quotationId,
                                        orderNumber);

                        /**
                         * Lanzar procesamiento async.
                         *
                         * IMPORTANTE:
                         * El request HTTP NO esperará
                         * completion de quotation.
                         */
                        skyDropXProcessOrchestratorService
                                        .processQuotationAsync(
                                                        quotationId,
                                                        request);
                        /**
                         * Regresar respuesta inmediata.
                         *
                         * IMPORTANTE:
                         * Todavía NO se espera completion.
                         * Todavía NO se realiza polling.
                         */
                        SkyDropXQuotationProcessResponseDTO processResponse = new SkyDropXQuotationProcessResponseDTO();

                        processResponse.setQuotationId(
                                        quotationId);

                        processResponse.setStatus(
                                        "PROCESSING");

                        return processResponse;

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-QUOTATION] error generando quotation",
                                        ex);

                        notificacionService.enviarError(
                                        "Error generando quotation SkyDropX");

                        throw new RuntimeException(
                                        "Error generando quotation SkyDropX");
                }
        }
}