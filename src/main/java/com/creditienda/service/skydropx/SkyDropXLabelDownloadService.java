package com.creditienda.service.skydropx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio encargado de descargar
 * guía PDF en memoria.
 */
@Service
public class SkyDropXLabelDownloadService {

    private static final Logger log = LogManager.getLogger(
            SkyDropXLabelDownloadService.class);

    private final RestTemplate restTemplate;

    public SkyDropXLabelDownloadService(
            RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    /**
     * Descargar label PDF.
     *
     * @param labelUrl url temporal SkyDropX
     * @return PDF bytes
     */
    public byte[] downloadLabel(
            String labelUrl) {

        try {

            log.info(
                    "[SKYDROPX-LABEL] descargando label");

            ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                    labelUrl,
                    HttpMethod.GET,
                    null,
                    ByteArrayResource.class);

            byte[] pdfBytes = response.getBody()
                    .getByteArray();

            log.info(
                    "[SKYDROPX-LABEL] bytes descargados={}",
                    pdfBytes.length);

            return pdfBytes;

        } catch (Exception ex) {

            log.error(
                    "[SKYDROPX-LABEL] error descargando label",
                    ex);

            throw new RuntimeException(
                    "Error descargando label");
        }
    }
}