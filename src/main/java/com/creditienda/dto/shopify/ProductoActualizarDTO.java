package com.creditienda.dto.shopify;

public class ProductoActualizarDTO {
    private String handle; // optional, to resolve the product
    private String variantId; // gid://shopify/ProductVariant/...
    private String precioStr;
    private String compareAtPriceStr;
    private Integer cantidad;
    private Double precio; // now Double
    private Double compareAtPrice;

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getPrecioStr() {
        return precioStr;
    }

    public void setPrecioStr(String precioStr) {
        this.precioStr = precioStr;
    }

    public String getCompareAtPriceStr() {
        return compareAtPriceStr;
    }

    public void setCompareAtPriceStr(String compareAtPriceStr) {
        this.compareAtPriceStr = compareAtPriceStr;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Double getCompareAtPrice() {
        return compareAtPrice;
    }

    public void setCompareAtPrice(Double compareAtPrice) {
        this.compareAtPrice = compareAtPrice;
    }
}