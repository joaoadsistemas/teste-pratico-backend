package com.spring.credit_simulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculos financeiros usando Sistema Price (parcelas fixas).
 * Fórmula: PMT = PV * r / (1 - (1 + r)^-n)
 */
public class LoanCalculator {

    private static final int CALCULATION_SCALE = 10; // Precisão para cálculos intermediários
    private static final int MONEY_SCALE = 2; // Precisão final (centavos)
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP; // Padrão bancário

    public static BigDecimal calculateMonthlyPayment(
            BigDecimal loanAmount,
            BigDecimal annualInterestRate,
            int termMonths) {

        // Caso especial: taxa zero
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(
                    BigDecimal.valueOf(termMonths),
                    MONEY_SCALE,
                    ROUNDING_MODE
            );
        }

        // Taxa anual % para mensal decimal: 5.0 → 5.0/100/12 = 5.0/1200
        BigDecimal monthlyRate = annualInterestRate.divide(
                BigDecimal.valueOf(1200),
                CALCULATION_SCALE,
                ROUNDING_MODE
        );

        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = pow(onePlusRate, termMonths);

        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        BigDecimal numerator = loanAmount
                .multiply(monthlyRate)
                .multiply(onePlusRatePowerN);

        return numerator.divide(denominator, MONEY_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateTotalAmount(
            BigDecimal monthlyPayment,
            int termMonths) {

        return monthlyPayment.multiply(BigDecimal.valueOf(termMonths))
                .setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateTotalInterest(
            BigDecimal totalAmount,
            BigDecimal loanAmount) {

        return totalAmount.subtract(loanAmount)
                .setScale(MONEY_SCALE, ROUNDING_MODE);
    }

    /**
     * Implementação customizada de potência para BigDecimal (não tem pow() nativo).
     */
    private static BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }

        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base)
                    .setScale(CALCULATION_SCALE, ROUNDING_MODE);
        }

        return result;
    }
}