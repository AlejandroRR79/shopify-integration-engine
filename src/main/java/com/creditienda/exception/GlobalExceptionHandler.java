package com.creditienda.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * JSON malformado o tipo incorrecto en el body.
     * Antes devolvía 403 — ahora devuelve 400 con detalle.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseError(
            HttpMessageNotReadableException ex) {

        String causa = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        log.warn("[GLOBAL-HANDLER] JSON inválido: {}", causa);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "JSON inválido — revisa el formato del payload");
        body.put("detalle", causa);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Errores de Shopify API (422, 404, etc.) que burbujean
     * desde servicios que no los atrapan internamente.
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleShopifyClientError(
            HttpClientErrorException ex) {

        log.warn("[GLOBAL-HANDLER] Error de servicio externo: {} — {}",
                ex.getStatusCode(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("error", "Error en servicio externo");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * ShopifyException lanzada explícitamente por los servicios.
     */
    @ExceptionHandler(ShopifyException.class)
    public ResponseEntity<Map<String, Object>> handleShopifyException(
            ShopifyException ex) {

        log.warn("[GLOBAL-HANDLER] ShopifyException: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", ex.getType());
        body.put("message", ex.getMessage());
        body.put("detalle", ex.getDetail() != null ? ex.getDetail() : "");

        return ResponseEntity.unprocessableEntity().body(body);
    }

    /**
     * Argumentos inválidos — parámetros faltantes o valores incorrectos.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("[GLOBAL-HANDLER] Argumento inválido: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(body);
    }
}
