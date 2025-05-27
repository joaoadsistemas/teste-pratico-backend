package com.spring.credit_simulator.controller;

import com.spring.credit_simulator.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração end-to-end do SimulationController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private SimulationRequest validRequest;
    private String validRequestJson;

    @BeforeEach
    void setUp() throws Exception {
        validRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("50000.00"))
                .birthDate(LocalDate.now().minusYears(35))
                .loanTermMonths(24)
                .build();

        validRequestJson = objectMapper.writeValueAsString(validRequest);
    }

    @Test
    @DisplayName("POST /api/v1/simulations - Deve simular empréstimo com sucesso")
    void deveSimularEmprestimoComSucesso() throws Exception {
        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson))

                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.loanAmount").value("50000.00"))
                .andExpect(jsonPath("$.birthDate").value(validRequest.getBirthDate().toString()))
                .andExpect(jsonPath("$.clientAge").value(35))
                .andExpect(jsonPath("$.loanTermMonths").value(24))
                .andExpect(jsonPath("$.annualInterestRate").value("3.0"))

                .andExpect(jsonPath("$.monthlyPayment").exists())
                .andExpect(jsonPath("$.monthlyPayment").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.totalInterest").exists())

                // BigDecimal serializado como String para manter precisão
                .andExpect(jsonPath("$.monthlyPayment").isString())
                .andExpect(jsonPath("$.totalAmount").isString())
                .andExpect(jsonPath("$.totalInterest").isString());
    }

    @Test
    @DisplayName("POST /api/v1/simulations - Deve retornar 400 para dados inválidos")
    void deveRetornarBadRequestParaDadosInvalidos() throws Exception {
        String requestSemValor = """
            {
                "birthDate": "1990-01-01",
                "loanTermMonths": 24
            }
            """;

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestSemValor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.loanAmount").exists());

        String requestValorBaixo = """
            {
                "loanAmount": 500.00,
                "birthDate": "1990-01-01",
                "loanTermMonths": 24
            }
            """;

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValorBaixo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.loanAmount")
                        .value(containsString("mínimo")));

        String requestPrazoInvalido = """
            {
                "loanAmount": 10000.00,
                "birthDate": "1990-01-01",
                "loanTermMonths": 3
            }
            """;

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPrazoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.loanTermMonths")
                        .value(containsString("mínimo é de 6 meses")));
    }

    @Test
    @DisplayName("POST /api/v1/simulations - Deve rejeitar menor de idade")
    void deveRejeitarMenorDeIdade() throws Exception {
        SimulationRequest menorRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(LocalDate.now().minusYears(17))
                .loanTermMonths(12)
                .build();

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menorRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Rule Violation"))
                .andExpect(jsonPath("$.message").value(containsString("18 anos")))
                .andExpect(jsonPath("$.field").value("birthDate"))
                .andExpect(jsonPath("$.rejectedValue").value(17));
    }

    /**
     * Testa processamento síncrono para batches pequenos (≤100).
     */
    @Test
    @DisplayName("POST /api/v1/simulations/batch - Deve processar batch pequeno sincronamente")
    void deveProcessarBatchPequenoSincronamente() throws Exception {
        List<SimulationRequest> simulations = Arrays.asList(
                SimulationRequest.builder()
                        .loanAmount(new BigDecimal("10000.00"))
                        .birthDate(LocalDate.now().minusYears(25))
                        .loanTermMonths(12)
                        .build(),
                SimulationRequest.builder()
                        .loanAmount(new BigDecimal("20000.00"))
                        .birthDate(LocalDate.now().minusYears(45))
                        .loanTermMonths(24)
                        .build()
        );

        BatchSimulationRequest batchRequest = BatchSimulationRequest.builder()
                .simulations(simulations)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        List<SimulationResponse> responses = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SimulationResponse.class)
        );

        // Verificar taxas corretas por faixa etária
        assertEquals(new BigDecimal("5.0"), responses.get(0).getAnnualInterestRate()); // 25 anos
        assertEquals(new BigDecimal("2.0"), responses.get(1).getAnnualInterestRate()); // 45 anos
    }

    /**
     * Testa processamento assíncrono para batches grandes (>100).
     */
    @Test
    @DisplayName("POST /api/v1/simulations/batch - Deve aceitar batch grande para processamento assíncrono")
    void deveAceitarBatchGrandeParaProcessamentoAssincrono() throws Exception {
        SimulationRequest template = SimulationRequest.builder()
                .loanAmount(new BigDecimal("10000.00"))
                .birthDate(LocalDate.now().minusYears(30))
                .loanTermMonths(12)
                .build();

        List<SimulationRequest> manySimulations = Arrays.asList(new SimulationRequest[150]);
        for (int i = 0; i < 150; i++) {
            manySimulations.set(i, template);
        }

        BatchSimulationRequest largeBatch = BatchSimulationRequest.builder()
                .simulations(manySimulations)
                .build();

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeBatch)))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.totalSimulations").value(150))
                .andExpect(jsonPath("$.status").value("ACEITO"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.acceptedAt").exists());
    }

    @Test
    @DisplayName("Deve aceitar e retornar datas no formato ISO 8601")
    void deveAceitarFormatoDataISO() throws Exception {
        String requestComData = """
            {
                "loanAmount": 25000.00,
                "birthDate": "1995-12-25",
                "loanTermMonths": 36
            }
            """;

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestComData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthDate").value("1995-12-25"))
                .andExpect(jsonPath("$.clientAge").isNumber());
    }

    /**
     * Verifica precisão de BigDecimal na serialização JSON.
     */
    @Test
    @DisplayName("Deve manter precisão de valores monetários")
    void deveManterPrecisaoMonetaria() throws Exception {
        SimulationRequest preciseRequest = SimulationRequest.builder()
                .loanAmount(new BigDecimal("12345.67"))
                .birthDate(LocalDate.now().minusYears(30))
                .loanTermMonths(18)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preciseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanAmount").value("12345.67"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        SimulationResponse simResponse = objectMapper.readValue(response, SimulationResponse.class);

        assertEquals(2, simResponse.getMonthlyPayment().scale());
        assertEquals(2, simResponse.getTotalAmount().scale());
        assertEquals(2, simResponse.getTotalInterest().scale());
    }

    @Test
    @DisplayName("GET /api/v1/simulations/batch/{id}/status - Deve retornar status do batch")
    void deveRetornarStatusDoBatch() throws Exception {
        String batchId = "test-batch-123";

        mockMvc.perform(get("/api/v1/simulations/batch/{id}/status", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.progress").isNumber());
    }
}