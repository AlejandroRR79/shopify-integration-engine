package com.creditienda.dto.estafeta.guia;

public class WayBillRequestDTO {

    private IdentificationDTO identification;
    private SystemInformationDTO systemInformation;
    private LabelDefinitionDTO labelDefinition;

    public IdentificationDTO getIdentification() {
        return identification;
    }

    public void setIdentification(IdentificationDTO identification) {
        this.identification = identification;
    }

    public SystemInformationDTO getSystemInformation() {
        return systemInformation;
    }

    public void setSystemInformation(SystemInformationDTO systemInformation) {
        this.systemInformation = systemInformation;
    }

    public LabelDefinitionDTO getLabelDefinition() {
        return labelDefinition;
    }

    public void setLabelDefinition(LabelDefinitionDTO labelDefinition) {
        this.labelDefinition = labelDefinition;
    }
}
