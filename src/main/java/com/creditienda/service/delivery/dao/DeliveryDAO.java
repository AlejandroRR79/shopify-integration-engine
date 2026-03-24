package com.creditienda.service.delivery.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;

import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.dto.delivery.EstatusDeliveryDTO;
import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.EstatusOdcDTO;

@Repository
public class DeliveryDAO {

    private final JdbcTemplate jdbcTemplate;

    public DeliveryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<B2BSeguimientoEntregaOrdenDTO> findByEstatus(
            List<Integer> estatusOdcList,
            List<Integer> estatusDeliveryList) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                idShopifyOrder,
                idSucursalCliente,
                idSucursalProveedor,
                idShopify,
                email,
                name,
                financialStatus,
                fulfillmentStatus,
                totalPrice,
                subtotalPrice,
                totalTax,
                currency,
                orderNumber,
                createdAt,
                updatedAt,
                customerName,
                shippingAddess1,
                shippingAddess2,
                shippingCity,
                shippingCountry,
                shippingPhone,
                shippingCP,
                shippingState,
                shippingContact,
                detailPeso,
                detailCantidad,
                detailCodigoAMS,
                detailDescripcion,
                idMarketplace,
                precio,
                precio_producto,
                idEstatusOdc,
                comision,
                idEstatusDelivery,
                fechaEstatusDelivery,
                originResultCode,
                destinationsResultCode,
                waybill,
                trackingCode,
                rutaGuia,
                archivoGuia,
                destinationAddress,
                referenceNumber,
                fechaSolicitud,
                descripcionEntrega,
                shippingAddress,
                shippingNumExt,
                shippingColony,
                reasonCodeDescription,
                isInsurance,
                paqueteria
                FROM SHOPIFY_ORDER
                WHERE idEstatusOdc IN (
                """);

        // 🔥 IN dinámico ODC
        sql.append(String.join(",", estatusOdcList.stream().map(x -> "?").toList())).append(")");

        List<Object> params = new ArrayList<>(estatusOdcList);

        // 🔥 IN dinámico DELIVERY (opcional)
        if (estatusDeliveryList != null && !estatusDeliveryList.isEmpty()) {
            sql.append(" AND idEstatusDelivery IN (");
            sql.append(String.join(",", estatusDeliveryList.stream().map(x -> "?").toList()));
            sql.append(")");
            params.addAll(estatusDeliveryList);
        }

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

                    // 🔹 Delivery
                    dto.setIdEstatusOdc(rs.getInt("idEstatusOdc"));
                    dto.setIdEstatusDelivery(rs.getInt("idEstatusDelivery"));
                    dto.setDescripcionEntrega(rs.getString("descripcionEntrega"));
                    dto.setOriginResultCode(rs.getString("originResultCode"));
                    dto.setDestinationsResultCode(rs.getString("destinationsResultCode"));

                    // 🔹 Guías
                    dto.setWaybill(rs.getString("waybill"));
                    dto.setTrackingCode(rs.getString("trackingCode"));
                    dto.setRutaGuia(rs.getString("rutaGuia"));
                    dto.setArchivoGuia(rs.getString("archivoGuia"));
                    dto.setReferenceNumber(rs.getString("referenceNumber"));

                    // 🔥 Estatus ODC (mínimo viable sin romper B2B)
                    EstatusOdcDTO estatus = new EstatusOdcDTO();
                    estatus.setIdEstatusOdc(rs.getInt("idEstatusOdc"));
                    estatus.setIsOdc(true);

                    dto.setEstatusOdc(estatus);
                    // 🔥 Estatus DELIVERY (nuevo)
                    EstatusDeliveryDTO estatusDelivery = new EstatusDeliveryDTO();
                    estatusDelivery.setDescripcion(rs.getString("descripcionEntrega"));
                    Integer idDelivery = rs.getObject("idEstatusDelivery") != null
                            ? rs.getInt("idEstatusDelivery")
                            : null;

                    estatusDelivery.setIdEstatusDelivery(idDelivery);

                    dto.setEstatusDeliveryDTO(estatusDelivery);
                    return dto;
                });
    }

    public void updateEstatusDelivery(B2BActualizarEstatusEntregaDTO dto) {

        // 🔍 1. Validación básica (evita basura)
        if (dto.getOrderNumber() == null) {
            throw new IllegalArgumentException("orderNumber es requerido");
        }

        // 🔥 2. INSERT HISTORIAL
        String insertSql = """
                    INSERT INTO SHOPIFY_ORDER_HISTORY (
                        orderNumber,
                        trackingCode,
                        codigoEntrega,
                        descripcionEntrega,
                        fechaEstatus,
                        reasonCodeDescription,
                        createdAt
                    )
                    SELECT ?, ?, ?, ?, ?, ?, getdate()
                    WHERE NOT EXISTS (
                        SELECT 1 FROM SHOPIFY_ORDER_HISTORY
                        WHERE orderNumber = ?
                        AND codigoEntrega = ?
                    )
                """;

        jdbcTemplate.update(insertSql,
                dto.getOrderNumber(),
                dto.getTrackingCode(),
                dto.getCodigoEntrega(),
                dto.getDescripcionEntrega(),
                dto.getFechaEstatus(),
                dto.getReasonCodeDescription(),
                dto.getOrderNumber(),
                dto.getCodigoEntrega());

        // 🔥 3. UPDATE PRINCIPAL
        String updateSql = """
                    UPDATE SHOPIFY_ORDER
                    SET
                        trackingCode = ?,
                        idEstatusDelivery = ?,
                        descripcionEntrega = ?,
                        fechaEstatusDelivery = ?,
                        reasonCodeDescription = ?
                    WHERE orderNumber = ?
                """;

        jdbcTemplate.update(updateSql,
                dto.getTrackingCode(),
                dto.getCodigoEntrega(),
                dto.getDescripcionEntrega(),
                dto.getFechaEstatus(),
                dto.getReasonCodeDescription(),
                dto.getOrderNumber());
    }
}