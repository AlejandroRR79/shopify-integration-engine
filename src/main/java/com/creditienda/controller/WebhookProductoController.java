package com.creditienda.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookProductoController {

    private static final Logger logger = LogManager.getLogger(WebhookProductoController.class);
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @PostMapping("/productos")
    public ResponseEntity<String> recibirJson(@RequestBody Map<String, Object> payload) {
        try {
            String jsonFormateado = mapper.writeValueAsString(payload);
            logger.info("JSON recibido en /webhook/productos:\n{}", jsonFormateado);
        } catch (Exception e) {
            logger.error("Error al formatear el JSON", e);
        }

        return ResponseEntity.ok("JSON recibido correctamente");
    }
}
