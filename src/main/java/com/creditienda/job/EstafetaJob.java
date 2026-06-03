package com.creditienda.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.service.delivery.TrackingOrchestratorService;

@Component
@DisallowConcurrentExecution
public class EstafetaJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(EstafetaJob.class);

    private final TrackingOrchestratorService orchestrator;

    public EstafetaJob(TrackingOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Value("${estafeta.cron.exp}")
    private String cronExpression;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            log.info("⏰ Ejecutando EstafetaJob | key={}", context.getJobDetail().getKey());
            log.info("🔎 estafeta.cron.exp={}", cronExpression);

            orchestrator.ejecutarSincronizacion();

            log.info("🏁 Finaliza EstafetaJob");
        } catch (Exception e) {
            log.error("❌ Error en EstafetaJob", e);
        }
    }
}