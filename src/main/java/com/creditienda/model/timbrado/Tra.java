package com.creditienda.model.timbrado;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)

public class Tra {

    @XmlAttribute
    @JsonProperty("impuesto")
    private String impuesto;

    @XmlAttribute
    @JsonProperty("tipoFactor")
    private String tipoFactor;

    @XmlAttribute
    @JsonProperty("tasaCuota")
    private BigDecimal tasaCuota;

    @XmlAttribute
    @JsonProperty("importe")
    private BigDecimal importe;

    @XmlAttribute
    @JsonProperty("base")
    private BigDecimal base;

    public String getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(String impuesto) {
        this.impuesto = impuesto;
    }

    public String getTipoFactor() {
        return tipoFactor;
    }

    public void setTipoFactor(String tipoFactor) {
        this.tipoFactor = tipoFactor;
    }

    public BigDecimal getTasaCuota() {
        return tasaCuota;
    }

    public void setTasaCuota(BigDecimal tasaCuota) {
        this.tasaCuota = tasaCuota;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public BigDecimal getBase() {
        return base;
    }

    public void setBase(BigDecimal base) {
        this.base = base;
    }
}