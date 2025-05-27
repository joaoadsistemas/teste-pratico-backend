package com.spring.credit_simulator.exception;

import lombok.Getter;

/**
 * Exceção customizada para regras de negócio com informações estruturadas (field, rejectedValue).
 * Usa @Getter ao invés de @Data para manter imutabilidade após criação.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final String field;
    private final Object rejectedValue;

    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
    }

    public ValidationException(String field, Object rejectedValue, String message) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public static ValidationException invalidAge(int age) {
        return new ValidationException(
                "birthDate",
                age,
                String.format("Cliente deve ter pelo menos 18 anos. Idade atual: %d anos", age)
        );
    }

    public static ValidationException invalidLoanAmount(Number amount) {
        return new ValidationException(
                "loanAmount",
                amount,
                "Valor do empréstimo deve estar entre R$ 1.000 e R$ 1.000.000"
        );
    }
}