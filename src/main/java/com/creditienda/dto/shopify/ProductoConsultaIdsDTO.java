package com.creditienda.dto.shopify;

public class ProductoConsultaIdsDTO {
    private String variantId;
    private String inventoryItemId;

    public ProductoConsultaIdsDTO() {
    }

    public ProductoConsultaIdsDTO(String variantId, String inventoryItemId) {
        this.variantId = variantId;
        this.inventoryItemId = inventoryItemId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
}
