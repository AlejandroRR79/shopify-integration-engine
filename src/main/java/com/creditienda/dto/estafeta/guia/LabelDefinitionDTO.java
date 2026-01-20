package com.creditienda.dto.estafeta.guia;

public class LabelDefinitionDTO {

    private WayBillDocumentDTO wayBillDocument;
    private ItemDescriptionDTO itemDescription;
    private ServiceConfigurationDTO serviceConfiguration;
    private LocationDTO location;

    public WayBillDocumentDTO getWayBillDocument() {
        return wayBillDocument;
    }

    public void setWayBillDocument(WayBillDocumentDTO wayBillDocument) {
        this.wayBillDocument = wayBillDocument;
    }

    public ItemDescriptionDTO getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(ItemDescriptionDTO itemDescription) {
        this.itemDescription = itemDescription;
    }

    public ServiceConfigurationDTO getServiceConfiguration() {
        return serviceConfiguration;
    }

    public void setServiceConfiguration(ServiceConfigurationDTO serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }
}
