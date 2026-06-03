package com.creditienda.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedController {

    @GetMapping("/api/secure/data")
    public Map<String, Object> getSecureData() {
        return Map.of("status", "ok", "mensaje", "Acceso autorizado con token");
    }
}