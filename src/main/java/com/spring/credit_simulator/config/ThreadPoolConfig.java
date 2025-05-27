package com.spring.credit_simulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    /**
     * Pool de threads para processamento paralelo de batches.
     * Adapta automaticamente ao hardware usando availableProcessors().
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService simulationExecutorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}