package com.creditienda.model.timbrado;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Hdr {

    @XmlAttribute
    @JsonProperty("idFactura")
    private String idFactura;

    @XmlAttribute
    @JsonProperty("serie")
    private String serie;

    @XmlAttribute
    @JsonProperty("folio")
    private String folio;

    @XmlAttribute
    @JsonProperty("fechaEmisionCfdi")
    private String fechaEmisionCfdi;

    @XmlAttribute
    @JsonProperty("formaPago")
    private String formaPago;

    @XmlAttribute
    @JsonProperty("metodoPago")
    private String metodoPago;

    @XmlAttribute
    @JsonProperty("lugarExp")
    private String lugarExp;

    @XmlAttribute
    @JsonProperty("regimenFisEmisor")
    private String regimenFisEmisor;

    @XmlAttribute
    @JsonProperty("usoCfdiReceptor")
    private String usoCfdiReceptor;

    @XmlAttribute
    @JsonProperty("ejercicioFiscal")
    private String ejercicioFiscal;

    @XmlAttribute
    @JsonProperty("centroCostos")
    private String centroCostos;

    @XmlAttribute
    @JsonProperty("regimenFisReceptor")
    private String regimenFisReceptor;

    @XmlAttribute
    @JsonProperty("exportacion")
    private String exportacion;

    // Getters y setters
    public String getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(String idFactura) {
        this.idFactura = idFactura;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getFechaEmisionCfdi() {
        return fechaEmisionCfdi;
    }

    public void setFechaEmisionCfdi(String fechaEmisionCfdi) {
        this.fechaEmisionCfdi = fechaEmisionCfdi;
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getLugarExp() {
        return lugarExp;
    }

    public void setLugarExp(String lugarExp) {
        this.lugarExp = lugarExp;
    }

    public String getRegimenFisEmisor() {
        return regimenFisEmisor;
    }

    public void setRegimenFisEmisor(String regimenFisEmisor) {
        this.regimenFisEmisor = regimenFisEmisor;
    }

    public String getUsoCfdiReceptor() {
        return usoCfdiReceptor;
    }

    public void setUsoCfdiReceptor(String usoCfdiReceptor) {
        this.usoCfdiReceptor = usoCfdiReceptor;
    }

    public String getEjercicioFiscal() {
        return ejercicioFiscal;
    }

    public void setEjercicioFiscal(String ejercicioFiscal) {
        this.ejercicioFiscal = ejercicioFiscal;
    }

    public String getCentroCostos() {
        return centroCostos;
    }

    public void setCentroCostos(String centroCostos) {
        this.centroCostos = centroCostos;
    }

    public String getRegimenFisReceptor() {
        return regimenFisReceptor;
    }

    public void setRegimenFisReceptor(String regimenFisReceptor) {
        this.regimenFisReceptor = regimenFisReceptor;
    }

    public String getExportacion() {
        return exportacion;
    }

    public void setExportacion(String exportacion) {
        this.exportacion = exportacion;
    }
}