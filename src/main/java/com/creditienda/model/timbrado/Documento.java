package com.creditienda.model.timbrado;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "documento")
@XmlAccessorType(XmlAccessType.FIELD)
public class Documento {

    @JsonProperty("cab")
    private Cab cab;

    @JsonProperty("hdr")
    private Hdr hdr;

    @JsonProperty("mon")
    private Mon mon;

    @JsonProperty("dir")
    private List<Dir> dir;

    @JsonProperty("tex")
    private List<Tex> tex;

    @JsonProperty("det")
    private List<Det> det;

    @JsonProperty("tra")
    private List<Tra> tra;

    @JsonProperty("infglo")
    private Infglo infglo;

    @JsonProperty("ticket")
    private List<TicketDoc> ticket;

    // Getters y setters
    public Cab getCab() {
        return cab;
    }

    public void setCab(Cab cab) {
        this.cab = cab;
    }

    public Hdr getHdr() {
        return hdr;
    }

    public void setHdr(Hdr hdr) {
        this.hdr = hdr;
    }

    public Mon getMon() {
        return mon;
    }

    public void setMon(Mon mon) {
        this.mon = mon;
    }

    public List<Dir> getDir() {
        return dir;
    }

    public void setDir(List<Dir> dir) {
        this.dir = dir;
    }

    public List<Tex> getTex() {
        return tex;
    }

    public void setTex(List<Tex> tex) {
        this.tex = tex;
    }

    public List<Det> getDet() {
        return det;
    }

    public void setDet(List<Det> det) {
        this.det = det;
    }

    public List<Tra> getTra() {
        return tra;
    }

    public void setTra(List<Tra> tra) {
        this.tra = tra;
    }

    public Infglo getInfglo() {
        return infglo;
    }

    public void setInfglo(Infglo infglo) {
        this.infglo = infglo;
    }

    public List<TicketDoc> getTicket() {
        return ticket;
    }

    public void setTicket(List<TicketDoc> ticket) {
        this.ticket = ticket;
    }
}