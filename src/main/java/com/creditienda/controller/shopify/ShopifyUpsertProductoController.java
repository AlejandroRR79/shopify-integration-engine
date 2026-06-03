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

@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifyUpsertProductoController {

    private final ShopifyUpsertProductoService service;

    public ShopifyUpsertProductoController(ShopifyUpsertProductoService service) {
        this.service = service;
    }

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
