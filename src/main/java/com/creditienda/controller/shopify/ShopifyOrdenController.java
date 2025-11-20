package com.creditienda.controller.shopify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.service.shopify.ShopifyProcesarOrderService;

@RestController
@RequestMapping("/api/shopify/secure/ordenes")
public class ShopifyOrdenController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyOrdenController.class);

    private final ShopifyProcesarOrderService shopifyOrderService;

    public ShopifyOrdenController(ShopifyProcesarOrderService shopifyOrderService) {
        this.shopifyOrderService = shopifyOrderService;
    }

    @PostMapping
    public ResponseEntity<String> procesarOrdenesShopify() {

        try {
            boolean enviada = shopifyOrderService.procesarUnaOrden();
            int procesadas = enviada ? 1 : 0;

            return ResponseEntity.ok("✅ Órdenes procesadas correctamente: " + procesadas);
        } catch (Exception e) {
            logger.error("❌ Error al procesar órdenes Shopify: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar órdenes Shopify: " + e.getMessage());
        }
    }
}