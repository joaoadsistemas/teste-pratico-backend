package com.spring.credit_simulator.service;

import com.spring.credit_simulator.dto.BatchSimulationRequest;

/**
 * Abstração para sistemas de mensageria (RabbitMQ, Kafka, SQS).
 * Permite trocar implementação sem impactar a lógica de negócio.
 */
public interface MessageService {

    void sendToQueue(BatchSimulationRequest batchRequest);

    String checkBatchStatus(String batchId);
}