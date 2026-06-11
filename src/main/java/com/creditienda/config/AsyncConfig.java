package com.creditienda.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "shopifyMultiStoreExecutor")
    public Executor shopifyMultiStoreExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Shopify-Multi-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "skydropxExecutor")
    public Executor skydropxExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        /**
         * Threads base vivos.
         */
        executor.setCorePoolSize(10);

        /**
         * Máximo threads concurrentes.
         */
        executor.setMaxPoolSize(30);

        /**
         * Cola antes de crear más threads.
         */
        executor.setQueueCapacity(500);

        /**
         * Nombre threads.
         */
        executor.setThreadNamePrefix(
                "SkyDropX-Async-");

        /**
         * Esperar tareas al apagar.
         */
        executor.setWaitForTasksToCompleteOnShutdown(
                true);

        /**
         * Espera shutdown.
         */
        executor.setAwaitTerminationSeconds(
                60);

        executor.initialize();

        return executor;
    }
}