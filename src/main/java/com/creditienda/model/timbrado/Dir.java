package com.creditienda.model.timbrado;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)

public class Dir {

    @XmlAttribute
    @JsonProperty("calificador")
    private String calificador;

    @XmlAttribute
    @JsonProperty("rfc")
    private String rfc;

    @XmlAttribute
    @JsonProperty("nombre")
    private String nombre;

    @XmlAttribute
    @JsonProperty("calle")
    private String calle;

    @XmlAttribute
    @JsonProperty("noExterior")
    private String noExterior;

    @XmlAttribute
    @JsonProperty("noInterior")
    private String noInterior;

    @XmlAttribute
    @JsonProperty("colonia")
    private String colonia;

    @XmlAttribute
    @JsonProperty("municipio")
    private String municipio;

    @XmlAttribute
    @JsonProperty("estado")
    private String estado;

    @XmlAttribute
    @JsonProperty("localidad")
    private String localidad;

    @XmlAttribute
    @JsonProperty("pais")
    private String pais;

    @XmlAttribute
    @JsonProperty("codigoPais")
    private String codigoPais;

    @XmlAttribute
    @JsonProperty("codigoPostal")
    private String codigoPostal;

    // Getters y setters
    public String getCalificador() {
        return calificador;
    }

    public void setCalificador(String calificador) {
        this.calificador = calificador;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getNoExterior() {
        return noExterior;
    }

    public void setNoExterior(String noExterior) {
        this.noExterior = noExterior;
    }

    public String getNoInterior() {
        return noInterior;
    }

    public void setNoInterior(String noInterior) {
        this.noInterior = noInterior;
    }

    public String getColonia() {
        return colonia;
    }

    public void setColonia(String colonia) {
        this.colonia = colonia;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getCodigoPais() {
        return codigoPais;
    }

    public void setCodigoPais(String codigoPais) {
        this.codigoPais = codigoPais;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }
}