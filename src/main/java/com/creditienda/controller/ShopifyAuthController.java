package com.creditienda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.util.JwtUtil;

@RestController
@RequestMapping("/api/shopify/auth")
public class ShopifyAuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/token")
    public ResponseEntity<String> generarToken(@RequestParam String usuario, @RequestParam String clave) {
        if ("admin".equals(usuario) && "1234".equals(clave)) {
            String token = jwtUtil.generarToken(usuario);
            return ResponseEntity.ok(token);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        }
    }
}