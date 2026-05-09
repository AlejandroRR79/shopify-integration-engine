package com.creditienda.service.skydropx;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO;
import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO.Rate;

/**
 * Servicio encargado de aplicar las reglas de negocio
 * para seleccionar la mejor tarifa de SkyDropX.
 *
 * Reglas consideradas:
 * - success=true
 * - status != pending
 * - carrier permitido
 * - tipo de servicio compatible
 * - prioridad configurable:
 * PRICE
 * TIME
 *
 * Este servicio:
 * - consulta quotation consolidada
 * - realiza polling controlado
 * - evalúa y selecciona tarifas
 */
@Service
public class SkyDropXRateSelectionService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXRateSelectionService.class);

    /**
     * Prioridad de selección:
     * PRICE -> menor precio primero
     * TIME -> menor tiempo primero
     */
    @Value("${skydropx.selection.priority}")
    private String priority;

    /**
     * Lista de carriers permitidos.
     *
     * Se reutiliza la misma propiedad utilizada
     * al generar quotations.
     */
    @Value("${skydropx.requested-carriers}")
    private List<String> allowedCarriers;

    /**
     * Tipos de servicio permitidos.
     *
     * Ejemplo:
     * standard
     * express
     */
    @Value("${skydropx.selection.allowed-service-types}")
    private List<String> allowedServiceTypes;

    /**
     * Lock temporal en memoria
     * para evitar procesar
     * mismo quotationId simultáneamente.
     *
     * TEMPORAL:
     * Posteriormente migrará
     * a control distribuido en BD.
     */
    private final ConcurrentHashMap<String, Boolean> processingMap = new ConcurrentHashMap<>();

    /**
     * Cliente encargado de consultar
     * quotations consolidadas SkyDropX.
     */
    private final SkyDropXQuotationClientService quotationClientService;

    public SkyDropXRateSelectionService(
            SkyDropXQuotationClientService quotationClientService) {

        this.quotationClientService = quotationClientService;
    }

    /**
     * Método principal encargado de:
     * 1. Realizar polling quotation.
     * 2. Esperar completion.
     * 3. Filtrar rates válidos.
     * 4. Aplicar reglas negocio.
     * 5. Seleccionar mejor tarifa.
     *
     * @param quotationId quotationId SkyDropX
     * @return mejor tarifa encontrada
     */
    public Rate processQuotation(
            String quotationId) {

        /**
         * Evitar doble procesamiento.
         */
        if (processingMap.putIfAbsent(
                quotationId,
                true) != null) {

            log.info(
                    "[SKYDROPX-RATE-SELECTION] quotation ya procesandose={}",
                    quotationId);

            return null;
        }

        try {

            SkyDropXQuotationResponseDTO quotation = null;

            /**
             * Polling controlado.
             *
             * Se realizan hasta 3 intentos
             * esperando completion quotation.
             */
            for (int i = 1; i <= 3; i++) {

                quotation = quotationClientService
                        .getQuotation(
                                quotationId);

                if (quotation != null
                        && Boolean.TRUE.equals(
                                quotation.getIsCompleted())) {

                    log.info(
                            "[SKYDROPX-RATE-SELECTION] quotation completada intento={}",
                            i);

                    break;
                }

                log.info(
                        "[SKYDROPX-RATE-SELECTION] quotation pendiente intento={}",
                        i);

                Thread.sleep(10000);
            }

            /**
             * Validar quotation final.
             */
            if (quotation == null
                    || !Boolean.TRUE.equals(
                            quotation.getIsCompleted())) {

                log.warn(
                        "[SKYDROPX-RATE-SELECTION] quotation no completada quotationId={}",
                        quotationId);

                return null;
            }

            /**
             * Filtrar tarifas válidas.
             */
            List<Rate> validRates = quotation.getRates()
                    .stream()

                    /**
                     * Validar item.
                     */
                    .filter(item -> item != null)

                    /**
                     * Solo tarifas exitosas.
                     */
                    .filter(item -> Boolean.TRUE.equals(
                            item.getSuccess()))

                    /**
                     * Excluir tarifas pendientes.
                     */
                    .filter(item -> !"pending".equalsIgnoreCase(
                            item.getStatus()))

                    /**
                     * Carrier permitido.
                     */
                    .filter(this::isAllowedCarrier)

                    /**
                     * Tipo de servicio válido.
                     */
                    // .filter(this::isValidServiceType)

                    .collect(Collectors.toList());

            if (validRates.isEmpty()) {

                throw new RuntimeException(
                        "No existen tarifas válidas para seleccionar");
            }

            /**
             * Construir comparator dinámico.
             */
            Comparator<Rate> comparator = buildComparator();

            /**
             * Seleccionar mejor tarifa.
             */
            Rate selectedRate = validRates
                    .stream()
                    .min(comparator)
                    .orElseThrow();

            /**
             * Log mejor tarifa seleccionada.
             */
            log.info(
                    "[SKYDROPX-BEST-RATE] quotationId={} provider={} service={} total={} days={}",
                    quotationId,
                    selectedRate.getProviderName(),
                    selectedRate.getProviderServiceName(),
                    selectedRate.getTotal(),
                    selectedRate.getDays());

            return selectedRate;

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-RATE-SELECTION] error seleccionando tarifa",
                    ex);

            throw new RuntimeException(
                    ex.getMessage());

        } finally {

            /**
             * Liberar lock quotation.
             */
            processingMap.remove(
                    quotationId);
        }
    }

    /**
     * Construye comparator dinámico
     * dependiendo prioridad configurada.
     *
     * PRICE:
     * menor precio
     * luego menor tiempo
     *
     * TIME:
     * menor tiempo
     * luego menor precio
     *
     * @return comparator configurado
     */
    private Comparator<Rate> buildComparator() {

        if ("TIME".equalsIgnoreCase(
                priority)) {

            return Comparator
                    .comparing(
                            Rate::getDays)
                    .thenComparing(
                            item -> new BigDecimal(
                                    item.getTotal()));
        }

        return Comparator
                .comparing(
                        (Rate item) -> new BigDecimal(
                                item.getTotal()))
                .thenComparing(
                        Rate::getDays);
    }

    /**
     * Validar carrier permitido.
     *
     * @param item tarifa evaluada
     * @return true si carrier válido
     */
    private boolean isAllowedCarrier(
            Rate item) {

        String provider = item.getProviderName();

        if (provider == null) {

            return true;
        }

        return allowedCarriers
                .stream()
                .map(String::toLowerCase)
                .anyMatch(carrier -> carrier.equals(
                        provider.toLowerCase()));
    }

    /**
     * Validar tipo servicio permitido.
     *
     * TEMPORAL:
     * Actualmente se valida
     * únicamente contra properties.
     *
     * Posteriormente:
     * se validará contra metadata
     * persistida quotation/request.
     *
     * @param item tarifa evaluada
     * @return true si servicio válido
     */
    private boolean isValidServiceType(
            Rate item) {

        String providerServiceCode = item.getProviderServiceCode();

        if (providerServiceCode == null) {

            return false;
        }

        return allowedServiceTypes
                .stream()
                .map(String::toLowerCase)
                .anyMatch(service -> providerServiceCode
                        .toLowerCase()
                        .contains(service));
    }
}