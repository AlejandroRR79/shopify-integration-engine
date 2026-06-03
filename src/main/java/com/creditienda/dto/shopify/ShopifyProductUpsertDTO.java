package com.creditienda.dto.shopify;

import java.util.List;

public class ShopifyProductUpsertDTO {

    // ===== PRODUCT =====
    private String handle;
    private String title;
    private String bodyHtml;
    private String vendor;
    private String productCategory;
    private String tags;
    private String status;

    // ===== VARIANT =====
    private VariantDTO variant;

    // ===== IMAGES =====
    private List<ImageDTO> images;

    // ===== INNER CLASSES =====
    public static class VariantDTO {
        private String sku;
        private double price;
        private double compareAtPrice;
        private int inventoryQty;
        private String inventoryPolicy;
        private String fulfillmentService;
        private boolean requiresShipping;
        private boolean taxable;
        private String barcode;
        private double weight;
        private String weightUnit;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public double getCompareAtPrice() {
            return compareAtPrice;
        }

        public void setCompareAtPrice(double compareAtPrice) {
            this.compareAtPrice = compareAtPrice;
        }

        public int getInventoryQty() {
            return inventoryQty;
        }

        public void setInventoryQty(int inventoryQty) {
            this.inventoryQty = inventoryQty;
        }

        public String getInventoryPolicy() {
            return inventoryPolicy;
        }

        public void setInventoryPolicy(String inventoryPolicy) {
            this.inventoryPolicy = inventoryPolicy;
        }

        public String getFulfillmentService() {
            return fulfillmentService;
        }

        public void setFulfillmentService(String fulfillmentService) {
            this.fulfillmentService = fulfillmentService;
        }

        public boolean isRequiresShipping() {
            return requiresShipping;
        }

        public void setRequiresShipping(boolean requiresShipping) {
            this.requiresShipping = requiresShipping;
        }

        public boolean isTaxable() {
            return taxable;
        }

        public void setTaxable(boolean taxable) {
            this.taxable = taxable;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public String getWeightUnit() {
            return weightUnit;
        }

        public void setWeightUnit(String weightUnit) {
            this.weightUnit = weightUnit;
        }
    }

    public static class ImageDTO {
        private String src;
        private int position;
        private String altText;

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public String getAltText() {
            return altText;
        }

        public void setAltText(String altText) {
            this.altText = altText;
        }
    }

    // ===== GETTERS / SETTERS =====
    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public VariantDTO getVariant() {
        return variant;
    }

    public void setVariant(VariantDTO variant) {
        this.variant = variant;
    }

    public List<ImageDTO> getImages() {
        return images;
    }

    public void setImages(List<ImageDTO> images) {
        this.images = images;
    }
}
