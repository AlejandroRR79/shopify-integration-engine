package com.creditienda.service.b2b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;

import jakarta.annotation.PostConstruct;

@Service
public class B2BDeliveryClient {

    private static final Logger log = LoggerFactory.getLogger(B2BDeliveryClient.class);

    @Value("${b2b.delivery.base.url}")
    private String baseUrl;

    @Value("${b2b.delivery.usuario}")
    private String usuario;

    @Value("${b2b.delivery.cve-estatus-odc}")
    private String cveEstatusOdc;

    @Value("${b2b.delivery.id-sucursal-cliente}")
    private String idSucursalCliente;

    // 👉 NUEVO: endpoints parametrizados
    @Value("${b2b.delivery.endpoint.seguimiento}")
    private String seguimientoEndpoint;

    @Value("${b2b.delivery.endpoint.actualizar}")
    private String actualizarEndpoint;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void logConfig() {
        log.info("🔧 B2B CONFIG CARGADA");
        log.info("   baseUrl={}", baseUrl);
        log.info("   seguimientoEndpoint={}", seguimientoEndpoint);
        log.info("   actualizarEndpoint={}", actualizarEndpoint);
        log.info("   usuario={}", usuario);
        log.info("   idSucursalCliente={}", idSucursalCliente);
    }

    // ================= seguimientoEntrega =================
    public String seguimientoEntrega(String cveEstatusOdc) {
        String url = baseUrl + seguimientoEndpoint;

        log.debug("➡ Llamando seguimientoEntrega");
        log.debug("   URL={}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usuario", usuario);
        form.add("cveEstatusOdc", cveEstatusOdc);
        form.add("idSucursalCliente", idSucursalCliente);

        log.debug("📤 Payload seguimientoEntrega={}", form);

        try {
            String response = restTemplate.postForObject(
                    url,
                    new HttpEntity<>(form, headers),
                    String.class);

            log.debug("📥 Respuesta seguimientoEntrega={}", response);
            return response;

        } catch (Exception e) {
            log.error("❌ Error llamando seguimientoEntrega", e);
            throw e;
        }
    }

    // ================= actualizarEstatusDelivery =================
    public void actualizarEstatusDelivery(B2BActualizarEstatusEntregaDTO dto) {

        String url = baseUrl + actualizarEndpoint;

        log.debug("➡ Llamando actualizarEstatusDelivery");
        log.info("   URL={}", url);
        log.info("   DTO={}", dto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usuario", usuario);
        form.add("referenceNumber", dto.getReferenceNumber());
        form.add("trackingCode", dto.getTrackingCode());
        form.add("orderNumber", dto.getOrderNumber());
        form.add("codigoEntrega", dto.getCodigoEntrega());
        form.add("descripcionEntrega", dto.getDescripcionEntrega());
        form.add("fechaEstatus", dto.getFechaEstatus());

        log.info("📤 Payload actualizarEstatusDelivery={}", form);

        try {

            String response = restTemplate.postForObject(
                    url,
                    new HttpEntity<>(form, headers),
                    String.class);

            log.debug(
                    "📥 Respuesta B2B actualizarEstatusDelivery | order={} | response={}",
                    dto.getOrderNumber(),
                    response);

        } catch (HttpStatusCodeException e) {

            // ❌ Error HTTP controlado (400 / 500)
            log.error(
                    "❌ Error HTTP B2B | order={} | status={} | body={}",
                    dto.getOrderNumber(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            // 🔥 NO relanzar → el job continúa

        } catch (Exception e) {

            // ❌ Error técnico (timeout, conexión, etc.)
            log.error(
                    "❌ Error técnico B2B | order={}",
                    dto.getOrderNumber(),
                    e);

            // 🔥 NO relanzar → el job continúa
        }
    }
}
