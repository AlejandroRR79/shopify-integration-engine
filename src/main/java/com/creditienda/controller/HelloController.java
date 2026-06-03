package com.creditienda.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Map<String, Object> saludar() {
        return Map.of(
                "mensaje", "API REST funcionando correctamente",
                "timestamp", System.currentTimeMillis());
    }
}