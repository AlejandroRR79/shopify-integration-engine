package com.creditienda.dto.skydropx;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO request shipment SkyDropX.
 */
public class SkyDropXShipmentRequestDTO {

    private Shipment shipment;

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public static class Shipment {

        private String rate_id;

        private String printing_format;

        private Address address_from;

        private Address address_to;

        private List<PackageDetail> packages;

        public String getRate_id() {
            return rate_id;
        }

        public void setRate_id(String rate_id) {
            this.rate_id = rate_id;
        }

        public String getPrinting_format() {
            return printing_format;
        }

        public void setPrinting_format(String printing_format) {
            this.printing_format = printing_format;
        }

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

        public List<PackageDetail> getPackages() {
            return packages;
        }

        public void setPackages(List<PackageDetail> packages) {
            this.packages = packages;
        }
    }

    public static class Address {

        private String street1;

        private String name;

        private String company;

        private String phone;

        private String email;

        private String reference;

        private String tax_id_number;

        public String getStreet1() {
            return street1;
        }

        public void setStreet1(String street1) {
            this.street1 = street1;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getTax_id_number() {
            return tax_id_number;
        }

        public void setTax_id_number(String tax_id_number) {
            this.tax_id_number = tax_id_number;
        }
    }

    public static class PackageDetail {

        private String package_number;

        private Boolean package_protected;

        private BigDecimal declared_value;

        private String consignment_note;

        private String package_type;

        private List<Product> products;

        public String getPackage_number() {
            return package_number;
        }

        public void setPackage_number(String package_number) {
            this.package_number = package_number;
        }

        public Boolean getPackage_protected() {
            return package_protected;
        }

        public void setPackage_protected(Boolean package_protected) {
            this.package_protected = package_protected;
        }

        public BigDecimal getDeclared_value() {
            return declared_value;
        }

        public void setDeclared_value(BigDecimal declared_value) {
            this.declared_value = declared_value;
        }

        public String getConsignment_note() {
            return consignment_note;
        }

        public void setConsignment_note(String consignment_note) {
            this.consignment_note = consignment_note;
        }

        public String getPackage_type() {
            return package_type;
        }

        public void setPackage_type(String package_type) {
            this.package_type = package_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }
    }

    public static class Product {

        private String name;

        private Integer quantity;

        private BigDecimal price;

        private String sku;

        private String country_code;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getCountry_code() {
            return country_code;
        }

        public void setCountry_code(String country_code) {
            this.country_code = country_code;
        }
    }
}