package com.creditienda.service;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.creditienda.model.EstafetaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EstafetaJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EstafetaJob.class);

    @Autowired
    private EstafetHistorialClient estafetaClient;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Iniciando ejecución del job: {}", context.getJobDetail().getKey());

        try {
            // 1. Consumir el servicio REST
            String json = estafetaClient.consultarHistorial(); // ahora devuelve String

            // 2. Parsear el JSON
            ObjectMapper mapper = new ObjectMapper();
            EstafetaResponse response = mapper.readValue(json, EstafetaResponse.class);

            // 3. Obtener el último evento
            List<EstafetaResponse.ItemHistory> items = response.getItemHistories();
            if (items != null && !items.isEmpty()) {
                List<EstafetaResponse.History> histories = items.get(0).getHistories();
                if (histories != null && !histories.isEmpty()) {
                    EstafetaResponse.History ultimo = histories.get(histories.size() - 1);
                    logger.info("  Último evento:");
                    logger.info("Descripción: {}", ultimo.getSpanishDescription());
                    logger.info("Fecha: {}", ultimo.getEventDateTime());
                    logger.info("Almacén: {}", ultimo.getWarehouseName());
                } else {
                    logger.warn(" No se encontraron eventos en el historial.");
                }
            } else {
                logger.warn("No se encontraron itemHistories en la respuesta.");
            }

            logger.info("Job finalizado exitosamente: {}", context.getJobDetail().getKey());

        } catch (Exception e) {
            logger.error("Error durante la ejecución del job {}: {}", context.getJobDetail().getKey(), e.getMessage(),
                    e);
            throw new JobExecutionException("Fallo al ejecutar EstafetaClient", e);
        }
    }
}