package com.creditienda.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.timbrado.CredencialesDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(TokenProvider.class);

    // @Value("${b2b.auth.url}")
    private String authUrl;

    // @Value("${b2b.auth.idEmpresa}")
    private String idEmpresa;

    // @Value("${b2b.auth.clave}")
    private String clave;

    // @Value("${b2b.auth.password}")
    private String password;

    private final RestTemplate restTemplate = new RestTemplate();

    public String obtenerToken() {
        try {
            // Crear DTO con los campos exactos
            CredencialesDTO credenciales = new CredencialesDTO();
            credenciales.setIdEmpresa(idEmpresa);
            credenciales.setClave(clave);
            credenciales.setPassword(password);

            // Serializar a JSON para inspección
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(credenciales);
            logger.info("📤 JSON autenticación:\n{}", json);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Enviar solicitud
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<Map> response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, Map.class);

            logger.info("📥 Status de respuesta: {}", response.getStatusCode());
            logger.info("📥 Body de respuesta: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                Boolean exito = (Boolean) bodyMap.get("exito");

                if (Boolean.TRUE.equals(exito) && bodyMap.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");
                    String token = (String) data.get("token");
                    logger.info("🔑 Token recibido: {}", token);
                    return token;
                } else {
                    String mensaje = (String) bodyMap.get("mensajeError");
                    logger.warn("⚠️ Autenticación fallida: {}", mensaje);
                    throw new RuntimeException("Autenticación fallida: " + mensaje);
                }
            } else {
                throw new RuntimeException("No se pudo obtener el token: respuesta inválida");
            }

        } catch (Exception e) {
            logger.error("❌ Error al obtener token", e);
            throw new RuntimeException("Error al autenticar contra el PAC", e);
        }
    }
}