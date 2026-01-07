package com.creditienda.exception;

public class ShopifyException extends RuntimeException {

    private final String type;
    private final String detail;

    public ShopifyException(String type, String message, String detail) {
        super(message);
        this.type = type;
        this.detail = detail;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }
}
