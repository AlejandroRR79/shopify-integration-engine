package com.creditienda.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EstafetaResponse {

    private Result result;
    private List<ItemHistory> itemHistories;
    private List<Item> items;

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

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemHistory {
        private ErrorInfo error;
        private Information information;
        private List<History> histories;

        public ErrorInfo getError() {
            return error;
        }

        public void setError(ErrorInfo error) {
            this.error = error;
        }

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

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class History {
        private String spanishDescription;
        private String eventDateTime;
        private String warehouseName;

        public String getSpanishDescription() {
            return spanishDescription;
        }

        public void setSpanishDescription(String spanishDescription) {
            this.spanishDescription = spanishDescription;
        }

        public String getEventDateTime() {
            return eventDateTime;
        }

        public void setEventDateTime(String eventDateTime) {
            this.eventDateTime = eventDateTime;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public void setWarehouseName(String warehouseName) {
            this.warehouseName = warehouseName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Information information;
        private Service service;
        private PackageInfo packageInfo;
        private PickupDetails pickupDetails;
        private Status statusCurrent;
        private DeliveryDetails deliveryDetails;
        private List<Status> status;

        public Information getInformation() {
            return information;
        }

        public void setInformation(Information information) {
            this.information = information;
        }

        public Service getService() {
            return service;
        }

        public void setService(Service service) {
            this.service = service;
        }

        public PackageInfo getPackageInfo() {
            return packageInfo;
        }

        public void setPackageInfo(PackageInfo packageInfo) {
            this.packageInfo = packageInfo;
        }

        public PickupDetails getPickupDetails() {
            return pickupDetails;
        }

        public void setPickupDetails(PickupDetails pickupDetails) {
            this.pickupDetails = pickupDetails;
        }

        public Status getStatusCurrent() {
            return statusCurrent;
        }

        public void setStatusCurrent(Status statusCurrent) {
            this.statusCurrent = statusCurrent;
        }

        public DeliveryDetails getDeliveryDetails() {
            return deliveryDetails;
        }

        public void setDeliveryDetails(DeliveryDetails deliveryDetails) {
            this.deliveryDetails = deliveryDetails;
        }

        public List<Status> getStatus() {
            return status;
        }

        public void setStatus(List<Status> status) {
            this.status = status;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String code;
        private String spanishName;
        private String eventDateTime;
        private String warehouseName;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSpanishName() {
            return spanishName;
        }

        public void setSpanishName(String spanishName) {
            this.spanishName = spanishName;
        }

        public String getEventDateTime() {
            return eventDateTime;
        }

        public void setEventDateTime(String eventDateTime) {
            this.eventDateTime = eventDateTime;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public void setWarehouseName(String warehouseName) {
            this.warehouseName = warehouseName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorInfo {
        private String code;
        private String description;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        private String code;
        private String spanishName;
        private String estimatedDeliveryDate;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSpanishName() {
            return spanishName;
        }

        public void setSpanishName(String spanishName) {
            this.spanishName = spanishName;
        }

        public String getEstimatedDeliveryDate() {
            return estimatedDeliveryDate;
        }

        public void setEstimatedDeliveryDate(String estimatedDeliveryDate) {
            this.estimatedDeliveryDate = estimatedDeliveryDate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackageInfo {
        private String nameType;
        private double weight;

        public String getNameType() {
            return nameType;
        }

        public void setNameType(String nameType) {
            this.nameType = nameType;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PickupDetails {
        private String warehouseCode;
        private String pickupDateTime;

        public String getWarehouseCode() {
            return warehouseCode;
        }

        public void setWarehouseCode(String warehouseCode) {
            this.warehouseCode = warehouseCode;
        }

        public String getPickupDateTime() {
            return pickupDateTime;
        }

        public void setPickupDateTime(String pickupDateTime) {
            this.pickupDateTime = pickupDateTime;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryDetails {
        private String receiverName;
        private double latitude;
        private double longitude;

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}