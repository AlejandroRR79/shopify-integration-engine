package com.creditienda.dto;

public class ImageDTO {
    private String src;
    private Integer position;
    private String altText;

    public void setSrc(String src) {
        this.src = src;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getSrc() {
        return src;
    }

    public Integer getPosition() {
        return position;
    }

    public String getAltText() {
        return altText;
    }
}
