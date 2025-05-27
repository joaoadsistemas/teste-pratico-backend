package com.spring.credit_simulator.service;

import com.spring.credit_simulator.dto.BatchSimulationRequest;
import com.spring.credit_simulator.dto.BatchSimulationResponse;
import com.spring.credit_simulator.dto.SimulationRequest;
import com.spring.credit_simulator.dto.SimulationResponse;
import com.spring.credit_simulator.exception.ValidationException;
import com.spring.credit_simulator.util.LoanCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final ExecutorService executorService;

    public SimulationResponse simulate(SimulationRequest request) {
        log.debug("Iniciando simulação para: {}", request);

        int age = calculateAge(request.getBirthDate());
        validateAge(age);

        BigDecimal annualRate = determineInterestRate(age);
        log.info("Taxa de juros determinada: {}% ao ano para idade {}", annualRate, age);

        BigDecimal monthlyPayment = LoanCalculator.calculateMonthlyPayment(
                request.getLoanAmount(),
                annualRate,
                request.getLoanTermMonths()
        );

        BigDecimal totalAmount = LoanCalculator.calculateTotalAmount(
                monthlyPayment,
                request.getLoanTermMonths()
        );

        BigDecimal totalInterest = LoanCalculator.calculateTotalInterest(
                totalAmount,
                request.getLoanAmount()
        );

        SimulationResponse response = SimulationResponse.builder()
                .loanAmount(request.getLoanAmount())
                .birthDate(request.getBirthDate())
                .clientAge(age)
                .loanTermMonths(request.getLoanTermMonths())
                .annualInterestRate(annualRate)
                .monthlyPayment(monthlyPayment)
                .totalAmount(totalAmount)
                .totalInterest(totalInterest)
                .build();

        log.info("Simulação concluída. Parcela: R$ {}", monthlyPayment);
        return response;
    }

    /**
     * Estratégia adaptativa: batches ≤100 processados sincronamente, >100 assíncronos.
     */
    public Object processBatch(BatchSimulationRequest batchRequest) {
        int totalSimulations = batchRequest.getTotalSimulations();
        log.info("Processando batch {} com {} simulações",
                batchRequest.getBatchId(), totalSimulations);

        if (batchRequest.getSimulations().size() > 100) {
            return processAsyncBatch(batchRequest);
        } else {
            return processSyncBatch(batchRequest);
        }
    }

    /**
     * Processamento paralelo usando CompletableFuture com pool de threads customizado.
     */
    private List<SimulationResponse> processSyncBatch(BatchSimulationRequest batchRequest) {
        log.debug("Processando batch sincronamente");

        List<CompletableFuture<SimulationResponse>> futures = batchRequest.getSimulations()
                .stream()
                .map(request -> CompletableFuture.supplyAsync(
                        () -> simulate(request),
                        executorService
                ))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private BatchSimulationResponse processAsyncBatch(BatchSimulationRequest batchRequest) {
        log.info("Enviando batch {} para processamento assíncrono",
                batchRequest.getBatchId());

        // Em produção: messageService.sendToQueue(batchRequest);

        return BatchSimulationResponse.accepted(
                batchRequest.getBatchId(),
                batchRequest.getTotalSimulations()
        );
    }

    /**
     * Usa Period.between() para cálculo preciso considerando mês e dia.
     */
    private int calculateAge(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        Period period = Period.between(birthDate, today);
        return period.getYears();
    }

    private void validateAge(int age) {
        if (age < 18) {
            throw ValidationException.invalidAge(age);
        }

        if (age > 120) {
            throw new ValidationException(
                    "birthDate",
                    age,
                    "Idade inválida. Por favor, verifique a data de nascimento."
            );
        }
    }

    /**
     * Taxas por faixa etária: ≤25: 5%, 26-40: 3%, 41-60: 2%, >60: 4%.
     */
    private BigDecimal determineInterestRate(int age) {
        if (age <= 25) {
            return new BigDecimal("5.0");
        } else if (age <= 40) {
            return new BigDecimal("3.0");
        } else if (age <= 60) {
            return new BigDecimal("2.0");
        } else {
            return new BigDecimal("4.0");
        }
    }
}