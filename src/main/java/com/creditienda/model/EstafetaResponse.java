package com.creditienda.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
}