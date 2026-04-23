package com.creditienda.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.service.facturacion.DeliveryFacturacionService;

@Component
@DisallowConcurrentExecution
public class FacturacionJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(FacturacionJob.class);

    private final DeliveryFacturacionService service;

    @Value("${facturacion.cron.exp}")
    private String cron;

    public FacturacionJob(DeliveryFacturacionService service) {
        this.service = service;
    }

    @Override
    public void execute(JobExecutionContext context) {

        try {

            log.info("⏰ Ejecutando FacturacionJob");
            log.info("🔎 cron={}", cron);

            service.ejecutarFacturacion();

            log.info("🏁 Fin FacturacionJob");

        } catch (Exception e) {

            log.error("❌ Error en FacturacionJob", e);
        }
    }
}