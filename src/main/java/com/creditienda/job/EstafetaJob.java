package com.creditienda.job;

import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.creditienda.model.EstafetaResponse;
import com.creditienda.service.EstafetHistorialClient;
import com.creditienda.service.notificacion.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@DisallowConcurrentExecution
public class EstafetaJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EstafetaJob.class);

    private final EstafetHistorialClient estafetaClient;

    @Autowired
    private NotificacionService notificacionService;

    public EstafetaJob(EstafetHistorialClient estafetaClient) {
        this.estafetaClient = estafetaClient;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Iniciando ejecución del job: {}", context.getJobDetail().getKey());

        try {
            // String referencia = "4055911250502700000019";
            String referencia = "2015410173997631417025";
            String json = estafetaClient.consultarHistorialNumReferencia(referencia);

            ObjectMapper mapper = new ObjectMapper();
            EstafetaResponse response = mapper.readValue(json, EstafetaResponse.class);

            /*
             * List<EstafetaResponse.ItemHistory> items = response.getItemHistories();
             * if (items != null && !items.isEmpty()) {
             * EstafetaResponse.ItemHistory item = items.get(0);
             * 
             * ErrorInfo error = item.getError();
             * if (error != null) {
             * logger.warn("⚠️ Error en código de rastreo:");
             * logger.warn("Código: {}", error.getCode());
             * logger.warn("Descripción: {}", error.getDescription());
             * }
             * 
             * List<EstafetaResponse.History> histories = item.getHistories();
             * if (histories != null && !histories.isEmpty()) {
             * EstafetaResponse.History ultimo = histories.get(histories.size() - 1);
             * logger.info("📦 Último evento:");
             * logger.info("Descripción: {}", ultimo.getSpanishDescription());
             * logger.info("Fecha: {}", ultimo.getEventDateTime());
             * logger.info("Almacén: {}", ultimo.getWarehouseName());
             * } else {
             * logger.warn("No se encontraron eventos en el historial.");
             * }
             * } else {
             * logger.warn("No se encontraron itemHistories en la respuesta.");
             * }
             */

            List<EstafetaResponse.Item> items = response.getItems();
            if (items != null && !items.isEmpty()) {
                for (EstafetaResponse.Item item : items) {
                    logger.info("📦 Waybill: {}", item.getInformation().getWaybillCode());
                    logger.info("Referencia: {}", item.getInformation().getReferenceCode());

                    EstafetaResponse.Status statusActual = item.getStatusCurrent();
                    if (statusActual != null) {
                        logger.info("📍 Último estatus (statusCurrent):");
                        logger.info("Código: {}", statusActual.getCode());
                        logger.info("Descripción: {}", statusActual.getSpanishName());
                        logger.info("Fecha: {}", statusActual.getEventDateTime());
                        logger.info("Almacén: {}", statusActual.getWarehouseName());
                    } else {
                        logger.warn("⚠️ No se encontró statusCurrent en el item.");
                    }
                }
            } else {
                logger.warn("⚠️ No se encontraron items en la respuesta.");
            }

            logger.info("✅ Job finalizado exitosamente: {}", context.getJobDetail().getKey());
            // notificacionService.enviarConfirmacion("El job Estafeta se ejecutó
            // correctamente.");

        } catch (Exception e) {
            logger.error("❌ Error durante la ejecución del job {}: {}", context.getJobDetail().getKey(), e.getMessage(),
                    e);
            notificacionService.enviarError("Error en job Estafeta: " + e.getMessage());
            throw new JobExecutionException("Fallo al ejecutar EstafetaClient", e);
        }
    }
}
