package com.creditienda.dto.skydropx;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response quotation SkyDropX.
 */
public class SkyDropXQuotationResponseDTO {

    private String id;

    @JsonProperty("is_completed")
    private Boolean isCompleted;

    private List<Rate> rates;

    public String getId() {
        return id;
    }

    public void setId(
            String id) {
        this.id = id;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(
            Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public List<Rate> getRates() {
        return rates;
    }

    public void setRates(
            List<Rate> rates) {
        this.rates = rates;
    }

    /**
     * =====================================
     * RATE
     * =====================================
     */
    public static class Rate {

        private Boolean success;

        private String id;

        @JsonProperty("provider_name")
        private String providerName;

        @JsonProperty("provider_display_name")
        private String providerDisplayName;

        @JsonProperty("provider_service_name")
        private String providerServiceName;

        @JsonProperty("provider_service_code")
        private String providerServiceCode;

        private String status;

        private String total;

        private Integer days;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(
                Boolean success) {
            this.success = success;
        }

        public String getId() {
            return id;
        }

        public void setId(
                String id) {
            this.id = id;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(
                String providerName) {
            this.providerName = providerName;
        }

        public String getProviderDisplayName() {
            return providerDisplayName;
        }

        public void setProviderDisplayName(
                String providerDisplayName) {
            this.providerDisplayName = providerDisplayName;
        }

        public String getProviderServiceName() {
            return providerServiceName;
        }

        public void setProviderServiceName(
                String providerServiceName) {
            this.providerServiceName = providerServiceName;
        }

        public String getProviderServiceCode() {
            return providerServiceCode;
        }

        public void setProviderServiceCode(
                String providerServiceCode) {
            this.providerServiceCode = providerServiceCode;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(
                String status) {
            this.status = status;
        }

        public String getTotal() {
            return total;
        }

        public void setTotal(
                String total) {
            this.total = total;
        }

        public Integer getDays() {
            return days;
        }

        public void setDays(
                Integer days) {
            this.days = days;
        }
    }
}