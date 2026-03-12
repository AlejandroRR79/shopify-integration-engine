package com.creditienda.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.model.timbrado.Documento;
import com.creditienda.util.TokenProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

@Service
@Deprecated
/**
 * @deprecated Este cliente de timbrado está marcado como obsoleto y no se
 *             utiliza por el momento.
 *             Favor de usar la implementación actualizada o reactivar
 *             `TimbradoJsonDirectService` si es necesario.
 */
public class TimbradoClient {

    private static final Logger logger = LoggerFactory.getLogger(TimbradoClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TokenProvider tokenProvider;

    // @Value("${b2b.endpoint.timbre}")
    private String urlTimbre;

    // @Value("${b2b.endpoint.timbreIntegrador}")
    private String urlTimbreIntegrador;

    public TimbradoClient(TokenProvider tokenProvider, RestTemplate restTemplate) {
        this.tokenProvider = tokenProvider;
        this.restTemplate = restTemplate;

        logger.warn(
                "TimbradoClient marcado como @Deprecated: actualmente no se usa y puede ser eliminado en el futuro.");
    }

    public String timbrarJson(Documento doc) throws Exception {
        return enviar(doc, urlTimbre);
    }

    public String enviar(Documento doc, String url) throws Exception {
        String token = tokenProvider.obtenerToken();

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Envolver en "documento"
        String json = mapper.writeValueAsString(Map.of("documento", doc));
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("❌ JSON generado está vacío");
        }

        logger.info("🔐 Token obtenido");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        // Se evita loguear headers y JSON completos por contener datos sensibles (CFDI)
        logger.info("📤 Preparando envío de documento al PAC");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);
            logger.info("📥 Respuesta recibida del PAC (body omitido en logs por sensibilidad)");
            return response.getBody();
        } catch (HttpServerErrorException e) {
            logger.error("❌ Error 5xx del PAC | status={} ", e.getStatusCode());
            throw e;
        } catch (HttpClientErrorException e) {
            logger.error("❌ Error 4xx del PAC | status={}", e.getStatusCode());
            throw e;
        }
    }

    public String timbrarXmlBase64(Documento doc) throws Exception {
        // 1. Serializar a XML
        JAXBContext context = JAXBContext.newInstance(Documento.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter xmlWriter = new StringWriter();
        marshaller.marshal(doc, xmlWriter);
        String xml = xmlWriter.toString();

        // 2. Codificar en Base64
        String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));

        // 3. Preparar JSON de envío
        Map<String, String> payload = Map.of("xml", xmlBase64);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenProvider.obtenerToken());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        // 4. Enviar al PAC (ajusta la URL según Postman)
        String urlXml = urlTimbreIntegrador;
        ResponseEntity<String> response = restTemplate.exchange(
                urlXml,
                HttpMethod.POST,
                entity,
                String.class);

        logger.info("📥 Respuesta recibida del PAC (body omitido en logs por sensibilidad)");
        return response.getBody();
    }
}