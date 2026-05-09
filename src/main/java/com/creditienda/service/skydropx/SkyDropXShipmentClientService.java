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
     * Obtener shipment.
     */
    public SkyDropXShipmentResponseDTO getShipment(
            String shipmentId) {

        String token = skyDropXTokenService
                .getAccessToken();

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                token);

        HttpEntity<Void> entity = new HttpEntity<>(
                headers);

        String url = baseUrl
                + "/api/v1/shipments/"
                + shipmentId;

        log.info(
                "[SKYDROPX-GET-SHIPMENT] url={}",
                url);

        ResponseEntity<SkyDropXShipmentResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SkyDropXShipmentResponseDTO.class);

        return response.getBody();
    }
}