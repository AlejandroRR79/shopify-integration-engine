package com.creditienda.dto.estafeta.guia;

public class DestinationDTO {

    private Integer isDeliveryToPUDO;
    private Integer deliveryPUDOCode;
    private HomeAddressDTO homeAddress;

    public Integer getIsDeliveryToPUDO() {
        return isDeliveryToPUDO;
    }

    public void setIsDeliveryToPUDO(Integer isDeliveryToPUDO) {
        this.isDeliveryToPUDO = isDeliveryToPUDO;
    }

    public Integer getDeliveryPUDOCode() {
        return deliveryPUDOCode;
    }

    public void setDeliveryPUDOCode(Integer deliveryPUDOCode) {
        this.deliveryPUDOCode = deliveryPUDOCode;
    }

    public HomeAddressDTO getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(HomeAddressDTO homeAddress) {
        this.homeAddress = homeAddress;
    }
}
