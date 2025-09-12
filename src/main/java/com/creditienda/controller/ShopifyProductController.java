package com.creditienda.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.ProductoShopifyDTO;
import com.creditienda.service.ShopifyProductService;

@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifyProductController {

    private static final Logger logger = LogManager.getLogger(ShopifyProductController.class);
    private final ShopifyProductService shopifyProductService;

    public ShopifyProductController(ShopifyProductService shopifyProductService) {
        this.shopifyProductService = shopifyProductService;
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> listarProductos() {
        try {
            System.out.println("Iniciando Controller de productos");
            Map<String, Object> productos = shopifyProductService.obtenerProductos();
            if (productos.containsKey("error")) {
                logger.warn("⚠️ Error al obtener productos: {}", productos.get("error"));
                return ResponseEntity.status(502).body(productos); // Bad Gateway
            }
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            logger.error("❌ Excepción inesperada en /products: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error interno al consultar productos", "detalle", e.getMessage()));
        }
    }

    @PostMapping("/registrarProducto")
    public ResponseEntity<Map<String, Object>> registrarProducto(@RequestBody ProductoShopifyDTO dto) {
        try {
            logger.info("📦 Recibiendo producto para registrar en Shopify: {}", dto.getTitle());
            Map<String, Object> resultado = shopifyProductService.registrarProducto(dto);
            logger.info("✅ Resultado: SKU={}, ID={}", resultado.get("sku"), resultado.get("id"));
            return ResponseEntity.ok(resultado); // Devuelve {sku: ..., id: ...}
        } catch (Exception e) {
            logger.error("❌ Error al registrar producto: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error al registrar producto en Shopify", "detalle", e.getMessage()));
        }
    }
}