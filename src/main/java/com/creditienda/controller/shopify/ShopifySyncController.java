package com.creditienda.controller.shopify;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.RangoFechasDTO;
import com.creditienda.service.shopify.ShopifyProcesarOrderService;

@RestController
@RequestMapping("/api/shopify/secure/shopify")
public class ShopifySyncController {

    private static final Logger logger = LoggerFactory.getLogger(ShopifySyncController.class);

    private final ShopifyProcesarOrderService shopifyService;

    public ShopifySyncController(ShopifyProcesarOrderService shopifyService) {
        this.shopifyService = shopifyService;
    }

    @PostMapping("/sync-b2b-orders")
    public ResponseEntity<String> procesarOrdenesShopify(@RequestBody RangoFechasDTO fechas) {
        LocalDate inicio = fechas.getFechaInicio();
        LocalDate fin = fechas.getFechaFin();

        try {
            String resultado = shopifyService.procesarOrdenesEntreFechas(inicio, fin);
            return ResponseEntity.ok(resultado);

            // shopifyService.procesarUnaOrden("6658717155557");

            // shopifyService.procesarUnaOrden("6666054828261");

            // return ResponseEntity.ok("Se procesaron las órdenes correctamente.");
        } catch (Exception e) {
            logger.error("❌ Error general al procesar órdenes Shopify: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error general: " + e.getMessage());
        }
    }

    @PostMapping("/ordenes-ids-por-dias")
    public ResponseEntity<List<String>> obtenerOrdenesIdsPorDias(@RequestBody RangoFechasDTO fechas) {
        LocalDate inicio = fechas.getFechaInicio();
        LocalDate fin = fechas.getFechaFin();

        if (inicio == null || fin == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<String> ids = shopifyService.listarOrderIdsPorDias(inicio, fin);
            return ResponseEntity.ok(ids);
        } catch (Exception e) {
            logger.error("❌ Error al listar IDs por días: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}