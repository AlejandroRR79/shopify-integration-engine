package com.creditienda.dto.shopify;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductosLoteRequestDTO {

    @JsonProperty("productos")
    private List<ProductoActualizarDTO> productos;

    public ProductosLoteRequestDTO() {
    }

    public ProductosLoteRequestDTO(List<ProductoActualizarDTO> productos) {
        this.productos = productos;
    }

    public List<ProductoActualizarDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoActualizarDTO> productos) {
        this.productos = productos;
    }
}