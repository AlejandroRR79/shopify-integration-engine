package com.creditienda.controller.web;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.creditienda.service.complemento.ComplementoPagoService;

@RestController
@RequestMapping("/api/secure")
public class UploadController {

    private static final Logger log = LogManager.getLogger(UploadController.class);

    @Autowired
    private ComplementoPagoService complementoPagoService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío o no enviado"));
        }

        log.info("Upload recibido: nombre='{}', size={} bytes, contentType='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        complementoPagoService.procesar(file);

        return ResponseEntity.ok(Map.of("mensaje", "Complemento de Pago recibido y procesado"));
    }

    /**
     * Flujo 2: Subir filas seleccionadas (JSON)
     */
    @PostMapping("/uploadSelected")
    public ResponseEntity<?> uploadSelected(@RequestBody List<Map<String, Object>> registros) {
        if (registros == null || registros.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se enviaron registros seleccionados"));
        }

        log.info("UploadSelected recibido: {} registros", registros.size());

        // Aquí puedes delegar a otro método de tu servicio
        complementoPagoService.procesarSeleccionados(registros);

        return ResponseEntity.ok(Map.of("mensaje", "Registros seleccionados recibidos y procesados"));
    }

}