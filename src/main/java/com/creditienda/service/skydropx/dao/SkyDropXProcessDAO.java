package com.creditienda.service.skydropx.dao;

import java.util.List;
import java.util.Optional;

import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;

public interface SkyDropXProcessDAO {

        Long createProcess(SkyDropXProcessRecord processRecord);

        void supersedePreviousAndCreate(
                        String orderNumber,
                        SkyDropXProcessRecord processRecord);

        void updateQuotationCompleted(
                        String quotationId,
                        String quotationRawJson);

        void updateSelectedRate(
                        String quotationId,
                        String selectedRateId,
                        String selectedRateJson);

        void updateShipment(
                        String quotationId,
                        String shipmentId,
                        String shipmentRawJson,
                        String trackingNumber,
                        String labelUrl);

        void completeShipmentAndOrder(
                        String quotationId,
                        String shipmentId,
                        String shipmentRawJson,
                        String trackingNumber,
                        String labelUrl);

        void markCompleted(String quotationId);

        void updateShopifyOrderGuia(
                        String quotationId,
                        String waybill,
                        String trackingCode,
                        String rutaGuia);

        void markFailed(
                        String quotationId,
                        String errorMessage);

        void markSupersededByOrderNumber(
                        String orderNumber);

        Optional<SkyDropXProcessRecord> findByQuotationId(
                        String quotationId);

        List<SkyDropXProcessRecord> findStuckProcesses(
                        int stuckMinutes,
                        int maxRetries);

        void incrementRetryCount(String quotationId);

}