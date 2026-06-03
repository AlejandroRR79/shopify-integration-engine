package com.creditienda.model.timbrado;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)

public class Mon {

    @XmlAttribute
    @JsonProperty("moneda")
    private String moneda;

    @XmlAttribute
    @JsonProperty("tipoCambio")
    private BigDecimal tipoCambio;

    @XmlAttribute
    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @XmlAttribute
    @JsonProperty("total")
    private BigDecimal total;

    @XmlAttribute
    @JsonProperty("totalImpuestoTras")
    private BigDecimal totalImpuestoTras;

    @XmlAttribute
    @JsonProperty("pesoBruto")
    private BigDecimal pesoBruto;

    @XmlAttribute
    @JsonProperty("pesoNeto")
    private BigDecimal pesoNeto;

    // Getters y setters
    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getTipoCambio() {
        return tipoCambio;
    }

    public void setTipoCambio(BigDecimal tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotalImpuestoTras() {
        return totalImpuestoTras;
    }

    public void setTotalImpuestoTras(BigDecimal totalImpuestoTras) {
        this.totalImpuestoTras = totalImpuestoTras;
    }

    public BigDecimal getPesoBruto() {
        return pesoBruto;
    }

    public void setPesoBruto(BigDecimal pesoBruto) {
        this.pesoBruto = pesoBruto;
    }

    public BigDecimal getPesoNeto() {
        return pesoNeto;
    }

    public void setPesoNeto(BigDecimal pesoNeto) {
        this.pesoNeto = pesoNeto;
    }
}