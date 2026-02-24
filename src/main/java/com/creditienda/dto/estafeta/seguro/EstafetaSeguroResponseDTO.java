package com.creditienda.dto.estafeta.seguro;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EstafetaSeguroResponseDTO {

    @JsonProperty("Result")
    private Result Result;

    @JsonProperty("Item")
    private List<Item> Item;

    public Result getResult() {
        return Result;
    }

    public void setResult(Result result) {
        Result = result;
    }

    public List<Item> getItem() {
        return Item;
    }

    public void setItem(List<Item> item) {
        Item = item;
    }

    // =============================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("Code")
        private int Code;

        @JsonProperty("Description")
        private String Description;

        public int getCode() {
            return Code;
        }

        public void setCode(int code) {
            Code = code;
        }

        public String getDescription() {
            return Description;
        }

        public void setDescription(String description) {
            Description = description;
        }
    }

    // =============================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JsonProperty("Code")
        private String Code;

        @JsonProperty("TrackingCode")
        private String TrackingCode;

        @JsonProperty("ReferenceCode")
        private String ReferenceCode;

        @JsonProperty("CIA")
        private CIA CIA;

        @JsonProperty("Error")
        private Error Error;

        public String getCode() {
            return Code;
        }

        public void setCode(String code) {
            Code = code;
        }

        public String getTrackingCode() {
            return TrackingCode;
        }

        public void setTrackingCode(String trackingCode) {
            TrackingCode = trackingCode;
        }

        public String getReferenceCode() {
            return ReferenceCode;
        }

        public void setReferenceCode(String referenceCode) {
            ReferenceCode = referenceCode;
        }

        public CIA getCIA() {
            return CIA;
        }

        public void setCIA(CIA CIA) {
            this.CIA = CIA;
        }

        public Error getError() {
            return Error;
        }

        public void setError(Error error) {
            Error = error;
        }
    }

    // =============================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CIA {

        @JsonProperty("InsurancePDF")
        private String InsurancePDF;

        public String getInsurancePDF() {
            return InsurancePDF;
        }

        public void setInsurancePDF(String insurancePDF) {
            InsurancePDF = insurancePDF;
        }
    }

    // =============================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {

        @JsonProperty("Code")
        private int Code;

        @JsonProperty("Description")
        private String Description;

        public int getCode() {
            return Code;
        }

        public void setCode(int code) {
            Code = code;
        }

        public String getDescription() {
            return Description;
        }

        public void setDescription(String description) {
            Description = description;
        }
    }
}
