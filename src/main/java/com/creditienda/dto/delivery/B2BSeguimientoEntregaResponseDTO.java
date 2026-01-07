package com.creditienda.dto.delivery;

import java.util.List;

public class B2BSeguimientoEntregaResponseDTO {

    private Boolean isSuccess;
    private String error;
    private List<B2BSeguimientoEntregaOrdenDTO> data;

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<B2BSeguimientoEntregaOrdenDTO> getData() {
        return data;
    }

    public void setData(List<B2BSeguimientoEntregaOrdenDTO> data) {
        this.data = data;
    }
}
