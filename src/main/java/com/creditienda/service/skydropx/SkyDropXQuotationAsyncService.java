package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;

/**
 * Servicio encargado de procesar
 * quotations en background.
 *
 * IMPORTANTE:
 * Este flujo reemplaza dependencia
 * principal del webhook.
 */
@Service
public class SkyDropXQuotationAsyncService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXQuotationAsyncService.class);

    private final SkyDropXRateSelectionService skyDropXRateSelectionService;

    private final SkyDropXShipmentService skyDropXShipmentService;

    public SkyDropXQuotationAsyncService(
            SkyDropXRateSelectionService skyDropXRateSelectionService,
            SkyDropXShipmentService skyDropXShipmentService) {

        this.skyDropXRateSelectionService = skyDropXRateSelectionService;

        this.skyDropXShipmentService = skyDropXShipmentService;
    }

    /**
     * Procesar quotation async.
     *
     * Flujo:
     * 1. Polling GET quotation.
     * 2. Esperar is_completed=true.
     * 3. Filtrar tarifas válidas.
     * 4. Seleccionar mejor tarifa.
     * 5. Generar shipment.
     *
     * @param quotationId quotation generado
     * @param request     payload original Estafeta
     */
    @Async("skydropxExecutor")
    public void processQuotationAsync(
            String quotationId,
            WayBillRequestDTO request) {

        try {

            log.info(
                    "[SKYDROPX-ASYNC] iniciando processing quotationId={}",
                    quotationId);

            /**
             * Obtener mejor tarifa.
             */
            Rate selectedRate = skyDropXRateSelectionService
                    .processQuotation(
                            quotationId);

            log.info(
                    "[SKYDROPX-ASYNC] selectedRateId={}",
                    selectedRate.getId());

            /**
             * Generar shipment real.
             */
            skyDropXShipmentService
                    .generateShipment(
                            request,
                            selectedRate);

            log.info(
                    "[SKYDROPX-ASYNC] processing completado quotationId={}",
                    quotationId);

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-ASYNC] error processing quotationId={}",
                    quotationId,
                    ex);
        }
    }
}