package com.creditienda.model.timbrado;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ticketDoc")
@XmlAccessorType(XmlAccessType.FIELD)
public class TicketDoc {

    @XmlAttribute
    @JsonProperty("idTicket")
    private int idTicket;

    @XmlAttribute
    @JsonProperty("facturaGlobal")
    private int facturaGlobal;

    @XmlAttribute
    @JsonProperty("folio")
    private String folio;

    @XmlAttribute
    @JsonProperty("importeEnCFDI")
    private int importeEnCFDI;

    // Getters y setters
    public int getIdTicket() {
        return idTicket;
    }

    public void setIdTicket(int idTicket) {
        this.idTicket = idTicket;
    }

    public int getFacturaGlobal() {
        return facturaGlobal;
    }

    public void setFacturaGlobal(int facturaGlobal) {
        this.facturaGlobal = facturaGlobal;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public int getImporteEnCFDI() {
        return importeEnCFDI;
    }

    public void setImporteEnCFDI(int importeEnCFDI) {
        this.importeEnCFDI = importeEnCFDI;
    }
}