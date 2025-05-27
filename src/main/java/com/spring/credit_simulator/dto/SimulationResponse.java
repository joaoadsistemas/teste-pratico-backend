package com.spring.credit_simulator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {

    // Dados originais da solicitação
    @JsonFormat(shape = JsonFormat.Shape.STRING) // BigDecimal como String para precisão no JSON
    private BigDecimal loanAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Integer clientAge;
    private Integer loanTermMonths;

    // Dados calculados
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal annualInterestRate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal monthlyPayment;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalInterest;
}