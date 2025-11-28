package com.creditienda.controller.shopify;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.ProductosLoteRequestDTO;
import com.creditienda.dto.shopify.RespuestaActualizacionDTO;
import com.creditienda.dto.shopify.RespuestaLoteDTO;
import com.creditienda.service.shopify.ShopifyActualizarProductoService;

@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifyProductoController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyProductoController.class);

    private final ShopifyActualizarProductoService actualizarProductoService;

    public ShopifyProductoController(ShopifyActualizarProductoService actualizarProductoService) {
        this.actualizarProductoService = actualizarProductoService;
    }

    /**
     * Actualiza un producto individual
     * POST /api/shopify/secure/actualizar
     * Body: { "handle": "camisa-azul", "cantidad": 50, "precio": 599.00 }
     */
    @PostMapping("/actualizar")
    public ResponseEntity<RespuestaActualizacionDTO> actualizarProducto(
            @RequestBody ProductoActualizarDTO productoDTO) {
        try {
            logger.info("🔄 Solicitud para actualizar producto: {}", productoDTO);

            if (productoDTO == null) {
                return ResponseEntity.badRequest()
                        .body(new RespuestaActualizacionDTO(false, null, "El producto no puede ser nulo"));
            }

            RespuestaActualizacionDTO respuesta = actualizarProductoService.actualizarProductoIndividual(productoDTO);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            logger.error("❌ Error al actualizar producto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RespuestaActualizacionDTO(false,
                            productoDTO != null ? productoDTO.getHandle() : null,
                            "Error: " + e.getMessage()));
        }
    }

    /**
     * Actualiza múltiples productos
     * POST /api/shopify/secure/actualizar-lote
     * Body: { "productos": [ { "handle": "camisa-azul", "cantidad": 50, "precio":
     * 599.00 }, ... ] }
     */
    @PostMapping("/actualizar-lote")
    public ResponseEntity<RespuestaLoteDTO> actualizarProductosLote(
            @RequestBody ProductosLoteRequestDTO request) {
        try {
            List<ProductoActualizarDTO> productosDTO = request != null ? request.getProductos() : null;
            logger.info("🔄 Solicitud para actualizar {} productos",
                    productosDTO != null ? productosDTO.size() : 0);

            if (productosDTO == null || productosDTO.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new RespuestaLoteDTO(List.of(), List.of(), 0,
                                "La lista de productos no puede estar vacía"));
            }

            RespuestaLoteDTO respuesta = actualizarProductoService.actualizarProductosLote(productosDTO);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            logger.error("❌ Error al actualizar lote de productos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RespuestaLoteDTO(List.of(), List.of(), 0,
                            "Error: " + e.getMessage()));
        }
    }

}