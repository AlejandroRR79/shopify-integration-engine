package com.creditienda.service.shopify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.service.b2b.B2BService;
import com.creditienda.service.b2b.B2BTokenService;
import com.creditienda.service.notificacion.NotificacionService;
import com.creditienda.util.ShopifyOrderMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ShopifyProcesarOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyProcesarOrderService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    @Autowired
    private RestTemplate restTemplate;

    private final B2BService b2bService;
    private final B2BTokenService b2bTokenService;
    private final NotificacionService notificacionService;

    public ShopifyProcesarOrderService(B2BService b2bService,
            NotificacionService notificacionService,
            B2BTokenService b2bTokenService) {
        this.b2bService = b2bService;
        this.notificacionService = notificacionService;
        this.b2bTokenService = b2bTokenService;
    }

    public boolean procesarUnaOrden() {
        String url = String.format("https://%s/admin/api/%s/orders.json?status=any", shopDomain, apiVersion);
        logger.info("🔗 Consultando órdenes desde Shopify: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonCrudo = response.getBody();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(jsonCrudo, Map.class);
            List<Map<String, Object>> ordenes = (List<Map<String, Object>>) jsonMap.get("orders");

            if (ordenes == null || ordenes.isEmpty()) {
                logger.warn("⚠️ No se encontraron órdenes en Shopify");
                return false;
            }

            Map<String, Object> orden = ordenes.get(0);
            Map<String, Object> registro = ShopifyOrderMapper.transformar(orden);
            String jsonRegistro = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(registro);

            System.out.println("📦 JSON transformado para B2B:\n" + jsonRegistro);

            String b2bToken = b2bTokenService.obtenerTokenOC();
            return b2bService.enviarOrden(jsonRegistro, b2bToken, true);

        } catch (JsonProcessingException e) {
            logger.error("❌ Error al procesar el JSON recibido: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("❌ Error inesperado al consumir Shopify: {}", e.getMessage(), e);
            return false;
        }
    }

    public String procesarOrdenesEntreFechas(LocalDate inicio, LocalDate fin) {
        List<String> exitosas = new ArrayList<>();
        Map<String, String> fallidas = new LinkedHashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> ordenes = obtenerOrdenesEntreFechas(inicio, fin);

        if (ordenes.isEmpty()) {
            return "No se encontraron órdenes en el rango indicado.";
        }

        String b2bToken;
        try {
            b2bToken = b2bTokenService.obtenerTokenOC();
        } catch (Exception e) {
            logger.error("❌ No se pudo obtener el token B2B OC: {}", e.getMessage(), e);
            return "Error al obtener token B2B OC: " + e.getMessage();
        }

        for (Map<String, Object> orden : ordenes) {
            try {
                Map<String, Object> registro = ShopifyOrderMapper.transformar(orden);
                String jsonRegistro = mapper.writeValueAsString(registro);
                String numeroOC = String.valueOf(orden.get("id"));
                logger.info("📦 Procesando orden {}", numeroOC);

                boolean enviado = b2bService.enviarOrden(jsonRegistro, b2bToken, false);

                if (enviado) {
                    exitosas.add(numeroOC);
                } else {
                    fallidas.put(numeroOC, "Orden ya registrada ");
                }

            } catch (Exception e) {
                String numeroOC = String.valueOf(orden.get("id"));
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido";
                fallidas.put(numeroOC, mensaje);
            }
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("📦 Resultado de sincronización Shopify → B2B\n\n");
        resumen.append("Rango: ").append(inicio).append(" a ").append(fin).append("\n\n");

        resumen.append("✅ Órdenes exitosas:\n");
        if (exitosas.isEmpty()) {
            resumen.append(" - Ninguna\n");
        } else {
            exitosas.forEach(oc -> resumen.append(" - ").append(oc).append("\n"));
        }

        resumen.append("\n❌ Órdenes con error:\n");
        if (fallidas.isEmpty()) {
            resumen.append(" - Ninguna\n");
        } else {
            fallidas.forEach((oc, error) -> resumen.append(" - ").append(oc).append(" → ").append(error).append("\n"));
        }

        resumen.append("\nTotal: ").append(exitosas.size()).append(" exitosas / ").append(fallidas.size())
                .append(" con error");

        logger.info("📋 Resumen de sincronización:\n{}", resumen.toString());
        notificacionService.enviarResumen(resumen.toString());

        return "Proceso completado. Se enviaron " + exitosas.size() + " órdenes.";
    }

    public List<Map<String, Object>> obtenerOrdenesEntreFechas(LocalDate inicio, LocalDate fin) {
        List<Map<String, Object>> todas = new ArrayList<>();

        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            String url = String.format(
                    "https://%s/admin/api/%s/orders.json?status=any&created_at_min=%sT00:00:00-06:00&created_at_max=%sT23:59:59-06:00",
                    shopDomain, apiVersion, fecha, fecha);

            logger.info("📆 Consultando órdenes del día {} → {}", fecha, url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                List<Map<String, Object>> ordenes = (List<Map<String, Object>>) response.getBody()
                        .getOrDefault("orders", List.of());

                logger.info("📦 {} órdenes encontradas para {}", ordenes.size(), fecha);
                todas.addAll(ordenes);

            } catch (Exception e) {
                logger.error("❌ Error al consultar órdenes para {}: {}", fecha, e.getMessage(), e);
            }
        }

        logger.debug("📊 Total de órdenes a procesar: {}", todas.size());
        return todas;
    }

    public boolean procesarUnaOrden(String numeroOrden) {
        String url = String.format("https://%s/admin/api/%s/orders/%s.json", shopDomain, apiVersion, numeroOrden);
        logger.info("🔗 Consultando orden específica en Shopify: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonCrudo = response.getBody();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(jsonCrudo, Map.class);
            Map<String, Object> orden = (Map<String, Object>) jsonMap.get("order");

            if (orden == null || orden.isEmpty()) {
                logger.warn("⚠️ No se encontró la orden con ID {}", numeroOrden);
                return false;
            }

            Map<String, Object> registro = ShopifyOrderMapper.transformar(orden);
            String jsonRegistro = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(registro);

            System.out.println("📦 JSON transformado para B2B:\n" + jsonRegistro);

            String b2bToken = b2bTokenService.obtenerTokenOC();
            return b2bService.enviarOrden(jsonRegistro, b2bToken, true);

        } catch (JsonProcessingException e) {
            logger.error("❌ Error al procesar el JSON recibido: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("❌ Error inesperado al consultar orden {}: {}", numeroOrden, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lista todos los IDs (order.id) dentro del rango, consultando día a día para
     * evitar
     * problemas de paginación en rangos grandes.
     */
    public List<String> listarOrderIdsPorDias(LocalDate fechaInicio, LocalDate fechaFin) {
        logger.info("🔎 listarOrderIdsPorDias - rango: {} → {}", fechaInicio, fechaFin);

        if (fechaInicio == null || fechaFin == null) {
            logger.warn("⚠️ listarOrderIdsPorDias - fechaInicio o fechaFin nulo");
            throw new IllegalArgumentException("fechaInicio y fechaFin son requeridas");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            logger.warn("⚠️ listarOrderIdsPorDias - fechaInicio {} es posterior a fechaFin {}", fechaInicio, fechaFin);
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin");
        }

        List<String> ids = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;

        // Reutilizar el método que ya devuelve órdenes por rango
        // (obtenerOrdenesEntreFechas)
        LocalDate dia = fechaInicio;
        while (!dia.isAfter(fechaFin)) {
            logger.info("📆 listarOrderIdsPorDias - consultando día {}", dia);
            try {
                List<Map<String, Object>> ordenesDelDia = obtenerOrdenesEntreFechas(dia, dia);
                logger.info("📦 {} órdenes obtenidas para {}", ordenesDelDia.size(), dia);

                for (Map<String, Object> orden : ordenesDelDia) {
                    Object id = orden.get("id");
                    if (id != null) {
                        ids.add(String.valueOf(id));
                    }
                }
            } catch (Exception e) {
                logger.error("❌ Error al obtener órdenes para {}: {}", dia, e.getMessage(), e);
            }

            try {
                Thread.sleep(150); // respeto rate limit
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            dia = dia.plusDays(1);
        }

        logger.info("🔚 listarOrderIdsPorDias completado - total IDs: {}", ids.size());
        if (ids.isEmpty())
            logger.warn("⚠️ listarOrderIdsPorDias devolvió lista vacía para rango {} → {}", fechaInicio, fechaFin);

        return ids;
    }
}