package com.creditienda.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@RestController
public class SaludoController {
    @Autowired
    private WebClient webClient;

    @SuppressWarnings("unchecked")
    @GetMapping("/demo-json")
    public Map<String, Object> obtenerDemoJson() {
        Mono<Map> respuesta = webClient
            .get()
            .uri("https://jsonplaceholder.typicode.com/posts/1")
            .retrieve()
            .bodyToMono(Map.class);

        return respuesta.block(); // ← sincrónico para simplificar
    }




    @GetMapping("/saludo")
    public String saludar() {
        return "Hola Alejandro, el servicio REST está funcionando correctamente 🚀";
    }

    /* 
        @GetMapping("/demo-json")
    public Map<String, Object> obtenerDemoJson() {
        return Map.of(
            "mensaje", "Hola Alejandro, este es un JSON de prueba",
            "estado", "activo",
            "codigo", 200
        );
    }
*/
}
