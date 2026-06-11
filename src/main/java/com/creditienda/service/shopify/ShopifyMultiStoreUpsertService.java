package com.creditienda.service.shopify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.config.ShopifyMultiStoreProperties;
import com.creditienda.config.ShopifyMultiStoreProperties.ShopifyStoreConfig;
import com.creditienda.dto.shopify.ShopifyProductUpsertDTO;

@Service
public class ShopifyMultiStoreUpsertService {

    private static final Logger log = LoggerFactory.getLogger(ShopifyMultiStoreUpsertService.class);

    private final ShopifyMultiStoreProperties multiStoreProperties;
    private final ShopifyTokenResolverService tokenResolver;
    private final RestTemplate restTemplate;
    private final Executor shopifyExecutor;

    public ShopifyMultiStoreUpsertService(
            ShopifyMultiStoreProperties multiStoreProperties,
            ShopifyTokenResolverService tokenResolver,
            RestTemplate restTemplate,
            @Qualifier("shopifyMultiStoreExecutor") Executor shopifyExecutor) {

        this.multiStoreProperties = multiStoreProperties;
        this.tokenResolver = tokenResolver;
        this.restTemplate = restTemplate;
        this.shopifyExecutor = shopifyExecutor;
    }

    // ================= API PUBLICA =================

    public Map<String, Map<String, Object>> upsert(ShopifyProductUpsertDTO dto) {

        List<ShopifyStoreConfig> stores = multiStoreProperties.getStores();
        Map<String, Map<String, Object>> conc = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = stores.stream()
                .map(store -> CompletableFuture.runAsync(() -> {
                    log.info("[MULTI-UPSERT] tienda={} handle={}", store.getAlias(), dto.getHandle());
                    Map<String, Object> resultado = new LinkedHashMap<>();
                    try {
                        String token = tokenResolver.resolverToken(store);
                        Map<String, Object> producto = buscarPorHandle(dto.getHandle(), store, token);

                        if (producto == null) {
                            crearProducto(dto, store, token);
                            resultado.put("accion", "CREADO");
                            log.info("[MULTI-UPSERT] CREADO tienda={} handle={}", store.getAlias(), dto.getHandle());
                        } else {
                            actualizarProductoYVariante(producto, dto, store, token);
                            resultado.put("accion", "ACTUALIZADO");
                            log.info("[MULTI-UPSERT] ACTUALIZADO tienda={} handle={}", store.getAlias(), dto.getHandle());
                        }

                        resultado.put("handle", dto.getHandle());
                        resultado.put("exito", true);
                        resultado.put("error", null);

                    } catch (Exception e) {
                        log.error("[MULTI-UPSERT] ERROR tienda={} handle={} msg={}",
                                store.getAlias(), dto.getHandle(), e.getMessage(), e);
                        resultado.put("accion", "ERROR");
                        resultado.put("handle", dto.getHandle());
                        resultado.put("exito", false);
                        resultado.put("error", e.getMessage());
                    }
                    conc.put(store.getDomain(), resultado);
                }, shopifyExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, Map<String, Object>> resultados = new LinkedHashMap<>();
        stores.forEach(s -> resultados.put(s.getDomain(), conc.get(s.getDomain())));
        return resultados;
    }

    // ================= BUSCAR =================

    @SuppressWarnings("unchecked")
    private Map<String, Object> buscarPorHandle(String handle, ShopifyStoreConfig store, String token) {

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://" + store.getDomain() + "/admin/api/" + store.getApiVersion()
                        + "/products.json?handle=" + handle,
                HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                Map.class);

        List<Map<String, Object>> products =
                (List<Map<String, Object>>) response.getBody().get("products");

        return (products == null || products.isEmpty()) ? null : products.get(0);
    }

    // ================= CREAR =================

    private void crearProducto(ShopifyProductUpsertDTO dto, ShopifyStoreConfig store, String token) {

        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> product = new HashMap<>();
        product.put("title", dto.getTitle());
        product.put("handle", dto.getHandle());
        product.put("body_html", dto.getBodyHtml());
        product.put("vendor", dto.getVendor());
        product.put("product_type", dto.getProductCategory());
        product.put("tags", dto.getTags());
        product.put("status", dto.getStatus());
        product.put("variants", List.of(buildVariantCreate(dto)));
        product.put("images", buildImages(dto));

        restTemplate.postForEntity(
                "https://" + store.getDomain() + "/admin/api/" + store.getApiVersion() + "/products.json",
                new HttpEntity<>(Map.of("product", product), headers),
                Map.class);
    }

    // ================= ACTUALIZAR =================

    @SuppressWarnings("unchecked")
    private void actualizarProductoYVariante(
            Map<String, Object> product,
            ShopifyProductUpsertDTO dto,
            ShopifyStoreConfig store,
            String token) {

        Long productId = Long.parseLong(product.get("id").toString());

        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> productUpdate = new HashMap<>();
        productUpdate.put("id", productId);
        productUpdate.put("title", dto.getTitle());
        productUpdate.put("body_html", dto.getBodyHtml());
        productUpdate.put("vendor", dto.getVendor());
        productUpdate.put("product_type", dto.getProductCategory());
        productUpdate.put("tags", dto.getTags());
        productUpdate.put("status", dto.getStatus());

        List<Map<String, Object>> images = buildImages(dto);
        if (!images.isEmpty()) {
            productUpdate.put("images", images);
        }

        restTemplate.exchange(
                "https://" + store.getDomain() + "/admin/api/" + store.getApiVersion()
                        + "/products/" + productId + ".json",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("product", productUpdate), headers),
                Map.class);

        actualizarVariante(product, dto, store, token);
    }

