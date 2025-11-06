package com.creditienda.service.b2b;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class B2BTokenService {

    private static final Logger logger = LoggerFactory.getLogger(B2BTokenService.class);

    @Value("${b2b.oc.auth.url}")
    private String ocAuthUrl;

    @Value("${b2b.oc.auth.usuario}")
    private String ocUsuario;

    @Value("${b2b.oc.auth.empresa}")
    private String ocEmpresa;

    @Value("${b2b.oc.auth.password}")
    private String ocPassword;

    public String obtenerTokenOC() {
        logger.info("🔐 Iniciando solicitud de token B2B para registro de OC");
        logger.info("🔐 URL: {}", ocAuthUrl);
        logger.info("🔐 Credenciales usadas: usuario={}, empresa={}", ocUsuario, ocEmpresa);

        // JSON preformado como String
        String jsonBody;
        try {
            jsonBody = new ObjectMapper().writeValueAsString(
                    new OCAuthPayload(ocUsuario, ocEmpresa, ocPassword));
        } catch (Exception e) {
            logger.error("❌ Error al serializar el cuerpo JSON", e);
            throw new IllegalStateException("No se pudo construir el cuerpo de la solicitud B2B OC", e);
        }

        // Headers robustos
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Accept-Charset", "UTF-8");

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // Logs detallados
        logger.info("➡️ Payload enviado: {}", jsonBody);
        logger.info("➡️ Headers enviados: {}", headers.toSingleValueMap());

        ResponseEntity<Map> response;
        try {
            response = new RestTemplate().postForEntity(ocAuthUrl, entity, Map.class);
        } catch (Exception ex) {
            logger.error("❌ Error al invocar el endpoint de autenticación B2B OC", ex);
            throw new IllegalStateException("No se pudo conectar con el servicio de autenticación B2B OC", ex);
        }

        logger.info("✅ Código de respuesta: {}", response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object data = response.getBody().get("data");
            if (data instanceof Map<?, ?> map && map.get("accessToken") instanceof String token) {
                logger.info("🔐 Token B2B OC obtenido correctamente: {}", token);
                return token;
            } else {
                logger.warn("⚠️ Estructura inesperada en respuesta B2B OC: {}", response.getBody());
            }
        } else {
            logger.warn("⚠️ Falló autenticación B2B OC. Código de estado: {}", response.getStatusCode());
        }

        throw new IllegalStateException("No se pudo obtener el token B2B OC: respuesta inválida");
    }

    // Clase interna para representar el payload
    private static class OCAuthPayload {
        public String idUsuario;
        public String idEmpresa;
        public String password;

        public OCAuthPayload(String idUsuario, String idEmpresa, String password) {
            this.idUsuario = idUsuario;
            this.idEmpresa = idEmpresa;
            this.password = password;
        }
    }
}