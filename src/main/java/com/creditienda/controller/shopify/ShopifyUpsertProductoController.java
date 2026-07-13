package com.creditienda.controller.shopify;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.common.ErrorResponseDTO;
import com.creditienda.dto.shopify.ShopifyProductUpsertDTO;
import com.creditienda.exception.ShopifyException;
import com.creditienda.service.shopify.ShopifyUpsertProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Shopify - Upsert Producto", description = "Registro o actualizacion de producto en tienda Shopify principal. Requiere JWT.")
@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifyUpsertProductoController {

    private final ShopifyUpsertProductoService service;

    public ShopifyUpsertProductoController(ShopifyUpsertProductoService service) {
        this.service = service;
    }

    @Operation(summary = "Upsert producto", description = "Crea el producto si no existe por handle, o lo actualiza si ya existe. Actualiza tambien variante e imagenes.")
    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(
            @RequestBody ShopifyProductUpsertDTO dto) {

        try {
            String result = service.upsert(dto);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", result));

        } catch (ShopifyException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(
                            e.getType(),
                            e.getMessage(),
                            e.getDetail()));
        }
    }
}
