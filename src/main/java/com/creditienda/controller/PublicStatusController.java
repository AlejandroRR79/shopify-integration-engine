package com.creditienda.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicStatusController {

    @GetMapping("/status")
    public Map<String, Object> estado() {
        return Map.of(
                "status", "ok",
                "mensaje", "API pública disponible sin autenticación",
                "timestamp", System.currentTimeMillis());
    }
}