package com.creditienda.dto.delivery;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({
        "rawData",
        "destinationAddress"
})
public class B2BSeguimientoEntregaOrdenDTO {

    // 🔹 Identificadores
    private Long idShopifyOrder;
    private Long idSucursalCliente;
    private Long idSucursalProveedor;
    private String idShopify;
    private Long idMarketplace;

    // 🔹 Orden
    private String orderNumber;
    private String name;
    private String financialStatus;
    private String fulfillmentStatus;

    // 🔹 Precios
    private BigDecimal totalPrice;
    private BigDecimal subtotalPrice;
    private BigDecimal totalTax;
    private String currency;
    private BigDecimal precio;
    private BigDecimal precio_producto;
    private BigDecimal comision;

    // 🔹 Cliente / envío
    private String email;
    private String customerName;
    private String shippingAddess1;
    private String shippingAddess2;
    private String shippingCity;
    private String shippingState;
    private String shippingCountry;
    private String shippingPhone;
    private String shippingCP;
    private String shippingContact;

    // 🔹 Fechas
    private String createdAt;
    private String updatedAt;
    private String fechaEstatusDelivery;
    private String fechaSolicitud;

    // 🔹 Producto
    private Integer detailPeso;
    private Integer detailCantidad;
    private String detailCodigoAMS;
    private String detailDescripcion;

    // 🔹 Delivery
    private Integer idEstatusOdc;
    private Integer idEstatusDelivery;
    private String descripcionEntrega;
    private String originResultCode;
    private String destinationsResultCode;

    // 🔹 Guías
    private String waybill;
    private String trackingCode;
    private String rutaGuia;
    private String archivoGuia;
    private String referenceNumber;

    // 🔹 Objetos anidados
    private EstatusOdcDTO estatusOdc;
    private Object estatusDelivery;

    // ================== GETTERS & SETTERS ==================

    public Long getIdShopifyOrder() {
        return idShopifyOrder;
    }

    public void setIdShopifyOrder(Long idShopifyOrder) {
        this.idShopifyOrder = idShopifyOrder;
    }

    public Long getIdSucursalCliente() {
        return idSucursalCliente;
    }

    public void setIdSucursalCliente(Long idSucursalCliente) {
        this.idSucursalCliente = idSucursalCliente;
    }

    public Long getIdSucursalProveedor() {
        return idSucursalProveedor;
    }

    public void setIdSucursalProveedor(Long idSucursalProveedor) {
        this.idSucursalProveedor = idSucursalProveedor;
    }

    public String getIdShopify() {
        return idShopify;
    }

    public void setIdShopify(String idShopify) {
        this.idShopify = idShopify;
    }

    public Long getIdMarketplace() {
        return idMarketplace;
    }

    public void setIdMarketplace(Long idMarketplace) {
        this.idMarketplace = idMarketplace;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFinancialStatus() {
        return financialStatus;
    }

    public void setFinancialStatus(String financialStatus) {
        this.financialStatus = financialStatus;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getSubtotalPrice() {
        return subtotalPrice;
    }

    public void setSubtotalPrice(BigDecimal subtotalPrice) {
        this.subtotalPrice = subtotalPrice;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getPrecio_producto() {
        return precio_producto;
    }

    public void setPrecio_producto(BigDecimal precio_producto) {
        this.precio_producto = precio_producto;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public void setComision(BigDecimal comision) {
        this.comision = comision;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getShippingAddess1() {
        return shippingAddess1;
    }

    public void setShippingAddess1(String shippingAddess1) {
        this.shippingAddess1 = shippingAddess1;
    }

    public String getShippingAddess2() {
        return shippingAddess2;
    }

    public void setShippingAddess2(String shippingAddess2) {
        this.shippingAddess2 = shippingAddess2;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingCP() {
        return shippingCP;
    }

    public void setShippingCP(String shippingCP) {
        this.shippingCP = shippingCP;
    }

    public String getShippingContact() {
        return shippingContact;
    }

    public void setShippingContact(String shippingContact) {
        this.shippingContact = shippingContact;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFechaEstatusDelivery() {
        return fechaEstatusDelivery;
    }

    public void setFechaEstatusDelivery(String fechaEstatusDelivery) {
        this.fechaEstatusDelivery = fechaEstatusDelivery;
    }

    public String getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(String fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public Integer getDetailPeso() {
        return detailPeso;
    }

    public void setDetailPeso(Integer detailPeso) {
        this.detailPeso = detailPeso;
    }

    public Integer getDetailCantidad() {
        return detailCantidad;
    }

    public void setDetailCantidad(Integer detailCantidad) {
        this.detailCantidad = detailCantidad;
    }

    public String getDetailCodigoAMS() {
        return detailCodigoAMS;
    }

    public void setDetailCodigoAMS(String detailCodigoAMS) {
        this.detailCodigoAMS = detailCodigoAMS;
    }

    public String getDetailDescripcion() {
        return detailDescripcion;
    }

    public void setDetailDescripcion(String detailDescripcion) {
        this.detailDescripcion = detailDescripcion;
    }

    public Integer getIdEstatusOdc() {
        return idEstatusOdc;
    }

    public void setIdEstatusOdc(Integer idEstatusOdc) {
        this.idEstatusOdc = idEstatusOdc;
    }

    public Integer getIdEstatusDelivery() {
        return idEstatusDelivery;
    }

    public void setIdEstatusDelivery(Integer idEstatusDelivery) {
        this.idEstatusDelivery = idEstatusDelivery;
    }

    public String getDescripcionEntrega() {
        return descripcionEntrega;
    }

    public void setDescripcionEntrega(String descripcionEntrega) {
        this.descripcionEntrega = descripcionEntrega;
    }

    public String getOriginResultCode() {
        return originResultCode;
    }

    public void setOriginResultCode(String originResultCode) {
        this.originResultCode = originResultCode;
    }

    public String getDestinationsResultCode() {
        return destinationsResultCode;
    }

    public void setDestinationsResultCode(String destinationsResultCode) {
        this.destinationsResultCode = destinationsResultCode;
    }

    public String getWaybill() {
        return waybill;
    }

    public void setWaybill(String waybill) {
        this.waybill = waybill;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getRutaGuia() {
        return rutaGuia;
    }

    public void setRutaGuia(String rutaGuia) {
        this.rutaGuia = rutaGuia;
    }

    public String getArchivoGuia() {
        return archivoGuia;
    }

    public void setArchivoGuia(String archivoGuia) {
        this.archivoGuia = archivoGuia;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public EstatusOdcDTO getEstatusOdc() {
        return estatusOdc;
    }

    public void setEstatusOdc(EstatusOdcDTO estatusOdc) {
        this.estatusOdc = estatusOdc;
    }

    public Object getEstatusDelivery() {
        return estatusDelivery;
    }

    public void setEstatusDelivery(Object estatusDelivery) {
        this.estatusDelivery = estatusDelivery;
    }
}
