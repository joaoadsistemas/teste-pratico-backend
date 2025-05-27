package com.spring.credit_simulator.service;

import com.spring.credit_simulator.dto.*;
import com.spring.credit_simulator.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários isolados do SimulationService usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private MessageService messageService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final SimulationService simulationService = new SimulationService(executorService);

    private SimulationRequest validRequest;
    private LocalDate adultBirthDate;
    private LocalDate youngAdultBirthDate;
    private LocalDate middleAgeBirthDate;
    private LocalDate seniorBirthDate;

    @BeforeEach
    void setUp() {
        LocalDate hoje = LocalDate.now();

        youngAdultBirthDate = hoje.minusYears(23); // Taxa 5%
        adultBirthDate = hoje.minusYears(35);      // Taxa 3%
        middleAgeBirthDate = hoje.minusYears(50);  // Taxa 2%
        seniorBirthDate = hoje.minusYears(65);     // Taxa 4%

        validRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("50000.00"))
                .birthDate(adultBirthDate)
                .loanTermMonths(24)
                .build();
    }

    @Test
    @DisplayName("Deve simular empréstimo corretamente para cliente adulto")
    void deveSimularEmprestimoParaClienteAdulto() {
        SimulationResponse response = simulationService.simulate(validRequest);

        assertNotNull(response);

        assertEquals(validRequest.getLoanAmount(), response.getLoanAmount());
        assertEquals(validRequest.getBirthDate(), response.getBirthDate());
        assertEquals(validRequest.getLoanTermMonths(), response.getLoanTermMonths());

        assertEquals(35, response.getClientAge());
        assertEquals(new BigDecimal("3.0"), response.getAnnualInterestRate());

        assertNotNull(response.getMonthlyPayment());
        assertNotNull(response.getTotalAmount());
        assertNotNull(response.getTotalInterest());

        assertTrue(response.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getTotalAmount().compareTo(response.getLoanAmount()) > 0);
        assertTrue(response.getTotalInterest().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Testa todas as 4 faixas etárias e suas respectivas taxas.
     */
    @Test
    @DisplayName("Deve aplicar taxa de juros correta para cada faixa etária")
    void deveAplicarTaxaCorretaPorFaixaEtaria() {
        // Jovem (≤25 anos) - 5%
        SimulationRequest youngRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(youngAdultBirthDate)
                .loanTermMonths(12)
                .build();

        SimulationResponse youngResponse = simulationService.simulate(youngRequest);
        assertEquals(new BigDecimal("5.0"), youngResponse.getAnnualInterestRate());

        // Adulto (26-40 anos) - 3%
        SimulationResponse adultResponse = simulationService.simulate(validRequest);
        assertEquals(new BigDecimal("3.0"), adultResponse.getAnnualInterestRate());

        // Meia-idade (41-60 anos) - 2%
        SimulationRequest middleAgeRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(middleAgeBirthDate)
                .loanTermMonths(12)
                .build();

        SimulationResponse middleAgeResponse = simulationService.simulate(middleAgeRequest);
        assertEquals(new BigDecimal("2.0"), middleAgeResponse.getAnnualInterestRate());

        // Sênior (>60 anos) - 4%
        SimulationRequest seniorRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(seniorBirthDate)
                .loanTermMonths(12)
                .build();

        SimulationResponse seniorResponse = simulationService.simulate(seniorRequest);
        assertEquals(new BigDecimal("4.0"), seniorResponse.getAnnualInterestRate());
    }

    @Test
    @DisplayName("Deve rejeitar simulação para menor de idade")
    void deveRejeitarSimulacaoParaMenorDeIdade() {
        LocalDate menorIdade = LocalDate.now().minusYears(17);
        SimulationRequest invalidRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("5000.00"))
                .birthDate(menorIdade)
                .loanTermMonths(12)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> simulationService.simulate(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("18 anos"));
        assertEquals("birthDate", exception.getField());
        assertEquals(17, exception.getRejectedValue());
    }

    /**
     * Verifica processamento síncrono para batches pequenos e que MessageService não é chamado.
     */
    @Test
    @DisplayName("Deve processar batch pequeno sincronamente")
    void deveProcessarBatchPequenoSincronamente() {
        List<SimulationRequest> simulations = Arrays.asList(
                SimulationRequest.builder()
                        .loanAmount(new BigDecimal("10000.00"))
                        .birthDate(youngAdultBirthDate)
                        .loanTermMonths(12)
                        .build(),
                SimulationRequest.builder()
                        .loanAmount(new BigDecimal("20000.00"))
                        .birthDate(adultBirthDate)
                        .loanTermMonths(24)
                        .build(),
                SimulationRequest.builder()
                        .loanAmount(new BigDecimal("30000.00"))
                        .birthDate(middleAgeBirthDate)
                        .loanTermMonths(36)
                        .build()
        );

        BatchSimulationRequest batchRequest = BatchSimulationRequest.builder()
                .simulations(simulations)
                .build();

        Object result = simulationService.processBatch(batchRequest);

        assertInstanceOf(List.class, result);

        @SuppressWarnings("unchecked")
        List<SimulationResponse> responses = (List<SimulationResponse>) result;

        assertEquals(3, responses.size());

        // Verifica que não chamou mensageria (processamento síncrono)
        verify(messageService, never()).sendToQueue(any());

        // Verifica taxas corretas por faixa etária
        assertEquals(new BigDecimal("5.0"), responses.get(0).getAnnualInterestRate());
        assertEquals(new BigDecimal("3.0"), responses.get(1).getAnnualInterestRate());
        assertEquals(new BigDecimal("2.0"), responses.get(2).getAnnualInterestRate());
    }

    /**
     * Testa edge cases no cálculo de idade (aniversários próximos).
     */
    @Test
    @DisplayName("Deve calcular idade precisamente em casos especiais")
    void deveCalcularIdadePrecisamente() {
        LocalDate hoje = LocalDate.now();

        // Aniversário foi ontem
        LocalDate aniversarioOntem = hoje.minusYears(30).minusDays(1);
        SimulationRequest request1 = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(aniversarioOntem)
                .loanTermMonths(12)
                .build();

        SimulationResponse response1 = simulationService.simulate(request1);
        assertEquals(30, response1.getClientAge());

        // Aniversário é amanhã
        LocalDate aniversarioAmanha = hoje.minusYears(30).plusDays(1);
        SimulationRequest request2 = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(aniversarioAmanha)
                .loanTermMonths(12)
                .build();

        SimulationResponse response2 = simulationService.simulate(request2);
        assertEquals(29, response2.getClientAge());

        // Aniversário é hoje
        LocalDate aniversarioHoje = hoje.minusYears(25);
        SimulationRequest request3 = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(aniversarioHoje)
                .loanTermMonths(12)
                .build();

        SimulationResponse response3 = simulationService.simulate(request3);
        assertEquals(25, response3.getClientAge());
    }

    @Test
    @DisplayName("Deve rejeitar idade irreal (mais de 120 anos)")
    void deveRejeitarIdadeIrreal() {
        LocalDate muitoIdoso = LocalDate.now().minusYears(121);
        SimulationRequest invalidRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(muitoIdoso)
                .loanTermMonths(12)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> simulationService.simulate(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Idade inválida"));
    }
}