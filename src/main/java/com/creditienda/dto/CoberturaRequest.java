package com.creditienda.dto;

import java.util.List;

public class CoberturaRequest {
    private List<Frequency> frequencies;

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public void setFrequencies(List<Frequency> frequencies) {
        this.frequencies = frequencies;
    }

    public static class Frequency {
        private List<PostalCode> origins;
        private List<PostalCode> destinations;

        public List<PostalCode> getOrigins() {
            return origins;
        }

        public void setOrigins(List<PostalCode> origins) {
            this.origins = origins;
        }

        public List<PostalCode> getDestinations() {
            return destinations;
        }

        public void setDestinations(List<PostalCode> destinations) {
            this.destinations = destinations;
        }
    }

    public static class PostalCode {
        private String postalCode;

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }
}