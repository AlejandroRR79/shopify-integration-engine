package com.creditienda.service.timbrado;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.util.TokenProvider;

@Service
public class TimbradoJsonDirectService {

    private static final Logger logger = LoggerFactory.getLogger(TimbradoJsonDirectService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenProvider tokenProvider;

    @Value("${b2b.endpoint.timbre}")
    private String urlTimbre;

    @Value("${b2b.endpoint.descargaPdf}")
    private String urlDescargaPdf;

    public TimbradoJsonDirectService(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String timbrarDesdeJson(String jsonCrudo) {
        String token = tokenProvider.obtenerToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(jsonCrudo, headers);

        logger.info("📤 Enviando JSON directo al PAC:\n{}", jsonCrudo);
        logger.info("🔐 Token usado: {}", token);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlTimbre,
                    HttpMethod.POST,
                    entity,
                    String.class);
            logger.info("📥 Respuesta del PAC:\n{}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            logger.error("❌ Error al timbrar JSON directo: {}", e.getMessage(), e);
            throw new IllegalStateException("Error al timbrar JSON directo", e);
        }
    }

    public String descargarPdfPorUuid(String uuidCfdi) {
        String token = tokenProvider.obtenerToken();

        String url = String.format("%s/%s", urlDescargaPdf, uuidCfdi);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        logger.info("📤 Solicitando PDF desde B2B por UUID: {}", uuidCfdi);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);
            logger.info("📥 PDF recibido en Base64");
            return response.getBody();
        } catch (Exception e) {
            logger.error("❌ Error al descargar PDF por UUID: {}", e.getMessage(), e);
            throw new IllegalStateException("Error al descargar PDF por UUID", e);
        }
    }

}