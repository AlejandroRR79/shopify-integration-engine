package com.creditienda.dto.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class B2BActualizarEstatusEntregaDTO {

    private static final Logger log = LoggerFactory.getLogger(B2BActualizarEstatusEntregaDTO.class);

    private String orderNumber;
    private String referenceNumber;
    private String trackingCode;
    private String codigoEntrega;
    private String descripcionEntrega;
    private String fechaEstatus;
    // 🔥 NUEVO CAMPO
    private String reasonCodeDescription;

    public String getReasonCodeDescription() {
        return reasonCodeDescription;
    }

    public void setReasonCodeDescription(String reasonCodeDescription) {
        this.reasonCodeDescription = reasonCodeDescription;
    }

    public B2BActualizarEstatusEntregaDTO() {
        log.debug("🧩 B2BActualizarEstatusEntregaDTO creada");
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        log.debug("➡ setOrderNumber={}", orderNumber);
        this.orderNumber = orderNumber;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        log.debug("➡ setReferenceNumber={}", referenceNumber);
        this.referenceNumber = referenceNumber;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        log.debug("➡ setTrackingCode={}", trackingCode);
        this.trackingCode = trackingCode;
    }

    public String getCodigoEntrega() {
        return codigoEntrega;
    }

    public void setCodigoEntrega(String codigoEntrega) {
        log.debug("➡ setCodigoEntrega={}", codigoEntrega);
        this.codigoEntrega = codigoEntrega;
    }

    public String getDescripcionEntrega() {
        return descripcionEntrega;
    }

    public void setDescripcionEntrega(String descripcionEntrega) {
        log.debug("➡ setDescripcionEntrega={}", descripcionEntrega);
        this.descripcionEntrega = descripcionEntrega;
    }

    public String getFechaEstatus() {
        return fechaEstatus;
    }

    public void setFechaEstatus(String fechaEstatus) {
        log.debug("➡ setFechaEstatus={}", fechaEstatus);
        this.fechaEstatus = fechaEstatus;
    }

    @Override
    public String toString() {
        return "B2BActualizarEstatusEntregaDTO{" +
                "orderNumber='" + orderNumber + '\'' +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", trackingCode='" + trackingCode + '\'' +
                ", codigoEntrega='" + codigoEntrega + '\'' +
                ", descripcionEntrega='" + descripcionEntrega + '\'' +
                ", fechaEstatus='" + fechaEstatus + '\'' +
                ", reasonCodeDescription='" + reasonCodeDescription + '\'' +
                '}';
    }
}
