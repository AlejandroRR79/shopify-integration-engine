package com.creditienda.dto.skydropx;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class GuiaUnificadaResponseDTO {

    private JsonNode estafeta;

    private SkyDropXQuotationProcessResponseDTO skydropx;

    private Map<String, JsonNode> errores;

    public JsonNode getEstafeta() {
        return estafeta;
    }

    public void setEstafeta(JsonNode estafeta) {
        this.estafeta = estafeta;
    }

    public SkyDropXQuotationProcessResponseDTO getSkydropx() {
        return skydropx;
    }

    public void setSkydropx(SkyDropXQuotationProcessResponseDTO skydropx) {
        this.skydropx = skydropx;
    }

    public Map<String, JsonNode> getErrores() {
        return errores;
    }

    public void setErrores(Map<String, JsonNode> errores) {
        this.errores = errores;
    }
}
