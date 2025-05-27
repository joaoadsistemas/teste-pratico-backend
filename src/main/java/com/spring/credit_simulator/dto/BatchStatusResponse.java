package com.spring.credit_simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para acompanhar progresso de batches em processamento assíncrono.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponse {

    private String batchId;
    private String status;
    private String message;
    private Integer progress;
    private Integer totalSimulations;
    private Integer processedSimulations;
    private Long estimatedTimeRemaining;

    @Builder.Default
    private LocalDateTime lastUpdate = LocalDateTime.now();
}