    // ================= VARIANTE =================

    @SuppressWarnings("unchecked")
    private void actualizarVariante(
            Map<String, Object> product,
            ShopifyProductUpsertDTO dto,
            ShopifyStoreConfig store,
            String token) {

        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");
        Long variantId = Long.parseLong(variants.get(0).get("id").toString());

        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> variant = new HashMap<>();
        variant.put("id", variantId);
        variant.put("price", dto.getVariant().getPrice());
        variant.put("compare_at_price", dto.getVariant().getCompareAtPrice());
        variant.put("inventory_policy", dto.getVariant().getInventoryPolicy());
        variant.put("weight", dto.getVariant().getWeight());
        variant.put("weight_unit", dto.getVariant().getWeightUnit());
        variant.put("barcode", dto.getVariant().getBarcode());
        variant.put("requires_shipping", dto.getVariant().isRequiresShipping());
        variant.put("taxable", dto.getVariant().isTaxable());

        restTemplate.exchange(
                "https://" + store.getDomain() + "/admin/api/" + store.getApiVersion()
                        + "/variants/" + variantId + ".json",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("variant", variant), headers),
                Map.class);
    }

    // ================= HELPERS =================

    private Map<String, Object> buildVariantCreate(ShopifyProductUpsertDTO dto) {
        Map<String, Object> variant = new HashMap<>();
        variant.put("sku", dto.getVariant().getSku());
        variant.put("price", dto.getVariant().getPrice());
        variant.put("compare_at_price", dto.getVariant().getCompareAtPrice());
        variant.put("inventory_management", "shopify");
        variant.put("inventory_quantity", dto.getVariant().getInventoryQty());
        variant.put("inventory_policy", dto.getVariant().getInventoryPolicy());
        variant.put("fulfillment_service", dto.getVariant().getFulfillmentService());
        variant.put("requires_shipping", dto.getVariant().isRequiresShipping());
        variant.put("taxable", dto.getVariant().isTaxable());
        variant.put("barcode", dto.getVariant().getBarcode());
        variant.put("weight", dto.getVariant().getWeight());
        variant.put("weight_unit", dto.getVariant().getWeightUnit());
        return variant;
    }

    private List<Map<String, Object>> buildImages(ShopifyProductUpsertDTO dto) {
        List<Map<String, Object>> images = new ArrayList<>();
        if (dto.getImages() != null) {
            dto.getImages().forEach(img -> {
                Map<String, Object> image = new HashMap<>();
                image.put("src", img.getSrc());
                image.put("position", img.getPosition());
                image.put("alt", img.getAltText());
                images.add(image);
            });
        }
        return images;
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Shopify-Access-Token", token);
        return h;
    }
}
