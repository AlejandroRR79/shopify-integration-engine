package com.creditienda.service.skydropx.constants;

public class SkyDropXProcessSupersededException extends RuntimeException {

    public SkyDropXProcessSupersededException(String quotationId) {
        super("Proceso supersedido, abortando quotationId=" + quotationId);
    }
}
