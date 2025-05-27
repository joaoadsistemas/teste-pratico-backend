package com.spring.credit_simulator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSimulationRequest {

    /**
     * ID único do batch gerado automaticamente via @Builder.Default.
     */
    @Builder.Default
    private String batchId = UUID.randomUUID().toString();

    /**
     * Lista de simulações com validação em cascata (@Valid).
     */
    @NotEmpty(message = "Deve haver pelo menos uma simulação no batch")
    @Size(max = 10000, message = "O batch não pode conter mais de 10.000 simulações")
    @Valid
    private List<SimulationRequest> simulations;

    private boolean asyncProcessing;

    public int getTotalSimulations() {
        return simulations != null ? simulations.size() : 0;
    }

    /**
     * Setter customizado que define automaticamente processamento assíncrono para batches > 100.
     */
    public void setSimulations(List<SimulationRequest> simulations) {
        this.simulations = simulations;
        this.asyncProcessing = simulations != null && simulations.size() > 100;
    }

    public BatchSimulationRequest(List<SimulationRequest> simulations) {
        this.batchId = UUID.randomUUID().toString();
        this.simulations = simulations;
        this.asyncProcessing = simulations != null && simulations.size() > 100;
    }
}