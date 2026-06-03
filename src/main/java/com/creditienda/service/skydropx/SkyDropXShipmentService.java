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
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;
import com.creditienda.dto.skydropx.SkyDropXShipmentRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXShipmentResponseDTO;
import com.creditienda.service.notificacion.NotificacionService;
import com.creditienda.service.skydropx.constants.SkyDropXProcessSupersededException;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.mapper.SkyDropXShipmentMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servicio encargado de generar
 * shipments en SkyDropX.
 *
 * Shipment = envío real.
 *
 * Aquí se generan:
 * - guía
 * - tracking
 * - label PDF
 */
@Service
public class SkyDropXShipmentService {

        private static final Logger log = LogManager.getLogger(
                        SkyDropXShipmentService.class);

        @Value("${skydropx.base-url}")
        private String baseUrl;

        private final RestTemplate restTemplate;

        private final SkyDropXTokenService skyDropXTokenService;

        private final SkyDropXShipmentMapper skyDropXShipmentMapper;

        private final SkyDropXShipmentClientService skyDropXShipmentClientService;

        private final SkyDropXShipmentPollingService skyDropXShipmentPollingService;

        private final SkyDropXLabelDownloadService skyDropXLabelDownloadService;

        private final NotificacionService notificacionService;
        private final SkyDropXProcessDAO skyDropXProcessDAO;

        public SkyDropXShipmentService(
                        RestTemplate restTemplate,
                        SkyDropXTokenService skyDropXTokenService,
                        SkyDropXShipmentMapper skyDropXShipmentMapper,
                        SkyDropXShipmentClientService skyDropXShipmentClientService,
                        SkyDropXShipmentPollingService skyDropXShipmentPollingService,
                        SkyDropXLabelDownloadService skyDropXLabelDownloadService,
                        NotificacionService notificacionService,
                        SkyDropXProcessDAO skyDropXProcessDAO) {

                this.restTemplate = restTemplate;

                this.skyDropXTokenService = skyDropXTokenService;

                this.skyDropXShipmentMapper = skyDropXShipmentMapper;

                this.skyDropXShipmentClientService = skyDropXShipmentClientService;

                this.skyDropXShipmentPollingService = skyDropXShipmentPollingService;

                this.skyDropXLabelDownloadService = skyDropXLabelDownloadService;

                this.notificacionService = notificacionService;
                this.skyDropXProcessDAO = skyDropXProcessDAO;
        }

        /**
         * Generar shipment real.
         *
         * @param quotationId  ID de cotización en SkyDropX.
         * @param request      payload original
         * @param selectedRate mejor tarifa
         */
        public void generateShipment(
                        String quotationId,
                        WayBillRequestDTO request,
                        Rate selectedRate) {

                try {

                        /**
                         * Obtener token OAuth.
                         */
                        String token = skyDropXTokenService
                                        .getAccessToken();

                        /**
                         * Construir shipment payload.
                         */
                        SkyDropXShipmentRequestDTO shipmentRequest = skyDropXShipmentMapper.map(
                                        request,
                                        selectedRate.getId());

                        HttpHeaders headers = new HttpHeaders();

                        headers.setContentType(
                                        MediaType.APPLICATION_JSON);

                        headers.setBearerAuth(
                                        token);

                        HttpEntity<SkyDropXShipmentRequestDTO> entity = new HttpEntity<>(
                                        shipmentRequest,
                                        headers);

                        ObjectMapper mapper = new ObjectMapper();

                        /**
                         * Debug payload shipment.
                         */
                        String requestJson = mapper.writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(
                                                        shipmentRequest);

                        log.info(
                                        "[SKYDROPX-SHIPMENT] request=\n{}",
                                        requestJson);

                        String url = baseUrl
                                        + "/api/v1/shipments";

                        log.info(
                                        "[SKYDROPX-SHIPMENT] invocando url={}",
                                        url);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        url,
                                        HttpMethod.POST,
                                        entity,
                                        String.class);

                        /**
                         * Debug response shipment.
                         */
                        log.info(
                                        "[SKYDROPX-SHIPMENT] response=\n{}",
                                        response.getBody());

                        /**
                         * =====================================
                         * OBTENER SHIPMENT ID
                         * =====================================
                         */
                        JsonNode root = mapper.readTree(
                                        response.getBody());

                        String shipmentId = root.path("data")
                                        .path("id")
                                        .asText();

                        log.info(
                                        "[SKYDROPX-SHIPMENT] shipmentId={}",
                                        shipmentId);

                        /**
                         * =====================================
                         * ESPERAR SHIPMENT LISTO
                         * =====================================
                         */
                        SkyDropXShipmentResponseDTO shipmentResponse = skyDropXShipmentPollingService
                                        .waitForShipment(
                                                        shipmentId,
                                                        quotationId);

                        /**
                         * =====================================
                         * TRACKING
                         * =====================================
                         */
                        log.info(
                                        "[SKYDROPX-SHIPMENT] trackingNumber={}",
                                        shipmentResponse
                                                        .getTrackingNumber());

                        log.info(
                                        "[SKYDROPX-SHIPMENT] trackingUrl={}",
                                        shipmentResponse
                                                        .getTrackingUrlProvider());

                        /**
                         * =====================================
                         * LABEL URL
                         * =====================================
                         */
                        String labelUrl = shipmentResponse
                                        .getLabelUrl();

                        log.info(
                                        "[SKYDROPX-SHIPMENT] labelUrl={}",
                                        labelUrl);

                        String shipmentRawJson = mapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(
                                                        shipmentResponse);

                        skyDropXProcessDAO.updateShipment(
                                        quotationId,
                                        shipmentId,
                                        shipmentRawJson,
                                        shipmentResponse.getTrackingNumber(),
                                        labelUrl);

                        log.info(
                                        "[SKYDROPX-PROCESS] SHIPMENT_COMPLETED persistido shipmentId={}, trackingNumber={}",
                                        shipmentId,
                                        shipmentResponse.getTrackingNumber());

                        /**
                         * =====================================
                         * DESCARGAR PDF
                         * =====================================
                         */
                        if (labelUrl != null
                                        && !labelUrl.isBlank()) {

                                byte[] pdfBytes = skyDropXLabelDownloadService
                                                .downloadLabel(
                                                                labelUrl);

                                log.info(
                                                "[SKYDROPX-SHIPMENT] label bytes={}",
                                                pdfBytes.length);

                                /**
                                 * TODO:
                                 * subir a S3.
                                 */

                        } else {

                                log.warn(
                                                "[SKYDROPX-SHIPMENT] labelUrl vacío");
                        }

                        skyDropXProcessDAO.markCompleted(quotationId);

                        log.info(
                                        "[SKYDROPX-PROCESS] COMPLETED quotationId={}",
                                        quotationId);

                } catch (SkyDropXProcessSupersededException pse) {

                        throw pse;

                } catch (Exception ex) {

                        log.error(
                                        "[SKYDROPX-SHIPMENT] error generando shipment",
                                        ex);

                        notificacionService.enviarError(
                                        "Error generando shipment SkyDropX");

                        throw new RuntimeException(
                                        "Error generando shipment SkyDropX");
                }
        }
}