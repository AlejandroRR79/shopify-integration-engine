package com.creditienda.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.creditienda.service.skydropx.SkyDropXRecoveryService;

@Component
@DisallowConcurrentExecution
public class SkyDropXRecoveryJob implements Job {

    private static final Logger log = LogManager.getLogger(SkyDropXRecoveryJob.class);

    private final SkyDropXRecoveryService recoveryService;

    public SkyDropXRecoveryJob(SkyDropXRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            log.info("[SKYDROPX-RECOVERY-JOB] ejecutando key={}", context.getJobDetail().getKey());
            recoveryService.ejecutarRecovery();
            log.info("[SKYDROPX-RECOVERY-JOB] finalizado");
        } catch (Exception ex) {
            log.error("[SKYDROPX-RECOVERY-JOB] error en ejecucion", ex);
        }
    }
}
