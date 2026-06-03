package com.creditienda.controller.shopify;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.ProductosLoteRequestDTO;
import com.creditienda.dto.shopify.RespuestaMultiTiendaDTO;
import com.creditienda.dto.shopify.ShopifyProductUpsertDTO;
import com.creditienda.dto.shopify.SyncB2BRequestDTO;
import com.creditienda.service.shopify.ShopifyMultiStoreBulkService;
import com.creditienda.service.shopify.ShopifyMultiStoreSyncService;
import com.creditienda.service.shopify.ShopifyMultiStoreUpsertService;

@RestController
@RequestMapping("/api/shopify/secure/multi")
public class ShopifyMultiStoreBulkController {

    private static final Logger log = LoggerFactory.getLogger(ShopifyMultiStoreBulkController.class);

    private final ShopifyMultiStoreBulkService multiStoreBulkService;
    private final ShopifyMultiStoreUpsertService multiStoreUpsertService;
    private final ShopifyMultiStoreSyncService multiStoreSyncService;

    public ShopifyMultiStoreBulkController(
            ShopifyMultiStoreBulkService multiStoreBulkService,
            ShopifyMultiStoreUpsertService multiStoreUpsertService,
            ShopifyMultiStoreSyncService multiStoreSyncService) {
        this.multiStoreBulkService = multiStoreBulkService;
        this.multiStoreUpsertService = multiStoreUpsertService;
        this.multiStoreSyncService = multiStoreSyncService;
    }

    // =============================
    // 1️⃣ PRECIO + INVENTARIO
    // =============================

    @PostMapping("/actualizar-lote-bulk")
    public ResponseEntity<RespuestaMultiTiendaDTO> actualizarLoteBulk(
            @RequestBody ProductosLoteRequestDTO request) {

        if (request == null
                || request.getProductos() == null
                || request.getProductos().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(new RespuestaMultiTiendaDTO(
                            null,
                            null));
        }

        log.info("[MULTI-STORE] BULK precio+inventario | productos={}",
                request.getProductos().size());

        return ResponseEntity.ok(
                multiStoreBulkService.actualizarProductosBulk(
                        request.getProductos()));
    }

    // =============================
    // 2️⃣ SOLO PRECIO
    // =============================

    @PostMapping("/bulk/precio")
    public ResponseEntity<RespuestaMultiTiendaDTO> bulkPrecio(
            @RequestBody List<ProductoActualizarDTO> productos) {

        if (productos == null || productos.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RespuestaMultiTiendaDTO(null, null));
        }

        log.info("[MULTI-STORE] BULK precio | productos={}", productos.size());

        return ResponseEntity.ok(
                multiStoreBulkService.actualizarPreciosBulk(productos));
    }

    // =============================
    // 3️⃣ SOLO INVENTARIO
    // =============================

    @PostMapping("/bulk/inventario")
    public ResponseEntity<RespuestaMultiTiendaDTO> bulkInventario(
            @RequestBody List<ProductoActualizarDTO> productos) {

        if (productos == null || productos.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RespuestaMultiTiendaDTO(null, null));
        }

        log.info("[MULTI-STORE] BULK inventario | productos={}", productos.size());

        return ResponseEntity.ok(
                multiStoreBulkService.actualizarInventarioBulk(productos));
    }

    // =============================
    // 4️⃣ UPSERT (crear o actualizar producto)
    // =============================

    @PostMapping("/upsert")
    public ResponseEntity<Map<String, Map<String, Object>>> upsert(
            @RequestBody ShopifyProductUpsertDTO dto) {

        if (dto == null || dto.getHandle() == null || dto.getHandle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[MULTI-STORE] UPSERT handle={}", dto.getHandle());

        return ResponseEntity.ok(
                multiStoreUpsertService.upsert(dto));
    }

    // =============================
    // 5️⃣ SYNC OC POR RANGO DE FECHAS
    // =============================

    @PostMapping("/sync-b2b-orders")
    public ResponseEntity<Map<String, Object>> syncB2BOrders(
            @RequestBody SyncB2BRequestDTO request) {

        if (request == null || request.getTienda() == null || request.getTienda().isBlank()
                || request.getFechaInicio() == null || request.getFechaFin() == null) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[MULTI-SYNC] sync ordenes tienda={} rango={} a {}",
                request.getTienda(), request.getFechaInicio(), request.getFechaFin());

        return ResponseEntity.ok(
                multiStoreSyncService.sincronizarPorFechas(
                        request.getTienda(), request.getFechaInicio(), request.getFechaFin()));
    }

    // =============================
    // 6️⃣ SYNC UNA OC POR ID
    // =============================

    @PostMapping("/sync-b2b-orden")
    public ResponseEntity<Map<String, Object>> syncB2BOrden(
            @RequestBody SyncB2BRequestDTO request) {

        if (request == null || request.getTienda() == null || request.getTienda().isBlank()
                || request.getOrdenId() == null || request.getOrdenId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[MULTI-SYNC] sync orden unica tienda={} ordenId={}",
                request.getTienda(), request.getOrdenId());

        return ResponseEntity.ok(
                multiStoreSyncService.sincronizarUnaOrden(
                        request.getTienda(), request.getOrdenId()));
    }
}
