package com.creditienda.service.b2b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.B2BActualizarEstatusResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    // ✅ CONSTRUCTOR CORRECTO
    public B2BDeliveryClient(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

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
        // 🔧 CORREGIDO: multipart → form-urlencoded (IIS compatible)
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 🔧 CORREGIDO: headers explícitos (evita 400 IIS)
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Connection", "close");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usuario", usuario);
        form.add("cveEstatusOdc", cveEstatusOdc);
        form.add("idSucursalCliente", idSucursalCliente);

        log.debug("📤 Payload seguimientoEntrega={}", form);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {

            // 🔧 CORREGIDO: postForObject → exchange
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            // 🔧 CORREGIDO: regresar SOLO el body
            log.debug("📥 Respuesta seguimientoEntrega={}", response.getBody());
            return response.getBody();

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

        // 🔧 CORREGIDO: multipart → form-urlencoded (IIS compatible)
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 🔧 CORREGIDO: headers explícitos (evita 400 IIS)
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Connection", "close");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("usuario", usuario);
        form.add("referenceNumber", dto.getReferenceNumber());
        form.add("trackingCode", dto.getTrackingCode());
        form.add("orderNumber", dto.getOrderNumber());
        form.add("codigoEntrega", dto.getCodigoEntrega());
        form.add("descripcionEntrega", dto.getDescripcionEntrega());
        form.add("fechaEstatus", dto.getFechaEstatus());

        log.info("📤 Payload actualizarEstatusDelivery={}", form);

        // 🔧 CORREGIDO: HttpEntity explícito (ANTES no estaba controlado)
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {

            // 🔧 CORREGIDO: postForObject → exchange
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            String responseJson = response.getBody();

            log.debug(
                    "📥 Respuesta B2B actualizarEstatusDelivery | order={} | response={}",
                    dto.getOrderNumber(),
                    responseJson);

            // ✅ Parseo correcto
            B2BActualizarEstatusResponseDTO responseDto = mapper.readValue(responseJson,
                    B2BActualizarEstatusResponseDTO.class);

            // ✅ Validación funcional
            if (!Boolean.TRUE.equals(responseDto.getIsSuccess())) {
                throw new IllegalStateException(
                        "B2B actualizarEstatusDelivery falló: " + responseDto.getError());
            }

        } catch (HttpStatusCodeException e) {

            log.error(
                    "❌ Error HTTP B2B | order={} | status={} | body={}",
                    dto.getOrderNumber(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e);

            throw e;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {

            log.error(
                    "❌ Error parseando respuesta B2B | order={} | response no válido",
                    dto.getOrderNumber(),
                    e);

            throw new IllegalStateException("Respuesta B2B inválida", e);

        } catch (Exception e) {

            log.error(
                    "❌ Error B2B actualizarEstatusDelivery | order={}",
                    dto.getOrderNumber(),
                    e);

            throw e;
        }
    }

}
