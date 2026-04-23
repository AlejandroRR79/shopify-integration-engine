package com.creditienda.service.facturacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.dto.delivery.FacturacionResponse;
import com.creditienda.service.delivery.core.DeliveryCoreService;
import com.creditienda.service.notificacion.NotificacionService;

@Service
public class DeliveryFacturacionService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFacturacionService.class);

    private final DeliveryCoreService coreService;
    private final NotificacionService notificacionService;

    @Value("${b2b.delivery.base.url}")
    private String b2bBaseUrl;

    @Value("${b2b.delivery.endpoint.facturar}")
    private String b2bFacturarEndpoint;

    @Value("${b2b.delivery.usuarioFacturacion}")
    private String usuarioB2B;

    // 🔥 NUEVO (parametrizado)
    @Value("${facturacion.estatus.odc}")
    private String estatusOdcProp;

    public DeliveryFacturacionService(
            DeliveryCoreService coreService,
            NotificacionService notificacionService) {

        this.coreService = coreService;
        this.notificacionService = notificacionService;
    }

    public void ejecutarFacturacion() {

        List<String> facturadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        // 🔥 convertir property a lista
        List<String> estatusOdc = Arrays.stream(estatusOdcProp.split(","))
                .map(String::trim)
                .toList();

        log.info("📄 Estatus a facturar: {}", estatusOdc);

        // 🔥 reutiliza tu query existente
        List<B2BSeguimientoEntregaOrdenDTO> ordenes = coreService.consultarOrdenesPorEstatus(estatusOdc, null);

        if (ordenes == null || ordenes.isEmpty()) {
            log.warn("⚠ No hay órdenes para facturar");
            return;
        }

        for (B2BSeguimientoEntregaOrdenDTO orden : ordenes) {

            try {

                String url = b2bBaseUrl + b2bFacturarEndpoint;

                Map<String, String> body = new HashMap<>();
                body.put("idShopifyOrder", String.valueOf(orden.getIdShopifyOrder()));
                body.put("usuario", usuarioB2B);

                FacturacionResponse resp = coreService.invocarFacturacion(url, body);

                if (!resp.isSuccess()) {

                    errores.add(
                            "Facturación fallida | OC=" + orden.getOrderNumber() +
                                    " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                    " | waybill=" + orden.getWaybill());

                } else {

                    String nombreB2B = resp.getNombreB2B() != null
                            ? resp.getNombreB2B()
                            : "N/A";

                    facturadas.add(
                            "OC=" + orden.getOrderNumber() +
                                    " | fechaSolicitud=" + orden.getFechaSolicitud() +
                                    " | waybill=" + orden.getWaybill() +
                                    " | documentoB2B=" + nombreB2B);

                    log.info("📄 Facturada OC={} doc={}", orden.getOrderNumber(), nombreB2B);
                }

            } catch (Exception e) {

                errores.add(
                        "Error facturación | OC=" + orden.getOrderNumber() +
                                " | waybill=" + orden.getWaybill());

                log.error("❌ Error facturando OC={}", orden.getOrderNumber(), e);
            }
        }

        enviarCorreo(facturadas, errores);
    }

    private void enviarCorreo(List<String> facturadas, List<String> errores) {

        if (facturadas.isEmpty() && errores.isEmpty()) {
            log.info("ℹ No hubo facturación");
            return;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("📄 RESUMEN FACTURACIÓN B2B\n\n");

        if (!facturadas.isEmpty()) {
            sb.append("✅ FACTURADAS:\n");
            facturadas.forEach(o -> sb.append(" - ").append(o).append("\n"));
            sb.append("\n");
        }

        if (!errores.isEmpty()) {
            sb.append("⚠ ERRORES:\n");
            errores.forEach(o -> sb.append(" - ").append(o).append("\n"));
        }

        notificacionService.enviarResumen(sb.toString());
    }
}
