package com.spring.credit_simulator;

import com.spring.credit_simulator.dto.*;
import com.spring.credit_simulator.service.SimulationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de performance para validar requisitos de alta volumetria (até 10.000 simulações).
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceTest {

    @Autowired
    private SimulationService simulationService;

    private static final List<Long> temposDeResposta = new ArrayList<>();
    private static final List<Double> throughputMedidas = new ArrayList<>();

    @Test
    @Order(1)
    @DisplayName("Deve processar simulação individual em menos de 100ms")
    void testePerformanceSimulacaoIndividual() {
        SimulationRequest request = criarRequestPadrao();

        // Warmup da JVM para medições precisas (JIT compilation)
        for (int i = 0; i < 10; i++) {
            simulationService.simulate(request);
        }

        Instant inicio = Instant.now();
        SimulationResponse response = simulationService.simulate(request);
        Instant fim = Instant.now();

        long tempoMs = Duration.between(inicio, fim).toMillis();
        temposDeResposta.add(tempoMs);

        assertNotNull(response);
        assertTrue(tempoMs < 100,
                String.format("Simulação individual deve levar menos de 100ms. Levou: %d ms", tempoMs));

        System.out.printf("Tempo de simulação individual: %d ms%n", tempoMs);
    }

    /**
     * Teste de throughput: métrica chave para alta volumetria.
     */
    @Test
    @Order(2)
    @DisplayName("Deve processar pelo menos 100 simulações por segundo")
    void testeThroughputSimulacoes() {
        int numeroSimulacoes = 1000;
        List<SimulationRequest> requests = IntStream.range(0, numeroSimulacoes)
                .mapToObj(i -> criarRequestVariado(i))
                .collect(Collectors.toList());

        Instant inicio = Instant.now();

        List<SimulationResponse> responses = requests.stream()
                .map(simulationService::simulate)
                .collect(Collectors.toList());

        Instant fim = Instant.now();

        long tempoTotalMs = Duration.between(inicio, fim).toMillis();
        double tempoTotalSegundos = tempoTotalMs / 1000.0;
        double simulacoesPorSegundo = numeroSimulacoes / tempoTotalSegundos;

        throughputMedidas.add(simulacoesPorSegundo);

        assertEquals(numeroSimulacoes, responses.size());

        assertTrue(simulacoesPorSegundo > 100,
                String.format("Deve processar pelo menos 100 simulações/segundo. Atual: %.2f",
                        simulacoesPorSegundo));

        System.out.printf("Throughput: %.2f simulações/segundo%n", simulacoesPorSegundo);
        System.out.printf("Tempo total para %d simulações: %.2f segundos%n",
                numeroSimulacoes, tempoTotalSegundos);
    }

    /**
     * Teste crítico: volume máximo do requisito (10.000 simulações).
     */
    @Test
    @Order(3)
    @DisplayName("Deve processar 10.000 simulações em tempo razoável")
    void testeCargaMaxima() {
        int volumeMaximo = 10000;

        // Dados variados com .parallel() para acelerar criação do dataset
        List<SimulationRequest> requests = IntStream.range(0, volumeMaximo)
                .parallel()
                .mapToObj(i -> criarRequestVariado(i))
                .collect(Collectors.toList());

        BatchSimulationRequest batchRequest = BatchSimulationRequest.builder()
                .simulations(requests)
                .build();

        Instant inicio = Instant.now();
        Object result = simulationService.processBatch(batchRequest);
        Instant fim = Instant.now();

        long tempoAceitacaoMs = Duration.between(inicio, fim).toMillis();

        assertNotNull(result);

        if (result instanceof BatchSimulationResponse) {
            BatchSimulationResponse batchResponse = (BatchSimulationResponse) result;
            assertEquals("ACEITO", batchResponse.getStatus());
            assertEquals(volumeMaximo, batchResponse.getTotalSimulations());

            assertTrue(tempoAceitacaoMs < 1000,
                    String.format("Aceitação de batch grande deve ser rápida. Levou: %d ms",
                            tempoAceitacaoMs));
        }

        System.out.printf("Tempo para aceitar batch de %d simulações: %d ms%n",
                volumeMaximo, tempoAceitacaoMs);
    }

    /**
     * Relatório de métricas coletadas durante os testes.
     */
    @AfterAll
    static void relatorioFinalPerformance() {
        System.out.println("\n📊 RELATÓRIO DE PERFORMANCE");
        System.out.println("=" .repeat(50));

        if (!temposDeResposta.isEmpty()) {
            double tempoMedio = temposDeResposta.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            System.out.printf("Tempo médio por simulação: %.2f ms%n", tempoMedio);
        }

        if (!throughputMedidas.isEmpty()) {
            double throughputMedio = throughputMedidas.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            System.out.printf("Throughput médio: %.2f simulações/segundo%n", throughputMedio);
        }

        System.out.println("=" .repeat(50));
    }

    private SimulationRequest criarRequestPadrao() {
        return SimulationRequest.builder()
                .loanAmount(new BigDecimal("50000.00"))
                .birthDate(LocalDate.now().minusYears(35))
                .loanTermMonths(24)
                .build();
    }

    /**
     * Cria dados variados mas determinísticos para simular carga real.
     */
    private SimulationRequest criarRequestVariado(int seed) {
        int idade = 20 + (seed % 60);           // 20 a 79 anos
        BigDecimal valor = new BigDecimal(10000 + (seed % 90000)); // 10k a 100k
        int meses = 12 + (seed % 48);           // 12 a 60 meses

        return SimulationRequest.builder()
                .loanAmount(valor)
                .birthDate(LocalDate.now().minusYears(idade))
                .loanTermMonths(meses)
                .build();
    }
}