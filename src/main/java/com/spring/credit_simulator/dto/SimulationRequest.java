package com.spring.credit_simulator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
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
public class SimulationRequest {

    /**
     * BigDecimal para precisão em cálculos financeiros.
     */
    @NotNull(message = "O valor do empréstimo é obrigatório")
    @DecimalMin(value = "1000.00", message = "O valor mínimo do empréstimo é R$ 1.000,00")
    @DecimalMax(value = "1000000.00", message = "O valor máximo do empréstimo é R$ 1.000.000,00")
    @Digits(integer = 8, fraction = 2, message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal loanAmount;

    @NotNull(message = "A data de nascimento é obrigatória")
    @Past(message = "A data de nascimento deve estar no passado")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotNull(message = "O prazo do empréstimo é obrigatório")
    @Min(value = 6, message = "O prazo mínimo é de 6 meses")
    @Max(value = 360, message = "O prazo máximo é de 360 meses (30 anos)")
    private Integer loanTermMonths;
}