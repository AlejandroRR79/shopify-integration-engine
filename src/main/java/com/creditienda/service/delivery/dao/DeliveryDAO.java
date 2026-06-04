package com.creditienda.service.delivery.dao;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;

import org.springframework.stereotype.Repository;

import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;

@Repository
public class DeliveryDAO {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryDAO.class);

    private final JdbcTemplate jdbcTemplate;

    public DeliveryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<B2BSeguimientoEntregaOrdenDTO> findByEstatus(
            List<String> estatusOdcList,
            List<Integer> estatusDeliveryList) {

        StringBuilder sql = new StringBuilder("""
                        SELECT
                        so.idShopifyOrder,
                        so.idSucursalCliente,
                        so.idSucursalProveedor,
                        so.idShopify,
                        so.email,
                        so.name,
                        so.financialStatus,
                        so.fulfillmentStatus,
                        so.totalPrice,
                        so.subtotalPrice,
                        so.totalTax,
                        so.currency,
                        so.orderNumber,
                        so.createdAt,
                        so.updatedAt,
                        so.customerName,
                        so.shippingAddess1,
                        so.shippingAddess2,
                        so.shippingCity,
                        so.shippingCountry,
                        so.shippingPhone,
                        so.shippingCP,
                        so.shippingState,
                        so.shippingContact,
                        so.detailPeso,
                        so.detailCantidad,
                        so.detailCodigoAMS,
                        so.detailDescripcion,
                        so.idMarketplace,
                        so.precio,
                        so.precio_producto,
                        so.idEstatusOdc,
                        so.comision,
                        so.idEstatusDelivery,
                        so.fechaEstatusDelivery,
                        so.originResultCode,
                        so.destinationsResultCode,
                        so.waybill,
                        so.trackingCode,
                        so.rutaGuia,
                        so.archivoGuia,
                        so.destinationAddress,
                        so.referenceNumber,
                        so.fechaSolicitud,
                        so.descripcionEntrega,
                        so.shippingAddress,
                        so.shippingNumExt,
                        so.shippingColony,
                        so.reasonCodeDescription,
                        so.isInsurance,
                        so.paqueteria,

                        -- 🔥 NUEVO
                        so.waybillDevolution,
                        so.trackingCodeDevolution,
                        eo.cveEstatusOdc,
                        ed.cveEstatusDelivery,
                        ed.codigo AS codigoDelivery

                        FROM SHOPIFY_ORDER so
                        JOIN EstatusOdc eo
                            ON eo.idEstatusOdc = so.idEstatusOdc
                        LEFT JOIN EstatusDelivery ed
                            ON ed.idEstatusDelivery = so.idEstatusDelivery
                        WHERE eo.cveEstatusOdc IN (
                """);
        // 🔥 IN dinámico ODC
        sql.append(String.join(",", estatusOdcList.stream().map(x -> "?").toList())).append(")");

        List<Object> params = new ArrayList<>(estatusOdcList);

        // 🔥 IN dinámico DELIVERY (opcional)
        if (estatusDeliveryList != null && !estatusDeliveryList.isEmpty()) {
            sql.append(" AND so.idEstatusDelivery IN (");
            sql.append(String.join(",", estatusDeliveryList.stream().map(x -> "?").toList()));
            sql.append(")");
            params.addAll(estatusDeliveryList);
        }

        logger.debug("🔍 Ejecutando SQL: {}", sql);

        return jdbcTemplate.query(
                sql.toString(),
                ps -> {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                },
                (rs, rowNum) -> {

                    B2BSeguimientoEntregaOrdenDTO dto = new B2BSeguimientoEntregaOrdenDTO();

                    // 🔹 IDs
                    dto.setIdShopifyOrder(rs.getLong("idShopifyOrder"));
                    dto.setIdSucursalCliente(rs.getLong("idSucursalCliente"));
                    dto.setIdSucursalProveedor(rs.getLong("idSucursalProveedor"));
                    dto.setIdShopify(rs.getString("idShopify"));
                    dto.setIdMarketplace(rs.getLong("idMarketplace"));

                    // 🔹 Orden
                    dto.setOrderNumber(rs.getString("orderNumber"));
                    dto.setName(rs.getString("name"));
                    dto.setFinancialStatus(rs.getString("financialStatus"));
                    dto.setFulfillmentStatus(rs.getString("fulfillmentStatus"));

                    // 🔹 Precios
                    dto.setTotalPrice(rs.getBigDecimal("totalPrice"));
                    dto.setSubtotalPrice(rs.getBigDecimal("subtotalPrice"));
                    dto.setTotalTax(rs.getBigDecimal("totalTax"));
                    dto.setCurrency(rs.getString("currency"));
                    dto.setPrecio(rs.getBigDecimal("precio"));
                    dto.setPrecio_producto(rs.getBigDecimal("precio_producto"));
                    dto.setComision(rs.getBigDecimal("comision"));

                    // 🔹 Cliente
                    dto.setEmail(rs.getString("email"));
                    dto.setCustomerName(rs.getString("customerName"));
                    dto.setShippingAddess1(rs.getString("shippingAddess1"));
                    dto.setShippingAddess2(rs.getString("shippingAddess2"));
                    dto.setShippingCity(rs.getString("shippingCity"));
                    dto.setShippingState(rs.getString("shippingState"));
                    dto.setShippingCountry(rs.getString("shippingCountry"));
                    dto.setShippingPhone(rs.getString("shippingPhone"));
                    dto.setShippingCP(rs.getString("shippingCP"));
                    dto.setShippingContact(rs.getString("shippingContact"));

                    // 🔹 Fechas
                    dto.setCreatedAt(rs.getString("createdAt"));
                    dto.setUpdatedAt(rs.getString("updatedAt"));
                    dto.setFechaEstatusDelivery(rs.getString("fechaEstatusDelivery"));
                    dto.setFechaSolicitud(rs.getString("fechaSolicitud"));

                    // 🔹 Producto
                    dto.setDetailPeso(rs.getInt("detailPeso"));
                    dto.setDetailCantidad(rs.getInt("detailCantidad"));
                    dto.setDetailCodigoAMS(rs.getString("detailCodigoAMS"));
                    dto.setDetailDescripcion(rs.getString("detailDescripcion"));

                    // 🔹 Delivery IDs
                    dto.setIdEstatusOdc(rs.getObject("idEstatusOdc", Integer.class));
                    dto.setIdEstatusDelivery(rs.getObject("idEstatusDelivery", Integer.class));
                    dto.setDescripcionEntrega(rs.getString("descripcionEntrega"));

                    // 🔥 NUEVO (CLAVES)
                    dto.setCveEstatusOdc(rs.getString("cveEstatusOdc"));
                    dto.setCveEstatusDelivery(rs.getString("cveEstatusDelivery"));
                    dto.setCodigoDelivery(rs.getObject("codigoDelivery", Integer.class));

                    // 🔹 Guías
                    dto.setWaybill(rs.getString("waybill"));
                    dto.setTrackingCode(rs.getString("trackingCode"));
                    dto.setRutaGuia(rs.getString("rutaGuia"));
                    dto.setArchivoGuia(rs.getString("archivoGuia"));
                    dto.setReferenceNumber(rs.getString("referenceNumber"));
                    dto.setWaybillDevolution(rs.getString("waybillDevolution"));
                    dto.setTrackingCodeDevolution(rs.getString("trackingCodeDevolution"));

                    dto.setPaqueteria(rs.getString("paqueteria"));

                    return dto;
                });
    }

    public void updateOrigenPaqueteriaEstafeta(
            String orderNumber,
            String waybill) {

        String sql = """
                    UPDATE SHOPIFY_ORDER
                    SET ORIGENPAQUETERIA = 'ESTAFETA',
                        waybill = ?
                    WHERE orderNumber = ?
                """;

        int rows = jdbcTemplate.update(sql, waybill, orderNumber);

        logger.info("[GUIA-UNIFICADA] SHOPIFY_ORDER ORIGENPAQUETERIA=ESTAFETA orderNumber={}, waybill={}, rows={}",
                orderNumber, waybill, rows);
    }

    public void updateEstatusDelivery(
            B2BActualizarEstatusEntregaDTO dto,
            String cveEstatusOdcNuevo,
            boolean esDevolucion) {

        // 🔥 VALIDACIÓN (AQUÍ VA)
        if (cveEstatusOdcNuevo == null) {
            throw new IllegalArgumentException("cveEstatusOdcNuevo es null");
        }
        if (dto.getCveEstatusDelivery() == null) {
            throw new IllegalArgumentException("cveEstatusDelivery es null");
        }

        if (dto.getIdShopifyOrder() == null) {
            throw new IllegalArgumentException("idShopifyOrder es null");
        }
        Long idShopifyOrder = dto.getIdShopifyOrder();

        // 🔥 INSERT HISTORIAL (TODO POR CVE → JOIN A IDS)
        String insertSql = """
                    INSERT INTO SHOPIFY_ORDER_DELIVERY (
                        idShopifyOrder,
                        idEstatusDelivery,
                        idEstatusOdc,
                        fecha_delivery,
                        created_at,
                        created_by,
                        reasonCodeDescription
                    )
                    SELECT
                        ?,
                        ed.idEstatusDelivery,
                        eo.idEstatusOdc,
                        ?,
                        GETDATE(),
                        'SYSTEM_CRON',
                        ?
                    FROM EstatusDelivery ed
                    JOIN EstatusOdc eo ON eo.cveEstatusOdc = ?
                    WHERE ed.cveEstatusDelivery = ?
                """;

        jdbcTemplate.update(insertSql,
                idShopifyOrder,
                dto.getFechaEstatus(),
                dto.getReasonCodeDescription(),
                cveEstatusOdcNuevo, // ✅ CVE ODC
                dto.getCveEstatusDelivery() // ✅ CVE DELIVERY
        );

        // 🔥 UPDATE PRINCIPAL (JOIN DOBLE)
        String updateSql = """
                    UPDATE so
                    SET
                        so.idEstatusDelivery = ed.idEstatusDelivery,
                        so.idEstatusOdc = eo.idEstatusOdc,
                        so.trackingCode = ?,
                        so.descripcionEntrega = ?,
                        so.fechaEstatusDelivery = ?,
                        so.reasonCodeDescription = ?
                    FROM SHOPIFY_ORDER so
                    JOIN EstatusDelivery ed ON ed.cveEstatusDelivery = ?
                    JOIN EstatusOdc eo ON eo.cveEstatusOdc = ?
                    WHERE so.idShopifyOrder = ?
                """;

        jdbcTemplate.update(updateSql,
                dto.getTrackingCode(),
                dto.getDescripcionEntrega(),
                dto.getFechaEstatus(),
                dto.getReasonCodeDescription(),
                dto.getCveEstatusDelivery(), // ✅ CVE DELIVERY
                cveEstatusOdcNuevo, // ✅ CVE ODC
                dto.getIdShopifyOrder());

        // 🔥 UPDATE DEVOLUCIÓN
        if (esDevolucion && dto.getWaybillDevolution() != null) {

            String updateDev = """
                        UPDATE SHOPIFY_ORDER
                        SET waybillDevolution = ? , trackingCodeDevolution = ?
                        WHERE idShopifyOrder = ?
                    """;

            jdbcTemplate.update(updateDev,
                    dto.getWaybillDevolution(),
                    dto.getTrackingCodeDevolution(),
                    dto.getIdShopifyOrder());
        }
    }
}