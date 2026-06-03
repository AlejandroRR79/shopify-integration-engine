package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.creditienda.dto.skydropx.SkyDropXShipmentResponseDTO;

@Service
public class SkyDropXShipmentClientService {

        private static final Logger log = LogManager.getLogger(
                        SkyDropXShipmentClientService.class);

        @Value("${skydropx.base-url}")
        private String baseUrl;

        private final RestTemplate restTemplate;

        private final SkyDropXTokenService skyDropXTokenService;

        public SkyDropXShipmentClientService(
                        RestTemplate restTemplate,
                        SkyDropXTokenService skyDropXTokenService) {

                this.restTemplate = restTemplate;
                this.skyDropXTokenService = skyDropXTokenService;
        }

        /**
         * Obtener shipment SkyDropX.
         *
         * Incluye:
         * - token cache
         * - invalidación automática
         * - retry simple 401
         */
        public SkyDropXShipmentResponseDTO getShipment(
                        String shipmentId) {

                String url = baseUrl
                                + "/api/v1/shipments/"
                                + shipmentId;

                log.info(
                                "[SKYDROPX-GET-SHIPMENT] shipmentId={} url={}",
                                shipmentId,
                                url);

                /**
                 * Primer intento.
                 */
                try {

                        return executeRequest(
                                        shipmentId,
                                        url);

                } catch (HttpClientErrorException.Unauthorized ex) {

                        /**
                         * Token expirado/inválido.
                         */
                        log.warn(
                                        "[SKYDROPX-GET-SHIPMENT] token expirado shipmentId={} retrying...",
                                        shipmentId);

                        /**
                         * Invalidar cache token.
                         */
                        skyDropXTokenService.invalidateToken();

                        /**
                         * Reintentar UNA vez.
                         */
                        return executeRequest(
                                        shipmentId,
                                        url);
                }
        }

        /**
         * Ejecutar request shipment.
         */
        private SkyDropXShipmentResponseDTO executeRequest(
                        String shipmentId,
                        String url) {

                String token = skyDropXTokenService
                                .getAccessToken();

                HttpHeaders headers = new HttpHeaders();

                headers.setBearerAuth(
                                token);

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<SkyDropXShipmentResponseDTO> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                SkyDropXShipmentResponseDTO.class);

                SkyDropXShipmentResponseDTO body = response.getBody();

                log.info(
                                "[SKYDROPX-GET-SHIPMENT] shipmentId={} shipment obtenido correctamente",
                                shipmentId);

                return body;
        }
}