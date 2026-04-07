package com.creditienda.util.constantes;

import java.util.Map;

public final class EstatusCve {

    private EstatusCve() {
    }

    // =========================
    // ODC (cveEstatusOdc) - CATÁLOGO REAL
    // =========================
    public static final String VIGENTE = "vigente";
    public static final String ACEPTADA = "aceptada";
    public static final String RECHAZADA = "rechazada";
    public static final String EMBARCADA = "embarcada";
    public static final String FACTURADA = "facturada";
    public static final String EMBARCANDO = "embarcando";
    public static final String EN_PROCESO = "enProceso";
    public static final String CERRADA = "cerrada";
    public static final String RECHAZANDO = "rechazando";
    public static final String TRANSITO = "transito";
    public static final String RECOLECCION = "recoleccion";
    public static final String CON_COBERTURA = "conCobertura";
    public static final String REVISION = "revision";
    public static final String ENTREGA_SUCURSAL = "entregaSucursal";
    public static final String SIN_COBERTURA = "sinCobertura";
    public static final String ERROR_GUIA = "errorGuia";
    // =========================
    // DEVOLUCIÓN (ODC)
    // =========================
    public static final String DEVOLUCION = "devolucion";
    public static final String ENTREGA_DEVOLUCION = "entregaDevolucion";

    // =========================
    // DELIVERY (cveEstatusDelivery)
    // =========================
    public static final String RECIBIDO_ESTAFETA = "recibidoEstafeta";
    public static final String EN_TRANSITO = "enTransito";
    public static final String EN_PROCESO_ENTREGA_DOMICILIO = "enProcesoEntregaDomicilio";
    public static final String EN_PROCESO_ENTREGA_OFICINA = "enProcesoEntregaOficina";
    public static final String DISPONIBLE_OFICINA = "disponibleOficina";
    public static final String ENTREGADO = "entregado";
    public static final String ENTREGADO_SUCURSAL = "entregadoSucursal";

    // =========================
    // DEVOLUCIÓN (DELIVERY)
    // =========================
    public static final String DEVOLUCION_NUEVA_GUIA = "devolucionNuevaGuia";
    public static final String ENTREGADO_DEVOLUCION = "entregadoDevolucion";

    // =========================
    // 🔥 MAPA ESTAFETA → DELIVERY (REAL)
    // =========================
    private static final Map<Integer, String> ESTAFETA_TO_DELIVERY = Map.of(
            1, RECIBIDO_ESTAFETA,
            2, EN_TRANSITO,
            3, EN_PROCESO_ENTREGA_DOMICILIO,
            4, EN_PROCESO_ENTREGA_OFICINA,
            5, DISPONIBLE_OFICINA,
            6, ENTREGADO,
            7, ENTREGADO_SUCURSAL,
            // 🔥 DEVOLUCIÓN
            8, DEVOLUCION_NUEVA_GUIA,
            9, ENTREGADO_DEVOLUCION);

    public static String getCveDelivery(Integer codigoEstafeta) {

        if (codigoEstafeta == null)
            return null;

        String cve = ESTAFETA_TO_DELIVERY.get(codigoEstafeta);

        if (cve == null) {
            throw new IllegalArgumentException("Código Estafeta no mapeado: " + codigoEstafeta);
        }

        return cve;
    }
}