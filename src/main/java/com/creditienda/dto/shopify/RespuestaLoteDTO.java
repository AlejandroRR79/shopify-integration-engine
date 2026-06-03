package com.creditienda.dto.shopify;

import java.util.List;

public class RespuestaLoteDTO {
    private List<String> exitosos;
    private List<String> fallidos;
    private int total;
    private String resumen;

    public RespuestaLoteDTO() {
    }

    public RespuestaLoteDTO(List<String> exitosos, List<String> fallidos, int total, String resumen) {
        this.exitosos = exitosos;
        this.fallidos = fallidos;
        this.total = total;
        this.resumen = resumen;
    }

    public List<String> getExitosos() {
        return exitosos;
    }

    public void setExitosos(List<String> exitosos) {
        this.exitosos = exitosos;
    }

    public List<String> getFallidos() {
        return fallidos;
    }

    public void setFallidos(List<String> fallidos) {
        this.fallidos = fallidos;
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
}