package com.creditienda.model;

import java.util.List;

public class EstafetaResponse {
    private Result result;
    private List<ItemHistory> itemHistories;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public List<ItemHistory> getItemHistories() {
        return itemHistories;
    }

    public void setItemHistories(List<ItemHistory> itemHistories) {
        this.itemHistories = itemHistories;
    }

    // Subclase: Result
    public static class Result {
        private boolean success;
        private int code;
        private String description;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // Subclase: ItemHistory
    public static class ItemHistory {
        private Information information;
        private List<History> histories;

        public Information getInformation() {
            return information;
        }

        public void setInformation(Information information) {
            this.information = information;
        }

        public List<History> getHistories() {
            return histories;
        }

        public void setHistories(List<History> histories) {
            this.histories = histories;
        }
    }

    // Subclase: Information
    public static class Information {
        private String originalWaybill;
        private String waybillCode;
        private String trackingCode;

        public String getOriginalWaybill() {
            return originalWaybill;
        }

        public void setOriginalWaybill(String originalWaybill) {
            this.originalWaybill = originalWaybill;
        }

        public String getWaybillCode() {
            return waybillCode;
        }

        public void setWaybillCode(String waybillCode) {
            this.waybillCode = waybillCode;
        }

        public String getTrackingCode() {
            return trackingCode;
        }

        public void setTrackingCode(String trackingCode) {
            this.trackingCode = trackingCode;
        }
    }

    // Subclase: History
    public static class History {
        private String spanishDescription;
        private String reasonCode;
        private String eventDateTime;
        private String warehouseCode;
        private String warehouseName;

        public String getSpanishDescription() {
            return spanishDescription;
        }

        public void setSpanishDescription(String spanishDescription) {
            this.spanishDescription = spanishDescription;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getEventDateTime() {
            return eventDateTime;
        }

        public void setEventDateTime(String eventDateTime) {
            this.eventDateTime = eventDateTime;
        }

        public String getWarehouseCode() {
            return warehouseCode;
        }

        public void setWarehouseCode(String warehouseCode) {
            this.warehouseCode = warehouseCode;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public void setWarehouseName(String warehouseName) {
            this.warehouseName = warehouseName;
        }
    }
}