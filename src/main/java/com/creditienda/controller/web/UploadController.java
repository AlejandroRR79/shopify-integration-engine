package com.creditienda.controller.web;

import java.util.Enumeration;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.creditienda.service.complemento.ComplementoPagoBatchService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/secure")
public class UploadController {

    private static final Logger log = LogManager.getLogger(UploadController.class);

    @Autowired
    private ComplementoPagoBatchService complementoPagoBatchService;

    /**
     * Flujo 1: Subir archivo completo (Excel)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload fallido: archivo vacío o no enviado");
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío o no enviado"));
        }

        log.info("Upload recibido: nombre='{}', size={} bytes, contentType='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 🚨 El servicio devuelve Map<String,Object>
        Map<String, Object> resultado = complementoPagoBatchService.procesarBatch(file);

        // Extraer cifras del resultado
        int total = (int) resultado.getOrDefault("totalRegistros", 0);
        int correctos = (int) resultado.getOrDefault("correctos", 0);
        int errores = (int) resultado.getOrDefault("errores", 0);
        int yaExistentes = (int) resultado.getOrDefault("yaExistentes", 0);

        log.info("Upload procesado: {} registros, {} correctos, {} errores, {} ya existentes",
                file.getOriginalFilename(), correctos, errores, yaExistentes);

        return ResponseEntity.ok(resultado);
    }

    @PostMapping(value = "/uploadComplementoPago", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadComplementoPago(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            log.info("Header {} = {}", name, request.getHeader(name));
        }

        if (file == null || file.isEmpty()) {
            log.warn("Upload fallido: archivo vacío o no enviado");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Archivo vacío o no enviado"));
        }

        log.info("Upload recibido: nombre='{}', size={} bytes, contentType='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        Map<String, Object> resultado = complementoPagoBatchService.procesarBatch(file);

        return ResponseEntity.ok(resultado);
    }

}