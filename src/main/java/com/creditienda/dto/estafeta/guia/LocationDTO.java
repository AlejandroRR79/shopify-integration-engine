package com.creditienda.dto.estafeta.guia;

public class LocationDTO {

    private Integer isDRAAlternative;
    private OriginDTO origin;
    private DestinationDTO destination;

    public Integer getIsDRAAlternative() {
        return isDRAAlternative;
    }

    public void setIsDRAAlternative(Integer isDRAAlternative) {
        this.isDRAAlternative = isDRAAlternative;
    }

    public OriginDTO getOrigin() {
        return origin;
    }

    public void setOrigin(OriginDTO origin) {
        this.origin = origin;
    }

    public DestinationDTO getDestination() {
        return destination;
    }

    public void setDestination(DestinationDTO destination) {
        this.destination = destination;
    }
}
