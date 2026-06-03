package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;

/**
 * Servicio encargado de procesar
 * quotations consolidadas de SkyDropX.
 *
 * Responsabilidades:
 * - Consultar quotation consolidada.
 * - Validar si la quotation terminó.
 * - Invocar selector de tarifas.
 * - Orquestar siguientes pasos.
 *
 * Este servicio NO recibe webhooks directamente.
 * Debe ser invocado desde SkyDropXWebhookService.
 */
@Service
public class SkyDropXQuotationProcessorService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXQuotationProcessorService.class);

    /**
     * Servicio encargado de consultar
     * quotations consolidadas en SkyDropX.
     */
    private final SkyDropXQuotationClientService quotationClientService;

    /**
     * Servicio encargado de seleccionar
     * la mejor tarifa.
     */
    private final SkyDropXRateSelectionService rateSelectionService;

    public SkyDropXQuotationProcessorService(
            SkyDropXQuotationClientService quotationClientService,
            SkyDropXRateSelectionService rateSelectionService) {

        this.quotationClientService = quotationClientService;

        this.rateSelectionService = rateSelectionService;
    }

    /**
     * Procesa una quotation consolidada.
     *
     * Flujo:
     * 1. Consultar quotation.
     * 2. Validar is_completed.
     * 3. Seleccionar mejor tarifa.
     * 4. Preparar siguientes pasos.
     *
     * @param quotationId     quotation a consultar
     * @param estafetaRequest payload original Estafeta
     */
    public void processQuotation(
            String quotationId,
            WayBillRequestDTO estafetaRequest) {

        SkyDropXQuotationResponseDTO quotation = quotationClientService
                .getQuotation(
                        quotationId);

        /**
         * Validar si la quotation ya terminó
         * procesamiento en SkyDropX.
         */
        if (quotation == null
                || !Boolean.TRUE.equals(
                        quotation.getIsCompleted())) {

            log.info(
                    "[SKYDROPX-PROCESSOR] quotation aun no completada quotationId={}",
                    quotationId);

            return;
        }

        /**
         * Obtener mejor tarifa
         * de acuerdo a reglas negocio.
         */
        Rate bestRate = rateSelectionService
                .processQuotation(
                        quotationId);

        if (bestRate == null) {

            log.warn(
                    "[SKYDROPX-PROCESSOR] no fue posible seleccionar tarifa quotationId={}",
                    quotationId);

            return;
        }

        log.info(
                "[SKYDROPX-PROCESSOR] mejor tarifa provider={} total={} days={}",
                bestRate.getProviderName(),
                bestRate.getTotal(),
                bestRate.getDays());

        /**
         * Próximos pasos:
         * - persistir selección
         * - generar shipment
         * - proteger shipment
         * - responder a B2B
         */
    }
}