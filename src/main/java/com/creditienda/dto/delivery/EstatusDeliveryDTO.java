package com.creditienda.dto.delivery;

public class EstatusDeliveryDTO {

    private Integer idEstatusDelivery;
    private String descripcion;
    private String codigo;

    public Integer getIdEstatusDelivery() {
        return idEstatusDelivery;
    }

    public void setIdEstatusDelivery(Integer idEstatusDelivery) {
        this.idEstatusDelivery = idEstatusDelivery;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}