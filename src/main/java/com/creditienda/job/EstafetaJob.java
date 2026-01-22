
package com.creditienda.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.service.delivery.DeliveryTrackingService;

@Component
@DisallowConcurrentExecution
public class EstafetaJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(EstafetaJob.class);

    private final DeliveryTrackingService deliveryTrackingService;

    public EstafetaJob(DeliveryTrackingService deliveryTrackingService) {
        this.deliveryTrackingService = deliveryTrackingService;
    }

    @Value("${estafeta.cron.exp}")
    private String cronExpression;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            log.info("⏰ Ejecutando EstafetaJob | key={}",
                    context.getJobDetail().getKey());

            log.info("🔎 estafeta.cron.exp (Spring) = {}",
                    cronExpression);

            deliveryTrackingService.sincronizarEstatusEntregas();

            log.info("🏁 Finaliza EstafetaJob");
        } catch (Exception e) {
            log.error("❌ Error en EstafetaJob: {}", e);
        }
    }
}
