package com.creditienda.service.delivery.core;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.dto.delivery.FacturacionResponse;
import com.creditienda.service.delivery.dao.DeliveryDAO;
import com.creditienda.util.constantes.EstatusCve;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryCoreService {

    private final DeliveryDAO dao;
    private static final Logger log = LoggerFactory.getLogger(DeliveryCoreService.class);

    private final RestTemplate restTemplate;

    public DeliveryCoreService(DeliveryDAO dao, RestTemplate restTemplate) {
        this.dao = dao;
        this.restTemplate = restTemplate;
    }

    @Transactional(readOnly = true)
    public List<B2BSeguimientoEntregaOrdenDTO> consultarOrdenesPorEstatus(List<String> idEstatusOC,
            List<Integer> idEstatusDeliverys) {

        return dao.findByEstatus(idEstatusOC, idEstatusDeliverys);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEstatusDelivery(
            B2BActualizarEstatusEntregaDTO dto,
            String cveEstatusOdcActual,
            boolean esDevolucion) {

        Integer codigoEstafeta = null;

        try {
            codigoEstafeta = Integer.valueOf(dto.getCodigoEstafetaOriginal());
        } catch (Exception e) {
            log.warn("Código inválido: {}", dto.getCodigoEstafetaOriginal());
        }

        // 🔥 ahora regresa CVE (String)
        String nuevoCveOdc = mapearEstatusOdc(
                cveEstatusOdcActual,
                codigoEstafeta,
                esDevolucion);
        log.info("ODC actual={}, codigoEstafeta={}, nuevoODC={}",
                cveEstatusOdcActual,
                codigoEstafeta,
                nuevoCveOdc);
        // 🔥 DAO ya trabaja con CVE
        try {
            dao.updateEstatusDelivery(dto, nuevoCveOdc, esDevolucion);
        } catch (Exception e) {
            log.error("Error en updateEstatusDelivery, haciendo rollback", e);
            throw e; // Propaga la excepción para que @Transactional haga rollback
        }

    }

    private String mapearEstatusOdc(
            String cveOdcActual,
            Integer codigoEstafeta,
            boolean esDevolucion) {

        if (codigoEstafeta == null)
            return cveOdcActual;

        // =========================
        // 🔥 TRANSICIÓN INICIAL (recoleccion → transito)
        // =========================
        if (EstatusCve.RECOLECCION.equals(cveOdcActual)) {

            switch (codigoEstafeta) {

                case 2:
                case 3:
                case 5:
                case 6:
                case 7:
                    return EstatusCve.TRANSITO;

                case 8:
                    return esDevolucion
                            ? EstatusCve.DEVOLUCION
                            : EstatusCve.TRANSITO;

                case 1:
                    return EstatusCve.RECOLECCION;

                default:
                    return cveOdcActual;
            }
        }

        // =========================
        // 🔥 FLUJO NORMAL (transito)
        // =========================
        if (EstatusCve.TRANSITO.equals(cveOdcActual)) {

            switch (codigoEstafeta) {

                case 2:
                case 3:
                case 5:
                    return EstatusCve.TRANSITO;

                case 6:
                    return EstatusCve.EMBARCADA;

                case 7:
                    return EstatusCve.ENTREGA_SUCURSAL;

                case 8:
                    return esDevolucion
                            ? EstatusCve.DEVOLUCION
                            : EstatusCve.TRANSITO;

                default:
                    return cveOdcActual;
            }
        }

        // =========================
        // 🔥 FLUJO DEVOLUCIÓN (devolucion → entregaDevolucion)
        // =========================
        if (EstatusCve.DEVOLUCION.equals(cveOdcActual)) {

            switch (codigoEstafeta) {

                case 1:
                case 2:
                case 3:
                    return EstatusCve.DEVOLUCION;

                case 4:
                case 9:
                    return EstatusCve.ENTREGA_DEVOLUCION;

                default:
                    return cveOdcActual;
            }
        }

        return cveOdcActual;
    }

    public FacturacionResponse invocarFacturacion(String url, Map<String, String> body) {

        FacturacionResponse result = new FacturacionResponse();

        try {

            ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());

            boolean isSuccess = json.has("isSuccess") && json.get("isSuccess").asBoolean();

            String nombreB2B = null;

            if (json.has("data") && json.get("data").has("documentoB2B")) {
                JsonNode doc = json.get("data").get("documentoB2B");

                if (doc.has("NombreB2B")) {
                    nombreB2B = doc.get("NombreB2B").asText();
                }
            }

            result.setSuccess(isSuccess);
            result.setNombreB2B(nombreB2B);

        } catch (Exception e) {

            log.error("❌ Error invocando facturación", e);

            result.setSuccess(false);
        }

        return result;
    }
}
