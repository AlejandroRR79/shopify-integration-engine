package com.creditienda.service.shopify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.shopify.ShopifyProductUpsertDTO;
import com.creditienda.exception.ShopifyException;

@Service
public class ShopifyUpsertProductoService {

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    @Autowired
    private RestTemplate restTemplate;

    // ================= PUBLIC =================

    public String upsert(ShopifyProductUpsertDTO dto) {

        try {
            Map<String, Object> product = buscarPorHandle(dto.getHandle());

            if (product == null) {
                crearProducto(dto);
                return "CREADO: " + dto.getHandle();
            } else {
                actualizarProductoYVariante(product, dto);
                return "ACTUALIZADO: " + dto.getHandle();
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ShopifyException(
                    "SHOPIFY_ERROR",
                    "Error al procesar producto en Shopify",
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ShopifyException(
                    "INTERNAL_ERROR",
                    "Error interno al procesar producto",
                    e.getMessage());
        }
    }

    // ================= BUSCAR =================

    private Map<String, Object> buscarPorHandle(String handle) {

        HttpHeaders headers = headers();

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://" + shopDomain + "/admin/api/" + apiVersion
                        + "/products.json?handle=" + handle,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        List<Map<String, Object>> products = (List<Map<String, Object>>) response.getBody().get("products");

        return (products == null || products.isEmpty()) ? null : products.get(0);
    }

    // ================= CREAR =================

    private void crearProducto(ShopifyProductUpsertDTO dto) {

        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // -------- Variant --------
        Map<String, Object> variant = buildVariantCreate(dto);

        // -------- Images --------
        List<Map<String, Object>> images = buildImages(dto);

        // -------- Product --------
        Map<String, Object> product = new HashMap<>();
        product.put("title", dto.getTitle());
        product.put("handle", dto.getHandle());
        product.put("body_html", dto.getBodyHtml());
        product.put("vendor", dto.getVendor());
        product.put("product_type", dto.getProductCategory());
        product.put("tags", dto.getTags());
        product.put("status", dto.getStatus());
        product.put("variants", List.of(variant));
        product.put("images", images);

        Map<String, Object> body = new HashMap<>();
        body.put("product", product);

        restTemplate.postForEntity(
                "https://" + shopDomain + "/admin/api/" + apiVersion + "/products.json",
                new HttpEntity<>(body, headers),
                Map.class);
    }

    // ================= ACTUALIZAR =================

    private void actualizarProductoYVariante(
            Map<String, Object> product,
            ShopifyProductUpsertDTO dto) {

        Long productId = Long.parseLong(product.get("id").toString());

        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // -------- UPDATE PRODUCT --------
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

        Map<String, Object> productBody = new HashMap<>();
        productBody.put("product", productUpdate);

        restTemplate.exchange(
                "https://" + shopDomain + "/admin/api/" + apiVersion
                        + "/products/" + productId + ".json",
                HttpMethod.PUT,
                new HttpEntity<>(productBody, headers),
                Map.class);

        // -------- UPDATE VARIANT --------
        actualizarVariante(product, dto);
    }

    // ================= VARIANT =================

    private void actualizarVariante(
            Map<String, Object> product,
            ShopifyProductUpsertDTO dto) {

        List<Map<String, Object>> variants = (List<Map<String, Object>>) product.get("variants");

        Long variantId = Long.parseLong(variants.get(0).get("id").toString());

        HttpHeaders headers = headers();
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

        Map<String, Object> body = new HashMap<>();
        body.put("variant", variant);

        restTemplate.exchange(
                "https://" + shopDomain + "/admin/api/" + apiVersion
                        + "/variants/" + variantId + ".json",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
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

    // ================= HEADERS =================

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Shopify-Access-Token", accessToken);
        return h;
    }
}
