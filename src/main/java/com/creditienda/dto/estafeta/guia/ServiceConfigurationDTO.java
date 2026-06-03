package com.creditienda.dto.estafeta.guia;

import com.fasterxml.jackson.annotation.JsonSetter;

public class ServiceConfigurationDTO {

    private Integer quantityOfLabels;
    private String serviceTypeId;
    private String salesOrganization;
    private String effectiveDate;
    private String originZipCodeForRouting;
    private Boolean isInsurance;
    private InsuranceDTO insurance;
    private Boolean isReturnDocument;
    private ReturnDocumentDTO returnDocument;

    public Integer getQuantityOfLabels() {
        return quantityOfLabels;
    }

    public void setQuantityOfLabels(Integer quantityOfLabels) {
        this.quantityOfLabels = quantityOfLabels;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getSalesOrganization() {
        return salesOrganization;
    }

    public void setSalesOrganization(String salesOrganization) {
        this.salesOrganization = salesOrganization;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getOriginZipCodeForRouting() {
        return originZipCodeForRouting;
    }

    public void setOriginZipCodeForRouting(String originZipCodeForRouting) {
        this.originZipCodeForRouting = originZipCodeForRouting;
    }

    public Boolean getIsInsurance() {
        return isInsurance;
    }

    @JsonSetter("isInsurance")
    public void setIsInsurance(Object value) {
        if (value instanceof Boolean) {
            this.isInsurance = (Boolean) value;
        } else if (value instanceof Integer) {
            this.isInsurance = ((Integer) value) == 1;
        }
    }

    public InsuranceDTO getInsurance() {
        return insurance;
    }

    public void setInsurance(InsuranceDTO insurance) {
        this.insurance = insurance;
    }

    public Boolean getIsReturnDocument() {
        return isReturnDocument;
    }

    @JsonSetter("isReturnDocument")
    public void setIsReturnDocument(Object value) {
        if (value instanceof Boolean) {
            this.isReturnDocument = (Boolean) value;
        } else if (value instanceof Integer) {
            this.isReturnDocument = ((Integer) value) == 1;
        }
    }

    public ReturnDocumentDTO getReturnDocument() {
        return returnDocument;
    }

    public void setReturnDocument(ReturnDocumentDTO returnDocument) {
        this.returnDocument = returnDocument;
    }
}
