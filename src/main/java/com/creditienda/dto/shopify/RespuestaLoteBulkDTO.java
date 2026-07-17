package com.creditienda.dto.shopify;

import java.util.List;
import java.util.Map;

public class RespuestaLoteBulkDTO {

    private List<String> exitosos;
    private List<String> fallidosPrecio;
    private List<String> fallidosInventario;
    private int total;
    private String resumen;
    private Map<String, String> erroresPrecio;
    private Map<String, String> erroresInventario;
    private String nombreTienda;

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
        this(exitosos, fallidosPrecio, fallidosInventario, total, resumen, Map.of(), Map.of());
    }

    public RespuestaLoteBulkDTO(
            List<String> exitosos,
            List<String> fallidosPrecio,
            List<String> fallidosInventario,
            int total,
            String resumen,
            Map<String, String> erroresPrecio,
            Map<String, String> erroresInventario) {

        this.exitosos = exitosos;
        this.fallidosPrecio = fallidosPrecio;
        this.fallidosInventario = fallidosInventario;
        this.total = total;
        this.resumen = resumen;
        this.erroresPrecio = erroresPrecio;
        this.erroresInventario = erroresInventario;
    }

    public Map<String, String> getErroresPrecio() { return erroresPrecio; }
    public void setErroresPrecio(Map<String, String> erroresPrecio) { this.erroresPrecio = erroresPrecio; }

    public Map<String, String> getErroresInventario() { return erroresInventario; }
    public void setErroresInventario(Map<String, String> erroresInventario) { this.erroresInventario = erroresInventario; }

    public String getNombreTienda() { return nombreTienda; }
    public void setNombreTienda(String nombreTienda) { this.nombreTienda = nombreTienda; }

    // getters
}
