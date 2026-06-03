package com.creditienda.dto.shopify;

public class RespuestaActualizacionDTO {
    private boolean exito;
    private String handle;
    private String mensaje;

    public RespuestaActualizacionDTO() {
    }

    public RespuestaActualizacionDTO(boolean exito, String handle, String mensaje) {
        this.exito = exito;
        this.handle = handle;
        this.mensaje = mensaje;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}