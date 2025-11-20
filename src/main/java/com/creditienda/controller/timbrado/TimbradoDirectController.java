package com.creditienda.controller.timbrado;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.timbrado.TimbradoJsonDirectService;

@RestController
@RequestMapping("/api/timbrado/secure")
public class TimbradoDirectController {

    private static final Logger logger = LoggerFactory.getLogger(TimbradoDirectController.class);

    private final TimbradoJsonDirectService timbradoService;

    public TimbradoDirectController(TimbradoJsonDirectService timbradoService) {
        this.timbradoService = timbradoService;
    }

    @PostMapping("/timbre")
    public ResponseEntity<String> timbrar(@RequestBody String jsonCrudo) {
        logger.info("📥 Solicitud recibida para timbrado directo");

        try {
            String respuesta = timbradoService.timbrarDesdeJson(jsonCrudo);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            logger.error("❌ Error en timbrado directo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al timbrar: " + e.getMessage());
        }
    }

    @GetMapping("/descargar-pdf-uuid")
    public ResponseEntity<String> descargarPdfPorUuid(@RequestParam("idCfdi") String uuidCfdi) {
        logger.info("📥 Solicitud GET para descargar PDF por UUID: {}", uuidCfdi);

        if (uuidCfdi == null || uuidCfdi.isBlank()) {
            return ResponseEntity.badRequest().body("❌ idCfdi es obligatorio");
        }

        try {
            String pdfBase64 = timbradoService.descargarPdfPorUuid(uuidCfdi);
            return ResponseEntity.ok(pdfBase64);
        } catch (Exception e) {
            logger.error("❌ Error al descargar PDF por UUID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al descargar PDF por UUID: " + e.getMessage());
        }
    }

}