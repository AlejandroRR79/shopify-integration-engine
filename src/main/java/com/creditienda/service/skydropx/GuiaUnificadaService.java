package com.creditienda.service.skydropx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.GuiaUnificadaResponseDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationProcessResponseDTO;
import com.creditienda.service.EstafetaGuiaClient;
import com.creditienda.service.delivery.dao.DeliveryDAO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GuiaUnificadaService {

    private static final Logger log = LogManager.getLogger(GuiaUnificadaService.class);

    private static final String CARRIER_ESTAFETA = "estafeta";
    private static final String CARRIER_SKYDROPX = "skydropx";

    @Value("${guia.prelacion}")
    private String prelacion;

    @Value("${estafeta.guia.effective-date.offset-days}")
    private int effectiveDateOffsetDays;

    private final EstafetaGuiaClient estafetaGuiaClient;
    private final SkyDropXQuotationService skyDropXQuotationService;
    private final DeliveryDAO deliveryDAO;
    private final ObjectMapper objectMapper;

    public GuiaUnificadaService(
            EstafetaGuiaClient estafetaGuiaClient,
            SkyDropXQuotationService skyDropXQuotationService,
            DeliveryDAO deliveryDAO,
            ObjectMapper objectMapper) {

        this.estafetaGuiaClient = estafetaGuiaClient;
        this.skyDropXQuotationService = skyDropXQuotationService;
        this.deliveryDAO = deliveryDAO;
        this.objectMapper = objectMapper;
    }

    public GuiaUnificadaResponseDTO generarGuia(WayBillRequestDTO request) {

        List<String> carriers = Arrays.stream(prelacion.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(c -> !c.isBlank())
                .toList();

        log.info("[GUIA-UNIFICADA] prelacion={}", carriers);

        Map<String, JsonNode> errores = new LinkedHashMap<>();

        for (String carrier : carriers) {

            if (CARRIER_ESTAFETA.equals(carrier)) {

                try {
                    GuiaUnificadaResponseDTO response = intentarEstafeta(request);
                    response.setErrores(errores.isEmpty() ? null : errores);
                    log.info("[GUIA-UNIFICADA] exito con Estafeta");
                    return response;

                } catch (Exception ex) {
                    JsonNode errorNode = parsearError(ex.getMessage());
                    errores.put("estafeta", errorNode);
                    log.warn("[GUIA-UNIFICADA] Estafeta fallo — error={}", ex.getMessage());
                }

            } else if (CARRIER_SKYDROPX.equals(carrier)) {

                try {
                    GuiaUnificadaResponseDTO response = intentarSkyDropX(request);
                    response.setErrores(errores.isEmpty() ? null : errores);
                    log.info("[GUIA-UNIFICADA] exito con SkyDropX");
                    return response;

                } catch (Exception ex) {
                    JsonNode errorNode = parsearError(ex.getMessage());
                    errores.put("skydropx", errorNode);
                    log.warn("[GUIA-UNIFICADA] SkyDropX fallo — error={}", ex.getMessage());
                }

            } else {
                log.warn("[GUIA-UNIFICADA] carrier desconocido en prelacion: {}", carrier);
            }
        }

        log.error("[GUIA-UNIFICADA] todos los carriers fallaron. prelacion={}", prelacion);
        GuiaUnificadaResponseDTO errorResponse = new GuiaUnificadaResponseDTO();
        errorResponse.setErrores(errores);
        throw new AllCarriersFailedException(errorResponse);
    }

    private GuiaUnificadaResponseDTO intentarEstafeta(WayBillRequestDTO request) {

        aplicarFechaEfectiva(request);

        String jsonRaw = estafetaGuiaClient.generarGuia(request);

        if (jsonRaw == null || jsonRaw.isBlank()) {
            throw new RuntimeException("Estafeta regresó respuesta vacía");
        }

        JsonNode estafetaNode = parsearJson(jsonRaw);

        JsonNode dataNode = estafetaNode.path("data");
        if (dataNode.isNull() || dataNode.isMissingNode()) {
            JsonNode resultNode = estafetaNode.path("labelPetitionResult").path("result");
            log.warn("[GUIA-UNIFICADA] Estafeta sin guia disponible code={} desc={}",
                    resultNode.path("code").asText(),
                    resultNode.path("description").asText());
            throw new RuntimeException(resultNode.toString());
        }

        actualizarOrdenEstafeta(request, dataNode);

        GuiaUnificadaResponseDTO response = new GuiaUnificadaResponseDTO();
        response.setEstafeta(estafetaNode);
        response.setSkydropx(null);
        return response;
    }

    private void actualizarOrdenEstafeta(WayBillRequestDTO request, JsonNode dataNode) {
        try {
            String rawRef = request.getLabelDefinition().getWayBillDocument().getReferenceNumber();
            String orderNumber = rawRef != null && rawRef.startsWith("ENL")
                    ? rawRef.substring(3)
                    : rawRef;

            String waybill = dataNode.path("WayBillDocument").path("WayBillNumber").asText(null);

            if (orderNumber == null || orderNumber.isBlank()) {
                log.warn("[GUIA-UNIFICADA] referenceNumber vacío, no se actualiza SHOPIFY_ORDER");
                return;
            }

            deliveryDAO.updateOrigenPaqueteriaEstafeta(orderNumber, waybill);

        } catch (Exception ex) {
            log.warn("[GUIA-UNIFICADA] no se pudo actualizar SHOPIFY_ORDER tras Estafeta: {}", ex.getMessage());
        }
    }

    private GuiaUnificadaResponseDTO intentarSkyDropX(WayBillRequestDTO request) {

        SkyDropXQuotationProcessResponseDTO skyResponse =
                skyDropXQuotationService.generateQuotation(request);

        if (skyResponse == null) {
            throw new RuntimeException("SkyDropX regresó respuesta vacía");
        }

        GuiaUnificadaResponseDTO response = new GuiaUnificadaResponseDTO();
        response.setEstafeta(null);
        response.setSkydropx(skyResponse);
        return response;
    }

    private void aplicarFechaEfectiva(WayBillRequestDTO request) {
        if (request == null
                || request.getLabelDefinition() == null
                || request.getLabelDefinition().getServiceConfiguration() == null) {
            return;
        }

        String effectiveDate = LocalDate.now()
                .plusDays(effectiveDateOffsetDays)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        request.getLabelDefinition()
                .getServiceConfiguration()
                .setEffectiveDate(effectiveDate);
    }

    public static class AllCarriersFailedException extends RuntimeException {
        private final GuiaUnificadaResponseDTO response;
        public AllCarriersFailedException(GuiaUnificadaResponseDTO response) {
            super("Todos los carriers fallaron");
            this.response = response;
        }
        public GuiaUnificadaResponseDTO getResponse() { return response; }
    }

    private JsonNode parsearJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.warn("[GUIA-UNIFICADA] no se pudo parsear JSON de Estafeta");
            return objectMapper.getNodeFactory().textNode(json);
        }
    }

    private JsonNode parsearError(String message) {
        try {
            if (message != null && message.trim().startsWith("{")) {
                return objectMapper.readTree(message);
            }
        } catch (Exception ignored) {
        }
        return objectMapper.getNodeFactory()
                .textNode(message != null ? message : "Error desconocido");
    }
}
