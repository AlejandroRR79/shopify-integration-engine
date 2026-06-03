package com.creditienda.dto.shopify;

import java.util.Map;

public class RespuestaMultiTiendaDTO {

    private String loteId;

    /** Clave: alias de tienda, Valor: resultado del bulk */
    private Map<String, RespuestaLoteBulkDTO> resultados;

    public RespuestaMultiTiendaDTO() {}

    public RespuestaMultiTiendaDTO(String loteId, Map<String, RespuestaLoteBulkDTO> resultados) {
        this.loteId = loteId;
        this.resultados = resultados;
    }

    public String getLoteId() { return loteId; }
    public void setLoteId(String loteId) { this.loteId = loteId; }

    public Map<String, RespuestaLoteBulkDTO> getResultados() { return resultados; }
    public void setResultados(Map<String, RespuestaLoteBulkDTO> resultados) { this.resultados = resultados; }
}
