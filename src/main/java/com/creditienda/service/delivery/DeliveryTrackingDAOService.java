package com.creditienda.service.delivery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.model.EstafetaResponse;
import com.creditienda.service.EstafetHistorialClient;
import com.creditienda.service.delivery.core.DeliveryCoreService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryTrackingDAOService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingDAOService.class);

    private final DeliveryCoreService coreService;
    private final EstafetHistorialClient estafetaClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${b2b.seguimiento.estatus.odc}")
    private String estatusOdc;

    @Value("${b2b.seguimiento.estatus.delivery:}")
    private String estatusDelivery;

    public DeliveryTrackingDAOService(
            DeliveryCoreService coreService,
            EstafetHistorialClient estafetaClient) {

        this.coreService = coreService;
        this.estafetaClient = estafetaClient;

    }

    public void sincronizarEstatusEntregas() {

        List<String> errores = new ArrayList<>();

        log.info("🚀 [DAO] Inicia sincronización");

        try {

            List<Integer> estatusOdcList = parseToIntegerList(estatusOdc);
            List<Integer> estatusDeliveryList = parseToIntegerList(estatusDelivery);

            List<B2BSeguimientoEntregaOrdenDTO> ordenes = coreService.consultarOrdenesPorEstatus(estatusOdcList,
                    estatusDeliveryList);

            if (ordenes == null || ordenes.isEmpty()) {
                log.warn("[DAO] ⚠ No hay órdenes");
                return;
            }

            for (B2BSeguimientoEntregaOrdenDTO orden : ordenes) {
                procesarOrden(orden, errores);
            }

        } catch (Exception e) {
            log.error("❌ [DAO] Error general", e);
        }
    }

    private void procesarOrden(
            B2BSeguimientoEntregaOrdenDTO orden,
            List<String> errores) {

        B2BActualizarEstatusEntregaDTO update = new B2BActualizarEstatusEntregaDTO();

        try {

            String jsonEstafeta = estafetaClient.consultarHistorialNumReferencia(
                    orden.getWaybill());

            EstafetaResponse response = mapper.readValue(jsonEstafeta, EstafetaResponse.class);

            if (response.getItems() == null || response.getItems().isEmpty()) {
                errores.add("Sin items Estafeta OC=" + orden.getOrderNumber());
                return;
            }

            EstafetaResponse.Item item = response.getItems().get(0);

            // 🔴 ERROR ESTAFETA
            if (item.getError() != null) {
                errores.add("Error Estafeta OC=" + orden.getOrderNumber());
                return;
            }

            EstafetaResponse.Status status = item.getStatusCurrent();

            if (status == null) {
                errores.add("Sin status OC=" + orden.getOrderNumber());
                return;
            }

            // 🔥 IDEMPOTENCIA
            if (orden.getIdEstatusDelivery() != null
                    && status.getCode() != null
                    && status.getCode().equals(String.valueOf(orden.getIdEstatusDelivery()))) {
                return;
            }

            String reasonCodeDescription = "";

            if (item.getStatus() != null && !item.getStatus().isEmpty()) {
                EstafetaResponse.Status last = item.getStatus().get(item.getStatus().size() - 1);

                if (Boolean.TRUE.equals(last.getIsReasonCode())) {
                    reasonCodeDescription = last.getReasonCodeDescription();
                }
            }

            // 🔥 ARMAR DTO
            update.setOrderNumber(orden.getOrderNumber());
            update.setReferenceNumber(orden.getReferenceNumber());
            update.setTrackingCode(item.getInformation().getTrackingCode());
            update.setCodigoEntrega(status.getCode());
            update.setDescripcionEntrega(status.getSpanishName());

            // 🔥 FECHA
            String rawDate = status.getLocalDateTime();
            String fechaEstatus;

            try {
                rawDate = rawDate.replace("T", " ");
                LocalDateTime fecha = LocalDateTime.parse(rawDate,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                fechaEstatus = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            } catch (Exception e) {
                fechaEstatus = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            update.setFechaEstatus(fechaEstatus);
            update.setReasonCodeDescription(reasonCodeDescription);

            // 🔥 AQUÍ ESTÁ EL CAMBIO IMPORTANTE
            coreService.updateEstatusDelivery(update);

        } catch (Exception e) {

            log.error("❌ Error orden={}", orden.getOrderNumber(), e);

            errores.add(
                    "Error OC=" + orden.getOrderNumber() +
                            " | waybill=" + orden.getWaybill() +
                            " | " + e.getMessage());
        }
    }

    private List<Integer> parseToIntegerList(String value) {

        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
    }
}