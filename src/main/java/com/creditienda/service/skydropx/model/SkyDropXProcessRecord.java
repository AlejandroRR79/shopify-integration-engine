package com.creditienda.service.skydropx.model;

import java.time.LocalDateTime;

public class SkyDropXProcessRecord {

    private Long id;

    private Long idShopifyOrder;

    private String orderNumber;

    private String quotationId;

    private String shipmentId;

    private String selectedRateId;

    private String trackingNumber;

    private String labelUrl;

    private String requestJson;

    private String quotationRawJson;

    private String selectedRateJson;

    private String shipmentRawJson;

    private String statusId;

    private String processStep;

    private Integer retryCount;

    private Boolean isRetryable;

    private Boolean isActive;

    private Boolean hasFinalShipment;

    private LocalDateTime nextRetryAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public SkyDropXProcessRecord() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdShopifyOrder() {
        return idShopifyOrder;
    }

    public void setIdShopifyOrder(Long idShopifyOrder) {
        this.idShopifyOrder = idShopifyOrder;
    }

    public String getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(String quotationId) {
        this.quotationId = quotationId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getSelectedRateId() {
        return selectedRateId;
    }

    public void setSelectedRateId(String selectedRateId) {
        this.selectedRateId = selectedRateId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getLabelUrl() {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl) {
        this.labelUrl = labelUrl;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getQuotationRawJson() {
        return quotationRawJson;
    }

    public void setQuotationRawJson(String quotationRawJson) {
        this.quotationRawJson = quotationRawJson;
    }

    public String getSelectedRateJson() {
        return selectedRateJson;
    }

    public void setSelectedRateJson(String selectedRateJson) {
        this.selectedRateJson = selectedRateJson;
    }

    public String getShipmentRawJson() {
        return shipmentRawJson;
    }

    public void setShipmentRawJson(String shipmentRawJson) {
        this.shipmentRawJson = shipmentRawJson;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getProcessStep() {
        return processStep;
    }

    public void setProcessStep(String processStep) {
        this.processStep = processStep;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getIsRetryable() {
        return isRetryable;
    }

    public void setIsRetryable(Boolean isRetryable) {
        this.isRetryable = isRetryable;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getHasFinalShipment() {
        return hasFinalShipment;
    }

    public void setHasFinalShipment(Boolean hasFinalShipment) {
        this.hasFinalShipment = hasFinalShipment;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public String toString() {
        return "SkyDropXProcessRecord{" +
                "id=" + id +
                ", idShopifyOrder=" + idShopifyOrder +
                ", orderNumber='" + orderNumber + '\'' +
                ", quotationId='" + quotationId + '\'' +
                ", shipmentId='" + shipmentId + '\'' +
                ", selectedRateId='" + selectedRateId + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", labelUrl='" + labelUrl + '\'' +
                ", statusId='" + statusId + '\'' +
                ", processStep='" + processStep + '\'' +
                ", retryCount=" + retryCount +
                ", isRetryable=" + isRetryable +
                ", isActive=" + isActive +
                ", hasFinalShipment=" + hasFinalShipment +
                ", nextRetryAt=" + nextRetryAt +
                ", completedAt=" + completedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

}