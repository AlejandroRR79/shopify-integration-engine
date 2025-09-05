package com.creditienda.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopify/secure")
public class ShopifySecureController {

    @GetMapping("/ordenping/{id}")
    public ResponseEntity<String> obtenerOrden(@PathVariable String id) {
        return ResponseEntity.ok("Acceso autorizado: datos protegidos de la orden " + id);
    }
}