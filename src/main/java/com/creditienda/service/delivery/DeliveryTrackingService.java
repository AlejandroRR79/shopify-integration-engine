package com.creditienda.service.delivery;

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
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryTrackingService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingService.class);

    private final B2BDeliveryClient b2bClient;
    private final EstafetHistorialClient estafetaClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${b2b.seguimiento.estatus}")
    private List<String> estatusSeguimiento;

    public DeliveryTrackingService(
            B2BDeliveryClient b2bClient,
            EstafetHistorialClient estafetaClient) {

        this.b2bClient = b2bClient;
        this.estafetaClient = estafetaClient;
    }

    public void sincronizarEstatusEntregas() {

        log.info("🚀 Inicia sincronización de entregas");

        try {

            List<B2BSeguimientoEntregaOrdenDTO> ordenes = new ArrayList<>();

            for (String estatus : estatusSeguimiento) {
                ordenes.addAll(
                        consultarOrdenesPorEstatus(estatus));
            }

            if (ordenes.isEmpty()) {
                log.warn("⚠ B2B no regresó órdenes para seguimientoEntrega");
                return;
            }

            log.info("iniciando prodcesar ordenes");
            for (B2BSeguimientoEntregaOrdenDTO orden : ordenes) {
                procesarOrden(orden);
            }

        } catch (Exception e) {
            log.error("❌ Error general en sincronización", e);
        }
    }

    private List<B2BSeguimientoEntregaOrdenDTO> consultarOrdenesPorEstatus(String cveEstatusOdc) {

        try {
            log.info("🔎 Consultando OCs | cveEstatusOdc={}", cveEstatusOdc);

            String json = b2bClient.seguimientoEntrega(cveEstatusOdc);

            B2BSeguimientoEntregaResponseDTO response = mapper.readValue(json, B2BSeguimientoEntregaResponseDTO.class);

            log.info("lo que recibi de b2b OC \n{}", response.getData());
            if (response.getData() == null) {
                return List.of();
            }

            return response.getData();

        } catch (Exception e) {
            log.error("❌ Error consultando OCs | cveEstatusOdc={}", cveEstatusOdc, e);
            return List.of(); // 🔥 no romper flujo
        }
    }

    private void procesarOrden(B2BSeguimientoEntregaOrdenDTO orden) {

        log.debug("➡ Procesando orden={}", orden);

        try {
            // 3️⃣ Estafeta → historial (usar WAYBILL)
            String jsonEstafeta = estafetaClient.consultarHistorialNumReferencia(
                    orden.getWaybill());

            log.debug("📥 JSON Estafeta={}", jsonEstafeta);

            EstafetaResponse response = mapper.readValue(jsonEstafeta, EstafetaResponse.class);

            if (response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("⚠ Sin items Estafeta | guia={}", orden.getWaybill());
                return;
            }

            EstafetaResponse.Item item = response.getItems().get(0);

            // 🚫 NUEVA VALIDACIÓN: error operacional de Estafeta
            if (item.getError() != null) {
                log.warn(
                        "⚠ Estafeta sin información operacional | guia={} | errorCode={} | errorDesc={}",
                        orden.getWaybill(),
                        item.getError().getCode(),
                        item.getError().getDescription());
                return; // 🔥 NO mandar a B2B
            }
            EstafetaResponse.Status status = item.getStatusCurrent();

            if (status == null) {
                log.warn("⚠ Sin statusCurrent | guia={}", orden.getWaybill());
                return;
            }

            // 🛡️ IDEMPOTENCIA: si B2B ya tiene el mismo estatus, no actualizar
            if (orden.getIdEstatusDelivery() != null
                    && status.getCode().equals(String.valueOf(orden.getIdEstatusDelivery()))) {

                log.info(
                        "⏭ Idempotencia | Orden={} | Estatus ya aplicado={} | No se envía a B2B",
                        orden.getOrderNumber(),
                        status.getCode());
                return;
            }

            // 🔎 LOG CLAVE
            log.info(
                    "🧾 Estafeta | waybill={} | trackingEstafeta={} | code={} | desc={} | fecha={}",
                    orden.getWaybill(),
                    item.getInformation().getTrackingCode(),
                    status.getCode(),
                    status.getSpanishName(),
                    status.getLocalDateTime());

            // 4️⃣ Mapear → B2B
            B2BActualizarEstatusEntregaDTO update = new B2BActualizarEstatusEntregaDTO();

            // 🔥 CAMBIOS CLAVE
            update.setReferenceNumber(orden.getReferenceNumber()); // referenceNumber
            update.setTrackingCode(item.getInformation().getTrackingCode()); // trackingCode
            update.setOrderNumber(orden.getOrderNumber()); // orderNumber

            update.setCodigoEntrega(status.getCode());
            // update.setCodigoEntrega("6");
            update.setDescripcionEntrega(status.getSpanishName());

            // fechaEstatus → desde Estafeta
            String fechaEstatus;

            if (status.getLocalDateTime() != null
                    && status.getLocalDateTime().length() >= 10) {

                fechaEstatus = status.getLocalDateTime().substring(0, 10);

            } else {
                fechaEstatus = java.time.LocalDate.now().toString();

                log.warn(
                        "⚠ localDateTime nulo | guia={} | usando fecha actual={}",
                        orden.getWaybill(),
                        fechaEstatus);
            }

            update.setFechaEstatus(fechaEstatus);

            log.info("➡ DTO B2B update={}", update);

            // 5️⃣ B2B → actualizar
            b2bClient.actualizarEstatusDelivery(update);

        } catch (Exception e) {
            log.error("❌ Error procesando orden={}", orden.getOrderNumber(), e);
        }
    }

    private B2BSeguimientoEntregaResponseDTO obtenerGuias() {

        log.warn("🧪 Usando MOCK de seguimientoEntrega");

        List<B2BSeguimientoEntregaOrdenDTO> ordenes = List.of(
                crearOrdenMock("2015410173997631417025"),
                crearOrdenMock("4055911250502700000019"),
                crearOrdenMock("1234567890123456789012"));

        B2BSeguimientoEntregaResponseDTO response = new B2BSeguimientoEntregaResponseDTO();

        response.setIsSuccess(true);
        response.setData(ordenes);

        return response;
    }

    private B2BSeguimientoEntregaOrdenDTO crearOrdenMock(String guia) {

        B2BSeguimientoEntregaOrdenDTO orden = new B2BSeguimientoEntregaOrdenDTO();

        orden.setOrderNumber("OC-MOCK-" + guia.substring(0, 6));
        orden.setReferenceNumber("REF-MOCK-" + guia.substring(0, 6));
        orden.setTrackingCode(guia);

        return orden;
    }

}
