package com.creditienda.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.UnprocessableEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.creditienda.dto.ImageDTO;
import com.creditienda.dto.OptionDTO;
import com.creditienda.dto.ProductoShopifyDTO;
import com.creditienda.dto.VariantDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ShopifyProductService {

    private static final Logger logger = LogManager.getLogger(ShopifyProductService.class);

    @Value("${shopify.shop.domain}")
    private String shopDomain;

    @Value("${shopify.api.version}")
    private String apiVersion;

    @Value("${shopify.access.token}")
    private String accessToken;

    @Autowired
    private RestTemplate restTemplate;

    // ✅ Método original
    public Map<String, Object> obtenerProductos() {
        String url = String.format("https://%s/admin/api/%s/products.json", shopDomain, apiVersion);
        logger.info("🔗 Consultando productos desde Shopify: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("❌ Error al consultar productos: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "No se pudo obtener el listado de productos desde Shopify.");
        }
    }

    public Map<String, Object> registrarProducto(ProductoShopifyDTO dto) {
        String baseUrl = String.format("https://%s/admin/api/%s/products", shopDomain, apiVersion);

        logger.info("🔗 Url crear productos desde Shopify: {}", baseUrl);
        Long productId = dto.getProductId(); // viene del JSON

        Map<String, Object> product = new HashMap<>();
        product.put("handle", dto.getHandle());
        product.put("title", dto.getTitle());
        product.put("body_html", dto.getBodyHtml());
        product.put("vendor", dto.getVendor());
        product.put("tags", dto.getTags());
        product.put("published", dto.getPublished());
        product.put("options", buildOptions(dto.getOptions()));
        product.put("variants", buildVariants(dto.getVariants()));
        product.put("images", buildImages(dto.getImages()));
        product.put("status", dto.getStatus());
        product.put("product_type", dto.getProductType());
        product.put("product_Category", dto.getProductType());

        // ✅ Aquí insertas el bloque para la categoría estándar
        Map<String, String> taxonomy = dto.getStandardizedProductType();
        if (taxonomy != null && taxonomy.containsKey("product_taxonomy_node_id")) {
            String gid = taxonomy.get("product_taxonomy_node_id");
            if (gid != null && !gid.isBlank()) {
                product.put("standardized_product_type", taxonomy);
            }
        }

        Map<String, Object> payload = Map.of("product", product);

        // ✅ Imprimir JSON enviado a Shopify
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            logger.info("📦 JSON enviado a Shopify:\n{}", jsonPayload);
        } catch (Exception ex) {
            logger.warn("⚠️ No se pudo imprimir el JSON del payload: {}", ex.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            logger.info("productid {} antes de validar", productId);
            if (productId != null && existeProductoPorId(productId)) {
                String updateUrl = baseUrl + "/" + productId + ".json";
                restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
                logger.info("✏️ Producto actualizado en Shopify con ID: {}", productId);
                return Map.of(
                        "sku", obtenerSkuSeguro(dto),
                        "id", productId);
            } else {
                ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + ".json", entity, Map.class);
                Map<String, Object> body = response.getBody();
                Map<String, Object> createdProduct = (Map<String, Object>) body.get("product");
                Long newId = ((Number) createdProduct.get("id")).longValue();
                logger.info("🆕 Producto creado en Shopify con ID: {}", newId);

                // ✅ Reutiliza la variable taxonomy que ya existe arriba
                if (taxonomy != null && taxonomy.containsKey("product_taxonomy_node_id")) {
                    String gid = taxonomy.get("product_taxonomy_node_id");
                    if (gid != null && !gid.isBlank()) {
                        actualizarCategoriaViaGraphQL(newId, gid);
                    }
                }

                return Map.of(
                        "sku", obtenerSkuSeguro(dto),
                        "id", newId);
            }

        } catch (UnprocessableEntity e) {
            String detalle = e.getResponseBodyAsString();
            logger.error("❌ Error 422 de Shopify: {}", detalle);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Shopify rechazó el producto: " + detalle);
        } catch (Exception e) {
            logger.error("❌ Error inesperado al registrar producto: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al registrar producto");
        }
    }

    public void actualizarCategoriaViaGraphQL(Long productId, String taxonomyGid) {
        String graphqlUrl = String.format("https://%s/admin/api/%s/graphql.json", shopDomain, apiVersion);

        String graphqlQuery = String.format(
                """
                        {
                          "query": "mutation { productUpdate(input: {id: \\\"gid://shopify/Product/%d\\\", standardizedProductTypeId: \\\"%s\\\"}) { product { id standardizedProductType { productTaxonomyNode { id name fullName } } } } }"
                        }
                        """,
                productId, taxonomyGid);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);

        HttpEntity<String> entity = new HttpEntity<>(graphqlQuery, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(graphqlUrl, entity, String.class);
            logger.info("📦 GraphQL respuesta: {}", response.getBody());
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo actualizar categoría vía GraphQL: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildMetafield(String key, String value) {
        Map<String, Object> metafield = new HashMap<>();
        metafield.put("namespace", "shopify");
        metafield.put("key", key);
        metafield.put("value", value);
        metafield.put("type", "single_line_text_field");
        return metafield;
    }

    private List<Map<String, Object>> buildOptions(List<OptionDTO> options) {
        return options == null ? List.of()
                : options.stream()
                        .map(opt -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", opt.getName());
                            return map;
                        })
                        .toList();
    }

    private List<Map<String, Object>> buildVariants(List<VariantDTO> variants) {
        return variants == null ? List.of()
                : variants.stream()
                        .map(var -> {
                            Map<String, Object> variant = new HashMap<>();
                            variant.put("sku", var.getSku());
                            variant.put("grams", var.getGrams());
                            variant.put("inventory_management", var.getInventoryTracker());
                            variant.put("inventory_quantity", var.getInventoryQty());
                            variant.put("inventory_policy", var.getInventoryPolicy());
                            variant.put("fulfillment_service", var.getFulfillmentService());
                            variant.put("price", var.getPrice());
                            variant.put("compare_at_price", var.getCompareAtPrice());
                            variant.put("requires_shipping", var.getRequiresShipping());
                            variant.put("taxable", var.getTaxable());
                            variant.put("barcode", var.getBarcode());
                            variant.put("weight_unit", var.getWeightUnit());
                            variant.put("tax_code", var.getTaxCode());
                            variant.put("cost", var.getCostPerItem());
                            variant.put("inventory_policy", "deny"); // o "continue"
                            return variant;
                        })
                        .toList();
    }

    private List<Map<String, Object>> buildImages(List<ImageDTO> images) {
        return images == null ? List.of()
                : images.stream()
                        .map(img -> {
                            Map<String, Object> image = new HashMap<>();
                            image.put("src", img.getSrc());
                            image.put("position", img.getPosition());
                            image.put("alt", img.getAltText());
                            return image;
                        })
                        .toList();
    }

    private boolean existeProductoPorId(Long productId) {
        String url = String.format("https://%s/admin/api/%s/products/%d.json", shopDomain, apiVersion, productId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            logger.info("🔍 Producto con ID {} existe en Shopify", productId);
            return true;
        } catch (Exception e) {
            logger.warn("⚠️ Producto con ID {} no existe en Shopify", productId);
            return false;
        }
    }

    private String obtenerSkuSeguro(ProductoShopifyDTO dto) {
        return (dto.getVariants() != null && !dto.getVariants().isEmpty())
                ? dto.getVariants().get(0).getSku()
                : "SKU_NO_DEFINIDO";
    }

}