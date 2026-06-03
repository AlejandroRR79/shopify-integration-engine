package com.creditienda.dto.delivery;

public class FacturacionResponse {

    private boolean success;
    private String nombreB2B;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getNombreB2B() {
        return nombreB2B;
    }

    public void setNombreB2B(String nombreB2B) {
        this.nombreB2B = nombreB2B;
    }
}