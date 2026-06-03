package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.skydropx.SkyDropXQuotationResponseDTO;

/**
 * Servicio encargado de consultar
 * quotations consolidadas en SkyDropX.
 *
 * Responsabilidad:
 * - consumir GET quotation/{id}
 * - obtener quotation consolidada
 * - devolver payload completo
 */
@Service
public class SkyDropXQuotationClientService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXQuotationClientService.class);

    @Value("${skydropx.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private final SkyDropXTokenService tokenService;

    public SkyDropXQuotationClientService(
            RestTemplate restTemplate,
            SkyDropXTokenService tokenService) {

        this.restTemplate = restTemplate;

        this.tokenService = tokenService;
    }

    /**
     * Consulta quotation consolidada.
     *
     * Endpoint:
     * GET /api/v1/quotations/{id}
     *
     * @param quotationId quotation a consultar
     * @return quotation completa
     */
    public SkyDropXQuotationResponseDTO getQuotation(
            String quotationId) {

        try {

            /**
             * Obtener token OAuth.
             */
            String token = tokenService
                    .getAccessToken();

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(
                    token);

            HttpEntity<Void> entity = new HttpEntity<>(
                    headers);

            /**
             * Construir endpoint quotation.
             */
            String url = baseUrl
                    + "/api/v1/quotations/"
                    + quotationId;

            log.info(
                    "[SKYDROPX-GET-QUOTATION] url={}",
                    url);

            /**
             * Consumir quotation consolidada.
             */
            ResponseEntity<SkyDropXQuotationResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    SkyDropXQuotationResponseDTO.class);

            /**
             * Log completion quotation.
             */
            if (response.getBody() != null) {

                log.info(
                        "[SKYDROPX-GET-QUOTATION] quotationId={} isCompleted={}",
                        quotationId,
                        response.getBody()
                                .getIsCompleted());
            }

            return response.getBody();

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-GET-QUOTATION] error consultando quotation",
                    ex);

            throw new RuntimeException(
                    "Error consultando quotation SkyDropX");
        }
    }
}