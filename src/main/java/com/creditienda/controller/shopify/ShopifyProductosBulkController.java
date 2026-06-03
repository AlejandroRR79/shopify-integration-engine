package com.creditienda.controller.shopify;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.ProductosLoteRequestDTO;
import com.creditienda.dto.shopify.RespuestaLoteBulkDTO;
import com.creditienda.service.shopify.ShopifyProductosBulkService;

@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifyProductosBulkController {

        private static final Logger log = LoggerFactory.getLogger(ShopifyProductosBulkController.class);

        private final ShopifyProductosBulkService bulkService;

        public ShopifyProductosBulkController(ShopifyProductosBulkService bulkService) {
                this.bulkService = bulkService;
        }

        // =============================
        // 1️⃣ PRECIO + INVENTARIO
        // =============================

        @PostMapping("/actualizar-lote-bulk")
        public ResponseEntity<RespuestaLoteBulkDTO> actualizarLoteBulk(
                        @RequestBody ProductosLoteRequestDTO request) {

                if (request == null
                                || request.getProductos() == null
                                || request.getProductos().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body(new RespuestaLoteBulkDTO(
                                                        List.of(), // exitosos
                                                        List.of(), // fallidos precio
                                                        List.of(), // fallidos inventario
                                                        0,
                                                        "La lista de productos no puede estar vacía"));
                }

                log.info("🚀 Shopify BULK | productos={}",
                                request.getProductos().size());

                return ResponseEntity.ok(
                                bulkService.actualizarProductosBulk(
                                                request.getProductos()));
        }

        // =============================
        // 2️⃣ SOLO PRECIO
        // =============================
        @PostMapping("/bulk/precio")
        public ResponseEntity<RespuestaLoteBulkDTO> bulkPrecio(
                        @RequestBody List<ProductoActualizarDTO> productos) {

                if (productos == null || productos.isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body(new RespuestaLoteBulkDTO(
                                                        List.of(),
                                                        List.of(),
                                                        List.of(),
                                                        0,
                                                        "La lista de productos no puede estar vacía"));
                }

                log.info("💲 Shopify BULK PRECIO | productos={}",
                                productos.size());

                return ResponseEntity.ok(
                                bulkService.actualizarPreciosBulk(productos));
        }

        // =============================
        // 3️⃣ SOLO INVENTARIO
        // =============================
        @PostMapping("/bulk/inventario")
        public ResponseEntity<RespuestaLoteBulkDTO> bulkInventario(
                        @RequestBody List<ProductoActualizarDTO> productos) {

                if (productos == null || productos.isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body(new RespuestaLoteBulkDTO(
                                                        List.of(),
                                                        List.of(),
                                                        List.of(),
                                                        0,
                                                        "La lista de productos no puede estar vacía"));
                }

                log.info("📦 Shopify BULK INVENTARIO | productos={}",
                                productos.size());

                return ResponseEntity.ok(
                                bulkService.actualizarInventarioBulk(productos));
        }
}
