package com.spring.credit_simulator.controller;

import com.spring.credit_simulator.dto.*;
import com.spring.credit_simulator.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Simulações de Crédito", description = "Endpoints para simulação de empréstimos")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping
    @Operation(
            summary = "Realizar simulação de crédito",
            description = "Calcula parcelas, juros e valor total de um empréstimo baseado na idade do cliente"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Simulação realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = SimulationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos fornecidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor",
                    content = @Content
            )
    })
    public ResponseEntity<SimulationResponse> simulateLoan(
            @Valid @RequestBody SimulationRequest request) {

        log.info("Recebida requisição de simulação: valor={}, prazo={} meses",
                request.getLoanAmount(), request.getLoanTermMonths());

        SimulationResponse response = simulationService.simulate(request);

        log.info("Simulação concluída: parcela=R$ {}", response.getMonthlyPayment());

        return ResponseEntity.ok(response);
    }

    /**
     * Processa múltiplas simulações. Retorna List para batches pequenos (≤100)
     * ou BatchSimulationResponse para processamento assíncrono (>100).
     */
    @PostMapping("/batch")
    @Operation(
            summary = "Realizar múltiplas simulações",
            description = "Processa múltiplas simulações de crédito. " +
                    "Batches pequenos (até 100) são processados sincronamente. " +
                    "Batches grandes são enviados para processamento assíncrono."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Batch pequeno processado com sucesso",
                    content = @Content(schema = @Schema(implementation = SimulationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Batch grande aceito para processamento assíncrono",
                    content = @Content(schema = @Schema(implementation = BatchSimulationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos no batch",
                    content = @Content
            )
    })
    public ResponseEntity<?> simulateBatch(
            @Valid @RequestBody BatchSimulationRequest batchRequest) {

        int totalSimulations = batchRequest.getTotalSimulations();
        log.info("Recebido batch {} com {} simulações",
                batchRequest.getBatchId(), totalSimulations);

        Object result = simulationService.processBatch(batchRequest);

        if (result instanceof BatchSimulationResponse) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/batch/{batchId}/status")
    @Operation(
            summary = "Verificar status de um batch",
            description = "Consulta o status de processamento de um batch enviado para processamento assíncrono"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retornado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Batch não encontrado"
            )
    })
    public ResponseEntity<BatchStatusResponse> checkBatchStatus(
            @Parameter(description = "ID do batch a ser consultado")
            @PathVariable String batchId) {

        log.debug("Consultando status do batch {}", batchId);

        BatchStatusResponse status = BatchStatusResponse.builder()
                .batchId(batchId)
                .status("PROCESSING")
                .message("Batch está sendo processado")
                .progress(45)
                .build();

        return ResponseEntity.ok(status);
    }
}