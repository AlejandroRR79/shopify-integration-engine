package com.creditienda.controller.skydropx;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditienda.dto.estafeta.guia.WayBillRequestDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationProcessResponseDTO;
import com.creditienda.service.skydropx.SkyDropXQuotationService;

/**
 * Controller encargado de generar quotations
 * en SkyDropX.
 *
 * IMPORTANTE:
 * Este endpoint YA NO espera completion
 * de tarifas.
 *
 * El flujo actual es:
 *
 * 1. Generar quotation.
 * 2. Obtener quotationId.
 * 3. Regresar PROCESSING inmediatamente.
 *
 * Próximamente:
 * - Async polling.
 * - Persistencia BD.
 * - Recovery scheduler.
 */
@RestController
@RequestMapping("/api/secure/skydropx")
public class SkyDropXQuotationController {

    private final SkyDropXQuotationService skyDropXQuotationService;

    public SkyDropXQuotationController(
            SkyDropXQuotationService skyDropXQuotationService) {

        this.skyDropXQuotationService = skyDropXQuotationService;
    }

    /**
     * Genera quotation en SkyDropX.
     *
     * IMPORTANTE:
     * Todavía NO se espera completion.
     * Todavía NO se hace polling.
     *
     * Se regresa quotationId inmediatamente.
     *
     * @param request payload original Estafeta
     * @return quotationId + status PROCESSING
     */
    @PostMapping("/quotations")
    public ResponseEntity<SkyDropXQuotationProcessResponseDTO> generateQuotation(
            @RequestBody WayBillRequestDTO request) {

        /**
         * Generar quotation SkyDropX.
         */
        SkyDropXQuotationProcessResponseDTO response = skyDropXQuotationService.generateQuotation(
                request);

        /**
         * Regresar quotationId inmediatamente.
         */
        return ResponseEntity.ok(
                response);
    }
}