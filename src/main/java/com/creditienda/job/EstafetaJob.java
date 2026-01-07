
package com.creditienda.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Override
    public void execute(JobExecutionContext context) {
        log.info("⏰ Ejecutando EstafetaJob | key={}",
                context.getJobDetail().getKey());

        deliveryTrackingService.sincronizarEstatusEntregas();

        log.info("🏁 Finaliza EstafetaJob");
    }

}
