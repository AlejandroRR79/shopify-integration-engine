package com.creditienda.dto.skydropx;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SkyDropXShipmentResponseDTO {

    private Data data;

    private List<Included> included;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<Included> getIncluded() {
        return included;
    }

    public void setIncluded(List<Included> included) {
        this.included = included;
    }

    /**
     * =====================================
     * HELPERS
     * =====================================
     */

    /**
     * Obtener label URL.
     */
    public String getLabelUrl() {

        if (included == null) {
            return null;
        }

        for (Included item : included) {

            if ("package".equals(
                    item.getType())) {

                if (item.getAttributes() != null) {

                    return item.getAttributes()
                            .getLabelUrl();
                }
            }
        }

        return null;
    }

    /**
     * Obtener tracking number.
     */
    public String getTrackingNumber() {

        if (included == null) {
            return null;
        }

        for (Included item : included) {

            if ("package".equals(
                    item.getType())) {

                if (item.getAttributes() != null) {

                    return item.getAttributes()
                            .getTrackingNumber();
                }
            }
        }

        return null;
    }

    /**
     * Obtener tracking URL provider.
     */
    public String getTrackingUrlProvider() {

        if (included == null) {
            return null;
        }

        for (Included item : included) {

            if ("package".equals(
                    item.getType())) {

                if (item.getAttributes() != null) {

                    return item.getAttributes()
                            .getTrackingUrlProvider();
                }
            }
        }

        return null;
    }

    /**
     * =====================================
     * DATA
     * =====================================
     */
    public static class Data {

        private String id;

        private Attributes attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Attributes getAttributes() {
            return attributes;
        }

        public void setAttributes(
                Attributes attributes) {

            this.attributes = attributes;
        }
    }

    /**
     * =====================================
     * ATTRIBUTES
     * =====================================
     */
    public static class Attributes {

        @JsonProperty("workflow_status")
        private String workflowStatus;

        @JsonProperty("master_tracking_number")
        private String masterTrackingNumber;

        public String getWorkflowStatus() {
            return workflowStatus;
        }

        public void setWorkflowStatus(
                String workflowStatus) {

            this.workflowStatus = workflowStatus;
        }

        public String getMasterTrackingNumber() {
            return masterTrackingNumber;
        }

        public void setMasterTrackingNumber(
                String masterTrackingNumber) {

            this.masterTrackingNumber = masterTrackingNumber;
        }
    }

    /**
     * =====================================
     * INCLUDED
     * =====================================
     */
    public static class Included {

        private String type;

        private IncludedAttributes attributes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public IncludedAttributes getAttributes() {
            return attributes;
        }

        public void setAttributes(
                IncludedAttributes attributes) {

            this.attributes = attributes;
        }
    }

    /**
     * =====================================
     * INCLUDED ATTRIBUTES
     * =====================================
     */
    public static class IncludedAttributes {

        @JsonProperty("tracking_number")
        private String trackingNumber;

        @JsonProperty("tracking_status")
        private String trackingStatus;

        @JsonProperty("tracking_url_provider")
        private String trackingUrlProvider;

        @JsonProperty("label_url")
        private String labelUrl;

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public void setTrackingNumber(
                String trackingNumber) {

            this.trackingNumber = trackingNumber;
        }

        public String getTrackingStatus() {
            return trackingStatus;
        }

        public void setTrackingStatus(
                String trackingStatus) {

            this.trackingStatus = trackingStatus;
        }

        public String getTrackingUrlProvider() {
            return trackingUrlProvider;
        }

        public void setTrackingUrlProvider(
                String trackingUrlProvider) {

            this.trackingUrlProvider = trackingUrlProvider;
        }

        public String getLabelUrl() {
            return labelUrl;
        }

        public void setLabelUrl(
                String labelUrl) {

            this.labelUrl = labelUrl;
        }
    }
}