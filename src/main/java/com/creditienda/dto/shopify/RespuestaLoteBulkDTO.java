package com.creditienda.dto.shopify;

import java.util.List;

public class RespuestaLoteBulkDTO {

    private List<String> exitosos;
    private List<String> fallidosPrecio;
    private List<String> fallidosInventario;
    private int total;
    private String resumen;

    public List<String> getExitosos() {
        return exitosos;
    }

    public void setExitosos(List<String> exitosos) {
        this.exitosos = exitosos;
    }

    public List<String> getFallidosPrecio() {
        return fallidosPrecio;
    }

    public void setFallidosPrecio(List<String> fallidosPrecio) {
        this.fallidosPrecio = fallidosPrecio;
    }

    public List<String> getFallidosInventario() {
        return fallidosInventario;
    }

    public void setFallidosInventario(List<String> fallidosInventario) {
        this.fallidosInventario = fallidosInventario;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public RespuestaLoteBulkDTO(
            List<String> exitosos,
            List<String> fallidosPrecio,
            List<String> fallidosInventario,
            int total,
            String resumen) {

        this.exitosos = exitosos;
        this.fallidosPrecio = fallidosPrecio;
        this.fallidosInventario = fallidosInventario;
        this.total = total;
        this.resumen = resumen;
    }

    // getters
}
