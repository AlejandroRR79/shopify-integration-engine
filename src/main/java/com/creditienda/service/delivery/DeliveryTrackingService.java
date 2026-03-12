package com.creditienda.service.delivery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.dto.delivery.B2BSeguimientoEntregaResponseDTO;
import com.creditienda.model.EstafetaResponse;
import com.creditienda.service.EstafetHistorialClient;
import com.creditienda.service.b2b.B2BDeliveryClient;
import com.creditienda.service.notificacion.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryTrackingService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingService.class);

    private final B2BDeliveryClient b2bClient;
    private final EstafetHistorialClient estafetaClient;
    private final ObjectMapper mapper = new ObjectMapper();

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

    DateTimeFormatter formatterEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter formatterSalida = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NotificacionService notificacionService;

    public DeliveryTrackingService(
            B2BDeliveryClient b2bClient,
            EstafetHistorialClient estafetaClient,
            NotificacionService notificacionService) {

        this.b2bClient = b2bClient;
        this.estafetaClient = estafetaClient;
        this.notificacionService = notificacionService;
    }

    public void sincronizarEstatusEntregas() {

        List<String> sinOC = new ArrayList<>();
        List<String> actualizadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        log.info("🚀 Inicia sincronización de entregas");

        try {

            List<B2BSeguimientoEntregaOrdenDTO> ordenes = new ArrayList<>();

            for (String estatus : estatusSeguimiento) {
                ordenes.addAll(consultarOrdenesPorEstatus(estatus, errores));

            }

            if (ordenes.isEmpty()) {
                log.warn("⚠ B2B no regresó órdenes para seguimientoEntrega");
                sinOC.add("No se encontraron OCs para los estatus configurados: " + estatusSeguimiento);
                enviarResumenCorreo(sinOC, actualizadas, errores);
                return;
            }

            log.info("Iniciando procesamiento de órdenes");

            for (B2BSeguimientoEntregaOrdenDTO orden : ordenes) {
                procesarOrden(orden, actualizadas, errores);
            }
            enviarResumenCorreo(sinOC, actualizadas, errores);

        } catch (Exception e) {
            log.error("❌ Error general en sincronización", e);
        }
    }

    private List<B2BSeguimientoEntregaOrdenDTO> consultarOrdenesPorEstatus(
            String cveEstatusOdc,
            List<String> errores) {

        try {
            log.info("🔎 Consultando OCs | cveEstatusOdc={}", cveEstatusOdc);

            String json = b2bClient.seguimientoEntrega(cveEstatusOdc);

            if (json == null || json.isBlank()) {
                log.error("❌ B2B no respondió (timeout o error) | estatus={}", cveEstatusOdc);

                errores.add(
                        "B2B SIN RESPUESTA | estatus=" + cveEstatusOdc +
                                " | posible timeout o error de red");

                return List.of();
            }

            B2BSeguimientoEntregaResponseDTO response = mapper.readValue(json, B2BSeguimientoEntregaResponseDTO.class);

            log.info("lo que recibi de b2b OC ---------------> \n{}",
                    (response.getData() != null ? response.getData().size() : 0));
            if (response.getData() == null) {
                return List.of();
            }

            return response.getData();

        } catch (Exception e) {
            log.error("❌ Error consultando OCs | cveEstatusOdc={}", cveEstatusOdc, e);

            errores.add(
                    "Error consultando B2B | estatus=" + cveEstatusOdc +
                            " | " + e.getMessage());

            return List.of(); // 🔥 no romper flujo
        }
    }

    private void procesarOrden(
            B2BSeguimientoEntregaOrdenDTO orden,
            List<String> actualizadas,
            List<String> errores) {

        log.debug("➡ Procesando orden={}", orden);
        B2BActualizarEstatusEntregaDTO update = new B2BActualizarEstatusEntregaDTO();

        try {
            // 3️⃣ Estafeta → historial (usar WAYBILL)
            String jsonEstafeta = estafetaClient.consultarHistorialNumReferencia(
                    orden.getWaybill());

            log.debug("📥 JSON Estafeta={}", jsonEstafeta);

            EstafetaResponse response = mapper.readValue(jsonEstafeta, EstafetaResponse.class);

            if (response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("⚠ Sin items Estafeta | guia={}", orden.getWaybill());
                errores.add(
                        "Guía sin registro Estafeta | OC=" + orden.getOrderNumber() +
                                " | fechaSolicitud="
                                + (orden.getFechaSolicitud() != null ? orden.getFechaSolicitud() : "") +
                                " | waybill=" + orden.getWaybill());
                return;
            }

            EstafetaResponse.Item item = response.getItems().get(0);

            // 🚫 NUEVA VALIDACIÓN: error operacional de Estafeta
            if (item.getError() != null) {
                log.debug(
                        "⚠ Estafeta sin información operacional | guia={} | errorCode={} | errorDesc={}",
                        orden.getWaybill(),
                        item.getError().getCode(),
                        item.getError().getDescription());
                errores.add(
                        "Error Estafeta | OC=" + orden.getOrderNumber() +
                                " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                " | waybill=" + orden.getWaybill() +
                                " | " + item.getError().getDescription());
                return; // 🔥 NO mandar a B2B
            }
            EstafetaResponse.Status status = item.getStatusCurrent();

            if (status == null) {
                log.warn("⚠ Sin statusCurrent | guia={}", orden.getWaybill());
                errores.add(
                        "Sin statusCurrent | OC=" + orden.getOrderNumber() +
                                " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                " | waybill=" + orden.getWaybill());
                return;
            }

            // 🛡️ IDEMPOTENCIA: si B2B ya tiene el mismo estatus, no actualizar
            if (orden.getIdEstatusDelivery() != null
                    && status.getCode().equals(String.valueOf(orden.getIdEstatusDelivery()))) {

                log.debug(
                        "⏭ Idempotencia | Orden={} | Estatus ya aplicado={} | No se envía a B2B",
                        orden.getOrderNumber(),
                        status.getCode());
                return;
            }

            String reasonCodeDescription = "";

            if (item.getStatus() != null && !item.getStatus().isEmpty()) {
                log.info("Entra a obtener reasonCodeDescription");
                // Último evento del historial
                EstafetaResponse.Status lastStatus = item.getStatus().get(item.getStatus().size() - 1);

                if (Boolean.TRUE.equals(lastStatus.getIsReasonCode())
                        && lastStatus.getReasonCodeDescription() != null
                        && !lastStatus.getReasonCodeDescription().isBlank()) {
                    log.info("Ultimo estatus {}", lastStatus.getCode());
                    reasonCodeDescription = lastStatus.getReasonCodeDescription();
                }
            }

            // 🔥 CAMBIOS CLAVE
            update.setReferenceNumber(orden.getReferenceNumber()); // referenceNumber
            if (item.getInformation() == null) {
                log.warn("⚠ Estafeta sin information | guia={}", orden.getWaybill());
                errores.add(
                        "Estafeta sin information | OC=" + orden.getOrderNumber() +
                                " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                " | waybill=" + orden.getWaybill());
                return;
            }

            // 🔎 LOG CLAVE
            log.info(
                    "🧾 Estafeta | waybill={} | trackingEstafeta={} | code={} | desc={} | fecha={} | reasonCodeDesc={}",
                    orden.getWaybill(),
                    item.getInformation().getTrackingCode(),
                    status.getCode(),
                    status.getSpanishName(),
                    status.getLocalDateTime(),
                    reasonCodeDescription);

            update.setTrackingCode(item.getInformation().getTrackingCode());
            update.setOrderNumber(orden.getOrderNumber()); // orderNumber

            update.setCodigoEntrega(status.getCode());
            // update.setCodigoEntrega("6");
            update.setDescripcionEntrega(status.getSpanishName());

            // fechaEstatus → desde Estafeta
            String fechaEstatus;
            String rawDate = status.getLocalDateTime();

            try {
                log.info("Fecha Estafeta RAW: {}", rawDate);
                if (rawDate != null && !rawDate.isBlank()) {

                    rawDate = rawDate.replace("T", " ");

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime fecha = LocalDateTime.parse(rawDate, formatter);

                    fechaEstatus = fecha.format(formatter);

                } else {

                    fechaEstatus = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    log.warn("⚠ localDateTime nulo | guia={} | usando fecha actual={}",
                            orden.getWaybill(),
                            fechaEstatus);
                }

            } catch (Exception e) {

                log.warn("⚠ Error parseando fecha Estafeta: {}", rawDate);

                fechaEstatus = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            update.setFechaEstatus(fechaEstatus);
            update.setReasonCodeDescription(reasonCodeDescription);

            log.info("➡ DTO B2B update={}", update);

            // 5️⃣ B2B → actualizar
            b2bClient.actualizarEstatusDelivery(update);

            actualizadas.add(
                    "OC=" + update.getOrderNumber() +
                            " | referenceNumber=" + update.getReferenceNumber() +
                            " | trackingCode=" + update.getTrackingCode() +
                            " | codigoEntrega=" + update.getCodigoEntrega() +
                            " | descripcionEntrega=" + update.getDescripcionEntrega() +
                            " | fechaEstatus=" + update.getFechaEstatus() +
                            " | reasonCodeDescription=" + update.getReasonCodeDescription());

        } catch (Exception e) {
            log.error("❌ Error procesando orden={}", orden.getOrderNumber(), e);
            errores.add(
                    "Excepción | OC=" + update.getOrderNumber() +
                            " | fechaSolicitud=" + update.getFechaEstatus() +
                            " | waybill=" + orden.getWaybill() +
                            " | estatus=" + orden.getIdEstatusDelivery() +
                            " | trackingCode=" + update.getTrackingCode() +
                            " | codigoEntrega=" + update.getCodigoEntrega() +
                            " | descripcionEntrega=" + update.getDescripcionEntrega() +
                            " | " + e.getMessage());
        }
    }

    private void enviarResumenCorreo(
            List<String> sinOC,
            List<String> actualizadas,
            List<String> errores) {

        StringBuilder sb = new StringBuilder();

        sb.append("📦 RESUMEN SINCRONIZACIÓN ESTAFETA → B2B\n\n");

        sb.append("🔗 URLs Consumidas:\n");
        sb.append("B2B Seguimiento: ")
                .append(b2bBaseUrl).append(b2bSeguimientoEndpoint).append("\n");

        sb.append("B2B Actualizar: ")
                .append(b2bBaseUrl).append(b2bActualizarEndpoint).append("\n");

        sb.append("Estafeta API: ")
                .append(estafetaApiUrl).append("\n\n");

        if (sinOC.isEmpty() && actualizadas.isEmpty() && errores.isEmpty()) {
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

        notificacionService.enviarResumen(sb.toString());
    }

}
