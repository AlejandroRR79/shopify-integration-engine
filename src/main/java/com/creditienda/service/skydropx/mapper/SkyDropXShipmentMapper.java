package com.creditienda.service.skydropx.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.dto.estafeta.guia.AddressDTO;
import com.creditienda.dto.estafeta.guia.ContactDTO;
import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXShipmentRequestDTO;

/**
 * Mapper encargado de transformar
 * payload original Estafeta
 * a shipment SkyDropX.
 */
@Component
public class SkyDropXShipmentMapper {

    @Value("${skydropx.shipment.printing-format}")
    private String printingFormat;

    @Value("${skydropx.shipment.package-type}")
    private String packageType;

    @Value("${skydropx.shipment.country-code}")
    private String countryCode;

    @Value("${skydropx.shipment.consignment-note}")
    private String consignmentNote;

    /**
     * Construir shipment request.
     *
     * @param request        payload original
     * @param selectedRateId rate seleccionado
     * @return shipment request
     */
    public SkyDropXShipmentRequestDTO map(
            WayBillRequestDTO request,
            String selectedRateId) {

        SkyDropXShipmentRequestDTO root = new SkyDropXShipmentRequestDTO();

        SkyDropXShipmentRequestDTO.Shipment shipment = new SkyDropXShipmentRequestDTO.Shipment();

        /**
         * =====================================
         * RATE
         * =====================================
         */
        shipment.setRate_id(
                selectedRateId);

        /**
         * =====================================
         * PRINTING FORMAT
         * =====================================
         */
        shipment.setPrinting_format(
                printingFormat);

        /**
         * =====================================
         * ORIGIN
         * =====================================
         */
        ContactDTO originContact = request.getLabelDefinition()
                .getLocation()
                .getOrigin()
                .getContact();

        AddressDTO originAddress = request.getLabelDefinition()
                .getLocation()
                .getOrigin()
                .getAddress();

        SkyDropXShipmentRequestDTO.Address addressFrom = new SkyDropXShipmentRequestDTO.Address();

        addressFrom.setStreet1(
                originAddress.getRoadName()
                        + " "
                        + originAddress.getExternalNum());

        addressFrom.setName(
                originContact.getContactName());

        addressFrom.setCompany(
                originContact.getCorporateName());

        addressFrom.setPhone(
                originContact.getTelephone());

        addressFrom.setEmail(
                originContact.getEmail());

        /**
         * Reference requerida.
         */
        addressFrom.setReference(
                originAddress.getAddressReference() != null
                        ? originAddress.getAddressReference()
                        : "ORIGEN");

        shipment.setAddress_from(
                addressFrom);

        /**
         * =====================================
         * DESTINATION
         * =====================================
         */
        ContactDTO destinationContact = request.getLabelDefinition()
                .getLocation()
                .getDestination()
                .getHomeAddress()
                .getContact();

        AddressDTO destinationAddress = request.getLabelDefinition()
                .getLocation()
                .getDestination()
                .getHomeAddress()
                .getAddress();

        SkyDropXShipmentRequestDTO.Address addressTo = new SkyDropXShipmentRequestDTO.Address();

        addressTo.setStreet1(
                destinationAddress.getRoadName()
                        + " "
                        + destinationAddress.getExternalNum());

        addressTo.setName(
                destinationContact.getContactName());

        addressTo.setCompany(
                destinationContact.getCorporateName());

        addressTo.setPhone(
                destinationContact.getTelephone());

        /**
         * Email requerido.
         */
        addressTo.setEmail(
                destinationContact.getEmail() != null
                        ? destinationContact.getEmail()
                        : "noemail@skydropx.com");

        /**
         * Referencia negocio.
         */
        addressTo.setReference(
                request.getLabelDefinition()
                        .getWayBillDocument()
                        .getReferenceNumber());

        shipment.setAddress_to(
                addressTo);

        /**
         * =====================================
         * PACKAGE
         * =====================================
         */
        List<SkyDropXShipmentRequestDTO.PackageDetail> packages = new ArrayList<>();

        SkyDropXShipmentRequestDTO.PackageDetail packageDetail = new SkyDropXShipmentRequestDTO.PackageDetail();

        packageDetail.setPackage_number(
                "1");

        /**
         * Seguro paquete.
         */
        packageDetail.setPackage_protected(
                request.getLabelDefinition()
                        .getServiceConfiguration()
                        .getIsInsurance());

        /**
         * Valor declarado.
         */
        String declaredValue = request.getLabelDefinition()
                .getServiceConfiguration()
                .getInsurance()
                .getDeclaredValue();

        packageDetail.setDeclared_value(
                new BigDecimal(
                        declaredValue));

        /**
         * Carta porte ID requerido.
         */
        packageDetail.setConsignment_note(
                consignmentNote);

        /**
         * Tipo paquete.
         */
        packageDetail.setPackage_type(
                packageType);

        /**
         * =====================================
         * PRODUCTS
         * =====================================
         */
        List<SkyDropXShipmentRequestDTO.Product> products = new ArrayList<>();

        SkyDropXShipmentRequestDTO.Product product = new SkyDropXShipmentRequestDTO.Product();

        /**
         * Descripción producto.
         */
        product.setName(
                request.getLabelDefinition()
                        .getServiceConfiguration()
                        .getInsurance()
                        .getContentDescription());

        /**
         * Cantidad temporal.
         */
        product.setQuantity(
                1);

        /**
         * Precio producto.
         */
        product.setPrice(
                new BigDecimal(
                        declaredValue));

        /**
         * SKU negocio.
         */
        product.setSku(
                request.getLabelDefinition()
                        .getWayBillDocument()
                        .getReferenceNumber());

        /**
         * País.
         */
        product.setCountry_code(
                countryCode);

        products.add(
                product);

        packageDetail.setProducts(
                products);

        packages.add(
                packageDetail);

        shipment.setPackages(
                packages);

        root.setShipment(
                shipment);

        return root;
    }
}