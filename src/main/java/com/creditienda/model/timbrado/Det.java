package com.creditienda.model.timbrado;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Det {

    @XmlAttribute
    @JsonProperty("noId")
    private String noId;

    @XmlAttribute
    @JsonProperty("cantidad")
    private BigDecimal cantidad;

    @XmlAttribute
    @JsonProperty("unidadMedida")
    private String unidadMedida;

    @XmlAttribute
    @JsonProperty("descripcion")
    private String descripcion;

    @XmlAttribute
    @JsonProperty("valorUnitario")
    private BigDecimal valorUnitario;

    @XmlAttribute
    @JsonProperty("importe")
    private BigDecimal importe;

    @XmlAttribute
    @JsonProperty("cveProdServ")
    private String cveProdServ;

    @XmlAttribute
    @JsonProperty("cveUnidadMedida")
    private String cveUnidadMedida;

    @XmlAttribute
    @JsonProperty("noInterno")
    private String noInterno;

    @XmlAttribute
    @JsonProperty("posicionOc")
    private int posicionOc;

    @XmlAttribute
    @JsonProperty("objetoImpuesto")
    private String objetoImpuesto;

    @XmlElement(name = "dit")
    @JsonProperty("dit")
    private List<Tra> dit;

    public String getNoId() {
        return noId;
    }

    public void setNoId(String noId) {
        this.noId = noId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getCveProdServ() {
        return cveProdServ;
    }

    public void setCveProdServ(String cveProdServ) {
        this.cveProdServ = cveProdServ;
    }

    public String getCveUnidadMedida() {
        return cveUnidadMedida;
    }

    public void setCveUnidadMedida(String cveUnidadMedida) {
        this.cveUnidadMedida = cveUnidadMedida;
    }

    public String getNoInterno() {
        return noInterno;
    }

    public void setNoInterno(String noInterno) {
        this.noInterno = noInterno;
    }

    public int getPosicionOc() {
        return posicionOc;
    }

    public void setPosicionOc(int posicionOc) {
        this.posicionOc = posicionOc;
    }

    public String getObjetoImpuesto() {
        return objetoImpuesto;
    }

    public void setObjetoImpuesto(String objetoImpuesto) {
        this.objetoImpuesto = objetoImpuesto;
    }

    public List<Tra> getDit() {
        return dit;
    }

    public void setDit(List<Tra> dit) {
        this.dit = dit;
    }
}