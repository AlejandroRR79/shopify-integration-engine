package com.creditienda.service.shopify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.shopify.ProductoActualizarDTO;
import com.creditienda.dto.shopify.RespuestaActualizacionDTO;
import com.creditienda.dto.shopify.RespuestaLoteDTO;
import com.creditienda.service.notificacion.NotificacionService;

@Service
public class ShopifyActualizarProductoService {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyActualizarProductoService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final NotificacionService notificacionService;

    private String cachedLocationId = null;

    public ShopifyActualizarProductoService(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Obtiene y cachea el locationId */
    private String obtenerLocationId() {
        if (cachedLocationId != null)
            return cachedLocationId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                String.format("https://%s/admin/api/%s/locations.json", shopDomain, apiVersion),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        List<Map<String, Object>> locations = (List<Map<String, Object>>) response.getBody().get("locations");
        if (locations != null && !locations.isEmpty()) {
            cachedLocationId = locations.get(0).get("id").toString();
            logger.info("📍 LocationId obtenido: {}", cachedLocationId);
        }
        return cachedLocationId;
    }

    /** Obtiene variantId e inventoryItemId por handle */
    private Map<String, String> obtenerIdsPorHandle(String handle) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                String.format("https://%s/admin/api/%s/products.json?handle=%s", shopDomain, apiVersion, handle),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        List<Map<String, Object>> products = (List<Map<String, Object>>) response.getBody().get("products");
        if (products == null || products.isEmpty()) {
            throw new RuntimeException("Producto no encontrado para handle: " + handle);
        }

        Map<String, Object> product = products.get(0);
        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        Map<String, Object> variant = variants.get(0);

        String variantId = variant.get("id").toString();
        String inventoryItemId = variant.get("inventory_item_id").toString();

        return Map.of("variantId", variantId, "inventoryItemId", inventoryItemId);
    }

    /** Ajusta inventario vía REST */
    private boolean ajustarInventarioREST(String inventoryItemId, String locationId, int cantidad) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "inventory_item_id", Long.parseLong(inventoryItemId),
                    "location_id", Long.parseLong(locationId),
                    "available", cantidad // 👈 ahora se manda el valor absoluto
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    String.format("https://%s/admin/api/%s/inventory_levels/set.json", shopDomain, apiVersion),
                    new HttpEntity<>(body, headers),
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Inventario establecido en {} para item {}", cantidad, inventoryItemId);
                return true;
            } else {
                logger.error("❌ Error REST inventario: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error REST inventario: {}", e.getMessage());
            return false;
        }
    }

    /** Actualiza precio vía REST */
    /** Actualiza precio y compare_at_price vía REST */
    private boolean actualizarPrecioREST(String variantId, double precio, double compareAtPrice) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> variant = new HashMap<>();
            variant.put("id", Long.parseLong(variantId));
            variant.put("price", String.valueOf(precio));
            variant.put("compare_at_price", String.valueOf(compareAtPrice)); // 👈 nuevo campo

            Map<String, Object> body = Map.of("variant", variant);

            ResponseEntity<Map> response = restTemplate.exchange(
                    String.format("https://%s/admin/api/%s/variants/%s.json", shopDomain, apiVersion, variantId),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Precio actualizado a {} y compare_at_price a {} para variant {}", precio, compareAtPrice,
                        variantId);
                return true;
            } else {
                logger.error("❌ Error REST precio: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error REST precio: {}", e.getMessage());
            return false;
        }
    }

    /** Actualiza un solo producto */
    public RespuestaActualizacionDTO actualizarProductoIndividual(ProductoActualizarDTO dto) {
        RespuestaLoteDTO lote = actualizarProductosLote(List.of(dto));
        boolean exito = lote.getExitosos().contains(dto.getHandle());
        RespuestaActualizacionDTO respuesta = new RespuestaActualizacionDTO(exito, dto.getHandle(), lote.getResumen());

        String correo = generarCorreoResumen(lote);

        /*
         * if (exito)
         * notificacionService.enviarConfirmacion(correo);
         * else
         * notificacionService.enviarError(correo);
         */
        return respuesta;
    }

    /** Actualiza múltiples productos */
    public RespuestaLoteDTO actualizarProductosLote(List<ProductoActualizarDTO> productos) {
        String locationId = obtenerLocationId();
        List<String> exitosos = new ArrayList<>();
        List<String> fallidos = new ArrayList<>();

        for (ProductoActualizarDTO dto : productos) {
            try {
                Map<String, String> ids = obtenerIdsPorHandle(dto.getHandle());
                String variantId = ids.get("variantId");
                String inventoryItemId = ids.get("inventoryItemId");

                boolean inventarioOk = ajustarInventarioREST(inventoryItemId, locationId, dto.getCantidad());
                boolean precioOk = actualizarPrecioREST(variantId, dto.getPrecio(), dto.getCompareAtPrice());

                if (inventarioOk && precioOk) {
                    exitosos.add(dto.getHandle());
                } else {
                    fallidos.add(dto.getHandle());
                }

                Thread.sleep(200); // pausa para evitar rate limit

            } catch (Exception e) {
                fallidos.add(dto.getHandle());
                logger.error("❌ Error al actualizar {}: {}", dto.getHandle(), e.getMessage());
            }
        }

        String resumen = String.format("✅ %d actualizados / ❌ %d con error", exitosos.size(), fallidos.size());
        RespuestaLoteDTO lote = new RespuestaLoteDTO(exitosos, fallidos, productos.size(), resumen);

        String correo = generarCorreoResumen(lote);

        /*
         * if (fallidos.isEmpty())
         * notificacionService.enviarResumen(correo);
         * else
         * notificacionService.enviarError(correo);
         */
        return lote;
    }

    /** Genera correo resumen */
    private String generarCorreoResumen(RespuestaLoteDTO lote) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimado equipo,\n\n");
        sb.append("Se ha ejecutado la actualización masiva de productos en Shopify.\n\n");
        sb.append("✔️ Actualizados correctamente: ").append(lote.getExitosos().size()).append("\n");
        for (String h : lote.getExitosos())
            sb.append("   - ").append(h).append("\n");
        sb.append("\n❌ Con error: ").append(lote.getFallidos().size()).append("\n");
        for (String h : lote.getFallidos())
            sb.append("   - ").append(h).append("\n");
        sb.append("\nResumen: ").append(lote.getResumen()).append("\n\n");
        sb.append("Saludos,\nSistema de Integración Shopify\n");
        return sb.toString();
    }
}