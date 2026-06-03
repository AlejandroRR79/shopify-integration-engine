package com.creditienda.dto.skydropx;

import java.util.List;

public class SkyDropXQuotationRequestDTO {

        private Quotation quotation;

        public Quotation getQuotation() {
                return quotation;
        }

        public void setQuotation(Quotation quotation) {
                this.quotation = quotation;
        }

        public static class Quotation {

                private Address address_from;

                private Address address_to;

                private List<Parcel> parcels;

                private List<String> requested_carriers;

                public Address getAddress_from() {
                        return address_from;
                }

                public void setAddress_from(Address address_from) {
                        this.address_from = address_from;
                }

                public Address getAddress_to() {
                        return address_to;
                }

                public void setAddress_to(Address address_to) {
                        this.address_to = address_to;
                }

                public List<Parcel> getParcels() {
                        return parcels;
                }

                public void setParcels(List<Parcel> parcels) {
                        this.parcels = parcels;
                }

                public List<String> getRequested_carriers() {
                        return requested_carriers;
                }

                public void setRequested_carriers(
                                List<String> requested_carriers) {
                        this.requested_carriers = requested_carriers;
                }
        }

        public static class Address {

                private String country_code;

                private String postal_code;

                private String area_level1;

                private String area_level2;

                private String area_level3;

                public String getCountry_code() {
                        return country_code;
                }

                public void setCountry_code(String country_code) {
                        this.country_code = country_code;
                }

                public String getPostal_code() {
                        return postal_code;
                }

                public void setPostal_code(String postal_code) {
                        this.postal_code = postal_code;
                }

                public String getArea_level1() {
                        return area_level1;
                }

                public void setArea_level1(String area_level1) {
                        this.area_level1 = area_level1;
                }

                public String getArea_level2() {
                        return area_level2;
                }

                public void setArea_level2(String area_level2) {
                        this.area_level2 = area_level2;
                }

                public String getArea_level3() {
                        return area_level3;
                }

                public void setArea_level3(String area_level3) {
                        this.area_level3 = area_level3;
                }
        }

        public static class Parcel {

                private double length;

                private double width;

                private double height;

                private double weight;

                public double getLength() {
                        return length;
                }

                public void setLength(double length) {
                        this.length = length;
                }

                public double getWidth() {
                        return width;
                }

                public void setWidth(double width) {
                        this.width = width;
                }

                public double getHeight() {
                        return height;
                }

                public void setHeight(double height) {
                        this.height = height;
                }

                public double getWeight() {
                        return weight;
                }

                public void setWeight(double weight) {
                        this.weight = weight;
                }
        }
}