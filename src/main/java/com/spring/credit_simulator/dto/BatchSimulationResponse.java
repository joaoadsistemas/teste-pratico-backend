package com.spring.credit_simulator.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
@Builder
public class BatchSimulationResponse {

    @NonNull
    private String batchId;

    @NonNull
    private Integer totalSimulations;

    @Builder.Default
    private String status = "ACEITO";

    @Builder.Default
    private String message = "Batch recebido e irá ser processado";

    @Builder.Default
    private LocalDateTime acceptedAt = LocalDateTime.now();

    /**
     * Factory method para resposta de batch aceito.
     */
    public static BatchSimulationResponse accepted(String batchId, Integer totalSimulations) {
        return BatchSimulationResponse.builder()
                .batchId(batchId)
                .totalSimulations(totalSimulations)
                .build();
    }

    /**
     * Factory method para resposta de batch rejeitado.
     */
    public static BatchSimulationResponse rejected(String batchId, String errorMessage) {
        return BatchSimulationResponse.builder()
                .batchId(batchId)
                .totalSimulations(0)
                .status("REJEITADO")
                .message(errorMessage)
                .build();
    }
}