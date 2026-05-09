package com.creditienda.service.skydropx.mapper;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationRequestDTO;

/**
 * Mapper encargado de transformar
 * payload Estafeta hacia
 * quotation request SkyDropX.
 */
@Component
public class SkyDropXQuotationMapper {

    /**
     * Carriers configurados
     * desde application.properties.
     */
    @Value("${skydropx.requested-carriers}")
    private String requestedCarriers;

    /**
     * Transformación:
     * Estafeta -> SkyDropX quotation.
     *
     * @param request payload Estafeta
     * @return quotation request SkyDropX
     */
    public SkyDropXQuotationRequestDTO map(
            WayBillRequestDTO request) {

        SkyDropXQuotationRequestDTO dto = new SkyDropXQuotationRequestDTO();

        SkyDropXQuotationRequestDTO.Quotation quotation = new SkyDropXQuotationRequestDTO.Quotation();

        /**
         * =====================================
         * ADDRESS FROM
         * =====================================
         */
        SkyDropXQuotationRequestDTO.Address addressFrom = new SkyDropXQuotationRequestDTO.Address();

        addressFrom.setCountry_code(
                "MX");

        addressFrom.setPostal_code(
                request.getLabelDefinition()
                        .getLocation()
                        .getOrigin()
                        .getAddress()
                        .getZipCode());

        addressFrom.setArea_level1(
                request.getLabelDefinition()
                        .getLocation()
                        .getOrigin()
                        .getAddress()
                        .getStateAbbName());

        addressFrom.setArea_level2(
                request.getLabelDefinition()
                        .getLocation()
                        .getOrigin()
                        .getAddress()
                        .getTownshipName());

        addressFrom.setArea_level3(
                request.getLabelDefinition()
                        .getLocation()
                        .getOrigin()
                        .getAddress()
                        .getSettlementName());

        /**
         * =====================================
         * ADDRESS TO
         * =====================================
         */
        SkyDropXQuotationRequestDTO.Address addressTo = new SkyDropXQuotationRequestDTO.Address();

        addressTo.setCountry_code(
                "MX");

        addressTo.setPostal_code(
                request.getLabelDefinition()
                        .getLocation()
                        .getDestination()
                        .getHomeAddress()
                        .getAddress()
                        .getZipCode());

        addressTo.setArea_level1(
                request.getLabelDefinition()
                        .getLocation()
                        .getDestination()
                        .getHomeAddress()
                        .getAddress()
                        .getStateAbbName());

        addressTo.setArea_level2(
                request.getLabelDefinition()
                        .getLocation()
                        .getDestination()
                        .getHomeAddress()
                        .getAddress()
                        .getTownshipName());

        addressTo.setArea_level3(
                request.getLabelDefinition()
                        .getLocation()
                        .getDestination()
                        .getHomeAddress()
                        .getAddress()
                        .getSettlementName());

        /**
         * =====================================
         * PARCEL
         * =====================================
         */
        SkyDropXQuotationRequestDTO.Parcel parcel = new SkyDropXQuotationRequestDTO.Parcel();

        parcel.setLength(
                Double.parseDouble(
                        request.getLabelDefinition()
                                .getItemDescription()
                                .getLength()));

        parcel.setWidth(
                Double.parseDouble(
                        request.getLabelDefinition()
                                .getItemDescription()
                                .getWidth()));

        parcel.setHeight(
                Double.parseDouble(
                        request.getLabelDefinition()
                                .getItemDescription()
                                .getHeight()));

        parcel.setWeight(
                Double.parseDouble(
                        request.getLabelDefinition()
                                .getItemDescription()
                                .getWeight()));

        /**
         * =====================================
         * REQUESTED CARRIERS
         * =====================================
         */
        List<String> carriers = Arrays.asList(
                requestedCarriers.split(","));

        /**
         * =====================================
         * BUILD QUOTATION
         * =====================================
         */
        quotation.setAddress_from(
                addressFrom);

        quotation.setAddress_to(
                addressTo);

        quotation.setParcels(
                List.of(parcel));

        quotation.setRequested_carriers(
                carriers);

        dto.setQuotation(
                quotation);

        return dto;
    }
}