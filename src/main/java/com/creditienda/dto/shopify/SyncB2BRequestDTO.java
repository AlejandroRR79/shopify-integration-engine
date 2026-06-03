package com.creditienda.dto.shopify;

import java.time.LocalDate;

public class SyncB2BRequestDTO {

    /** Alias de la tienda configurada en shopify.stores[*].alias */
    private String tienda;

    /** Para sync por rango de fechas */
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    /** Para sync de una sola orden */
    private String ordenId;

    public String getTienda() {
        return tienda;
    }

    public void setTienda(String tienda) {
        this.tienda = tienda;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }
}
