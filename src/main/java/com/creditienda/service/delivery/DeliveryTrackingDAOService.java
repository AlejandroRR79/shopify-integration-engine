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
import com.creditienda.service.notificacion.NotificacionService;
import com.creditienda.util.constantes.EstatusCve;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryTrackingDAOService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingDAOService.class);

    private final DeliveryCoreService coreService;
    private final EstafetHistorialClient estafetaClient;
    private final NotificacionService notificacionService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${b2b.seguimiento.estatus.delivery:}")
    private String estatusDelivery;

    @Value("${b2b.seguimiento.estatus}")
    private List<String> estatusSeguimiento;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${b2b.delivery.base.url}")
    private String b2bBaseUrl;

    @Value("${b2b.delivery.endpoint.seguimiento}")
    private String b2bSeguimientoEndpoint;

    @Value("${b2b.delivery.endpoint.actualizar}")
    private String b2bActualizarEndpoint;

    @Value("${estafeta.api.url}")
    private String estafetaApiUrl;

    public DeliveryTrackingDAOService(
            DeliveryCoreService coreService,
            EstafetHistorialClient estafetaClient,
            NotificacionService notificacionService) {

        this.coreService = coreService;
        this.estafetaClient = estafetaClient;
        this.notificacionService = notificacionService;
    }

    public void sincronizarEstatusEntregas() {

        List<String> sinOC = new ArrayList<>();
        List<String> actualizadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<String> otrasPaqueterias = new ArrayList<>();
        log.info("🚀 [DAO] Inicia sincronización");

        try {

            List<String> estatusOdcCveList = estatusSeguimiento;
            List<Integer> estatusDeliveryList = parseToIntegerList(estatusDelivery);

            List<B2BSeguimientoEntregaOrdenDTO> ordenes = coreService.consultarOrdenesPorEstatus(estatusOdcCveList,
                    estatusDeliveryList);

            if (ordenes == null || ordenes.isEmpty()) {
                log.warn("⚠ [DAO] No hay órdenes");
                sinOC.add("No se encontraron órdenes para los estatus configurados");
                enviarResumenCorreo(sinOC, actualizadas, errores, otrasPaqueterias);
                return;
            }

            for (B2BSeguimientoEntregaOrdenDTO orden : ordenes) {

                String paqueteria = orden.getPaqueteria();

                if (paqueteria != null
                        && !paqueteria.trim().isEmpty()
                        && !paqueteria.equalsIgnoreCase("ESTAFETA")) {

                    otrasPaqueterias.add(
                            "OC=" + orden.getOrderNumber() +
                                    " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                    " | waybill=" + orden.getWaybill() +
                                    " | paqueteria=" + paqueteria);

                    continue; // 🔥 NO entra a procesarOrden
                }

                procesarOrden(orden, actualizadas, errores);
            }

            enviarResumenCorreo(sinOC, actualizadas, errores, otrasPaqueterias);

        } catch (Exception e) {
            log.error("❌ [DAO] Error general", e);
            throw new RuntimeException("Error en sincronización: " + e.getMessage(), e);
        }
    }

    private void procesarOrden(
            B2BSeguimientoEntregaOrdenDTO orden,
            List<String> actualizadas,
            List<String> errores) {

        B2BActualizarEstatusEntregaDTO update = new B2BActualizarEstatusEntregaDTO();

        try {

            String guia = (EstatusCve.DEVOLUCION.equals(orden.getCveEstatusOdc())
                    && orden.getWaybillDevolution() != null)
                            ? orden.getWaybillDevolution()
                            : orden.getWaybill();

            String jsonEstafeta = estafetaClient.consultarHistorialNumReferencia(guia);

            EstafetaResponse response = mapper.readValue(jsonEstafeta, EstafetaResponse.class);

            if (response.getItems() == null || response.getItems().isEmpty()) {
                errores.add("Sin items Estafeta OC=" + orden.getOrderNumber());
                return;
            }

            EstafetaResponse.Item item = response.getItems().get(0);

            if (item.getError() != null) {

                String errorMsg = "Error desconocido";

                try {
                    if (item.getError().getDescription() != null) {
                        errorMsg = item.getError().getDescription();
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo leer detalle de error Estafeta");
                }

                errores.add(
                        "Error Estafeta | OC=" + orden.getOrderNumber() +
                                " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                " | waybill=" + guia +
                                " | " + errorMsg);

                return;
            }

            EstafetaResponse.Status status = item.getStatusCurrent();

            if (response.getItems() == null || response.getItems().isEmpty()) {

                errores.add(
                        "Sin items Estafeta OC=" + orden.getOrderNumber() +
                                " | waybill=" + guia +
                                " | response vacío");

                return;
            }

            String codigoOriginal = status.getCode();

            Integer codigoEstafeta = Integer.valueOf(codigoOriginal);

            // 🔥 NUEVO (pero SIN cambiar nombres tuyos)
            String cveDelivery = EstatusCve.getCveDelivery(codigoEstafeta);

            // 🔥 aquí SOLO corregimos: ya no existe codigoDelivery
            if (cveDelivery == null) {
                errores.add("Código inválido Estafeta OC=" + orden.getOrderNumber());
                return;
            }

            // 🛡️ Idempotencia (usando cve, no id)
            if (orden.getCveEstatusDelivery() != null
                    && orden.getCveEstatusDelivery().equals(cveDelivery)) {
                return;
            }

            String reasonCodeDescription = "";

            if (item.getStatus() != null && !item.getStatus().isEmpty()) {
                EstafetaResponse.Status last = item.getStatus().get(item.getStatus().size() - 1);

                if (Boolean.TRUE.equals(last.getIsReasonCode())) {
                    reasonCodeDescription = last.getReasonCodeDescription();
                }
            }

            update.setIdShopifyOrder(orden.getIdShopifyOrder());
            // 🔥 GUARDAR ORIGINAL (para Core)
            update.setCodigoEstafetaOriginal(codigoOriginal);

            // 🔥 MAPEO PARA CATÁLOGO
            update.setCveEstatusDelivery(cveDelivery);

            // 🔥 DEVOLUCIÓN
            boolean esDevolucion = item.getService() != null &&
                    item.getService().getDevolutionWayBill() != null &&
                    !item.getService().getDevolutionWayBill().isBlank();

            if (esDevolucion) {
                update.setWaybillDevolution(
                        item.getService().getDevolutionWayBill());

                // 🔥 ESTE TE FALTA
                if (item.getInformation() != null) {
                    update.setTrackingCodeDevolution(
                            item.getInformation().getTrackingCode());
                }

            }

            update.setOrderNumber(orden.getOrderNumber());
            update.setReferenceNumber(orden.getReferenceNumber());
            if (item.getInformation() != null) {
                update.setTrackingCode(item.getInformation().getTrackingCode());
            }
            // update.setCodigoEntrega(status.getCode());
            update.setDescripcionEntrega(status.getSpanishName());

            // 🕒 Fecha
            String fechaEstatus;
            String rawDate = status.getLocalDateTime();

            try {
                rawDate = rawDate.replace("T", " ");
                LocalDateTime fecha = LocalDateTime.parse(
                        rawDate,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                fechaEstatus = fecha.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            } catch (Exception e) {
                fechaEstatus = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            update.setFechaEstatus(fechaEstatus);
            update.setReasonCodeDescription(reasonCodeDescription);

            log.info("➡ Actualizando DB: {}", update);

            // 🔥 UPDATE REAL
            coreService.updateEstatusDelivery(
                    update,
                    orden.getCveEstatusOdc(), // ✅ ESTE ES EL CAMBIO IMPORTANTE
                    esDevolucion);

            actualizadas.add(
                    "OC=" + update.getOrderNumber() +
                            " | tracking=" + update.getTrackingCode() +
                            " | cve=" + update.getCveEstatusDelivery());

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

    private void enviarResumenCorreo(
            List<String> sinOC,
            List<String> actualizadas,
            List<String> errores,
            List<String> otrasPaqueterias) {

        StringBuilder sb = new StringBuilder();

        sb.append("📦 RESUMEN SINCRONIZACIÓN ESTAFETA → DB\n\n");

        sb.append("🌎 AMBIENTE: ").append(activeProfile).append("\n\n");

        sb.append("🔗 URLs Consumidas:\n");
        sb.append("B2B Seguimiento: ")
                .append(b2bBaseUrl).append(b2bSeguimientoEndpoint).append("\n");

        sb.append("B2B Actualizar: ")
                .append(b2bBaseUrl).append(b2bActualizarEndpoint).append("\n");

        sb.append("Estafeta API: ")
                .append(estafetaApiUrl).append("\n\n");

        if (sinOC.isEmpty() && actualizadas.isEmpty() && errores.isEmpty() && otrasPaqueterias.isEmpty()) {
            log.info("ℹ No hubo cambios ni errores, no se envía correo");
            return;
        }

        if (!sinOC.isEmpty()) {
            sb.append("❌ SIN OC:\n");
            sinOC.forEach(o -> sb.append(" - ").append(o).append("\n"));
            sb.append("\n");
        }

        if (!actualizadas.isEmpty()) {
            sb.append("✅ ACTUALIZADAS:\n");
            actualizadas.forEach(o -> sb.append(" - ").append(o).append("\n"));
            sb.append("\n");
        }

        if (!errores.isEmpty()) {
            sb.append("⚠ ERRORES:\n");
            errores.forEach(o -> sb.append(" - ").append(o).append("\n"));
        }

        if (!otrasPaqueterias.isEmpty()) {
            sb.append("📦 OTRAS PAQUETERÍAS (NO ESTAFETA):\n");
            otrasPaqueterias.forEach(o -> sb.append(" - ").append(o).append("\n"));
            sb.append("\n");
        }

        notificacionService.enviarResumen(sb.toString());
    }

}