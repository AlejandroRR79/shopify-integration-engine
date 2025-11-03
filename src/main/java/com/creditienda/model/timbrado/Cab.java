package com.creditienda.model.timbrado;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Cab {

    @XmlAttribute
    @JsonProperty("rfcEmisor")
    private String rfcEmisor;

    @XmlAttribute
    @JsonProperty("rfcReceptor")
    private String rfcReceptor;

    @XmlAttribute
    @JsonProperty("tipoComprobante")
    private String tipoComprobante;

    @XmlAttribute
    @JsonProperty("versionCfdi")
    private String versionCfdi;

    // Getters y setters
    public String getRfcEmisor() {
        return rfcEmisor;
    }

    public void setRfcEmisor(String rfcEmisor) {
        this.rfcEmisor = rfcEmisor;
    }

    public String getRfcReceptor() {
        return rfcReceptor;
    }

    public void setRfcReceptor(String rfcReceptor) {
        this.rfcReceptor = rfcReceptor;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getVersionCfdi() {
        return versionCfdi;
    }

    public void setVersionCfdi(String versionCfdi) {
        this.versionCfdi = versionCfdi;
    }
}