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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Multi-Tienda", description = "Operaciones bulk y upsert sobre multiples tiendas Shopify en paralelo. Requiere JWT.")
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

    @Operation(summary = "Bulk precio+inventario (mismo lote todas las tiendas)", description = "Actualiza precio e inventario del mismo lote de productos en todas las tiendas configuradas.")
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

    @Operation(summary = "Bulk precio+inventario por tienda", description = "Actualiza precio e inventario con listas distintas por tienda. Body: {alias: [{handle, precio, cantidad}]}. Tiendas con update-price=false solo actualizan inventario.")
    @PostMapping("/actualizar-lote-por-tienda")
    public ResponseEntity<RespuestaMultiTiendaDTO> actualizarLoteBulkPorTienda(
            @RequestBody Map<String, List<ProductoActualizarDTO>> productosPorAlias) {

        if (productosPorAlias == null || productosPorAlias.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RespuestaMultiTiendaDTO(null, null));
        }

        log.info("[MULTI-STORE] BULK precio+inventario por tienda | tiendas={}",
                productosPorAlias.size());

        return ResponseEntity.ok(
                multiStoreBulkService.actualizarProductosBulkPorTienda(productosPorAlias));
    }

    // =============================
    // 2️⃣ SOLO PRECIO
    // =============================

    @Operation(summary = "Bulk solo precio (todas las tiendas)", description = "Actualiza solo precio en todas las tiendas con update-price=true.")
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

    @Operation(summary = "Bulk solo inventario (todas las tiendas)", description = "Actualiza solo inventario en todas las tiendas configuradas.")
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

    @Operation(summary = "Upsert mismo producto en todas las tiendas", description = "Crea o actualiza el mismo producto en todas las tiendas configuradas en paralelo.")
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

    @Operation(summary = "Upsert producto distinto por tienda", description = "Crea o actualiza un producto diferente por tienda. Body: {alias: ShopifyProductUpsertDTO}. Solo procesa tiendas presentes en el body.")
    @PostMapping("/upsert-by-store")
    public ResponseEntity<Map<String, Map<String, Object>>> upsertPorTienda(
            @RequestBody Map<String, ShopifyProductUpsertDTO> productoPorAlias) {

        if (productoPorAlias == null || productoPorAlias.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[MULTI-STORE] UPSERT por tienda | tiendas={}", productoPorAlias.size());

        return ResponseEntity.ok(
                multiStoreUpsertService.upsertPorTienda(productoPorAlias));
    }

    // =============================
    // 5️⃣ SYNC OC POR RANGO DE FECHAS
    // =============================

    @Operation(summary = "Sync ordenes B2B por rango de fechas", description = "Sincroniza ordenes Shopify hacia B2B en el rango indicado. Campo tienda = domain de la tienda.")
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
                        request.getTienda(), request.getFechaInicio(), request.getFechaFin(), request.isSoloConsulta()));
    }

    // =============================
    // 6️⃣ SYNC UNA OC POR ID
    // =============================

    @Operation(summary = "Sync una orden B2B por ID", description = "Sincroniza una orden especifica de Shopify hacia B2B. Usar soloConsulta=true para ver la orden sin enviarla.")
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
                        request.getTienda(), request.getOrdenId(), request.isSoloConsulta()));
    }
}
