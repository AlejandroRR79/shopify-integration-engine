package com.creditienda.service.b2b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.service.notificacion.NotificacionService;

@Service
public class B2BService {

    private static final Logger logger = LoggerFactory.getLogger(B2BService.class);

    @Value("${b2b.order.url}")
    private String orderUrl;

    private final NotificacionService notificacionService;

    private final RestTemplate restTemplate;

    public B2BService(NotificacionService notificacionService, RestTemplate restTemplate) {
        this.notificacionService = notificacionService;
        this.restTemplate = restTemplate;
    }

    public boolean enviarOrden(String rawBody, String token, boolean enviarNotificacion) {
        logger.info("📦 Enviando orden a B2B: {}", orderUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(rawBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(orderUrl, entity, String.class);
            logger.info("✅ Orden enviada. Código de respuesta: {}", response.getStatusCode());

            if (enviarNotificacion) {
                notificacionService.enviarConfirmacion("✅ Orden enviada correctamente a B2B:\n" + rawBody);
            }

            return true;

        } catch (HttpClientErrorException.Conflict conflict) {
            String detalle = conflict.getResponseBodyAsString();
            logger.warn("⚠️ Orden ya registrada: {}", detalle);

            if (enviarNotificacion) {
                notificacionService
                        .enviarError("⚠️ Orden ya registrada en B2B:\n" + rawBody + "\n\nRespuesta:\n" + detalle);
            }

            return false;

        } catch (HttpClientErrorException ex) {
            int codigo = ex.getStatusCode().value();
            String detalle = ex.getResponseBodyAsString();
            String mensaje = codigo + " → " + detalle;

            logger.error("❌ Error HTTP al enviar orden: {}", mensaje);

            if (enviarNotificacion) {
                notificacionService
                        .enviarError("❌ Error HTTP al enviar orden a B2B:\n" + rawBody + "\n\nRespuesta:\n" + mensaje);
            }

            throw new IllegalStateException(mensaje, ex);

        } catch (Exception ex) {
            String mensaje = "Error inesperado → " + (ex.getMessage() != null ? ex.getMessage() : "Sin detalle");
            logger.error("❌ Error inesperado al enviar orden", ex.getMessage());

            if (enviarNotificacion) {
                notificacionService.enviarError(
                        "❌ Error inesperado al enviar orden a B2B:\n" + rawBody + "\n\nExcepción:\n" + mensaje);
            }

            throw new IllegalStateException(mensaje, ex);
        }
    }
}