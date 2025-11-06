package com.creditienda.controller.shopify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.b2b.B2BService;
import com.creditienda.service.b2b.B2BTokenService;
import com.creditienda.service.shopify.HmacValidator;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final HmacValidator hmacValidator;
    private final B2BTokenService tokenService;
    private final B2BService b2bService;

    @Autowired
    public WebhookController(HmacValidator hmacValidator,
            B2BTokenService tokenService,
            B2BService b2bService) {
        this.hmacValidator = hmacValidator;
        this.tokenService = tokenService;
        this.b2bService = b2bService;
    }

    /**
     * Endpoint público para registrar orden de compra (OC) desde Shopify hacia el
     * servicio B2B.
     */
    @PostMapping("/registrarOC")
    public ResponseEntity<String> registrarOC(
            @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmac,
            @RequestBody String rawBody) {

        if (!hmacValidator.validar(rawBody, hmac)) {
            logger.warn("❌ Firma HMAC inválida para payload: {}", rawBody);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Firma HMAC inválida");
        }

        String token;
        try {
            token = tokenService.obtenerTokenOC();
        } catch (Exception ex) {
            logger.error("❌ Error al obtener token B2B", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Error de autenticación con B2B");
        }

        try {
            boolean enviado = b2bService.enviarOrden(rawBody, token);
            if (enviado) {
                logger.info("se envío la OC y se registro");
                return ResponseEntity.ok("Orden enviada correctamente");
            } else {
                logger.warn("⚠️ Orden ya registrada, pero se responde 200 a Shopify");
                return ResponseEntity.ok("La orden ya estaba registrada");
            }

        } catch (Exception ex) {
            logger.error("❌ Error al procesar la orden", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Error al enviar orden");
        }
    }
}