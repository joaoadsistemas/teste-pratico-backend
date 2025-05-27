package com.spring.credit_simulator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da LoanCalculator - base matemática do sistema.
 */
class LoanCalculatorTest {

    @Test
    @DisplayName("Deve calcular parcela corretamente para empréstimo padrão")
    void deveCalcularParcelaCorretamenteParaEmprestimoPadrao() {
        BigDecimal valorEmprestimo = new BigDecimal("10000.00");
        BigDecimal taxaAnual = new BigDecimal("5.0");
        int prazoMeses = 12;

        BigDecimal parcela = LoanCalculator.calculateMonthlyPayment(
                valorEmprestimo, taxaAnual, prazoMeses
        );

        // Valor calculado e verificado com calculadoras financeiras
        assertEquals(new BigDecimal("856.07"), parcela);
    }

    /**
     * Testa múltiplos cenários usando @ParameterizedTest - valores calculados e verificados.
     */
    @ParameterizedTest
    @DisplayName("Deve calcular parcelas corretamente para diversos cenários")
    @CsvSource({
            // valor, taxa, meses, parcela esperada
            "10000.00, 5.0, 12, 856.07",      // Caso base
            "50000.00, 3.0, 24, 2149.06",     // Empréstimo maior, taxa menor
            "100000.00, 2.0, 36, 2864.26",    // Taxa baixa
            "25000.00, 4.0, 60, 460.41",      // Prazo longo
            "1000.00, 5.0, 6, 169.11",        // Valores mínimos
            "1000000.00, 4.0, 360, 4774.15"   // Valores máximos
    })
    void deveCalcularParcelasCorretamenteParaDiversosCenarios(
            String valor, String taxa, int meses, String parcelaEsperada) {

        BigDecimal valorEmprestimo = new BigDecimal(valor);
        BigDecimal taxaAnual = new BigDecimal(taxa);
        BigDecimal esperado = new BigDecimal(parcelaEsperada);

        BigDecimal parcela = LoanCalculator.calculateMonthlyPayment(
                valorEmprestimo, taxaAnual, meses
        );

        assertEquals(esperado, parcela,
                String.format("Parcela incorreta para valor=%s, taxa=%s%%, meses=%d",
                        valor, taxa, meses));
    }

    /**
     * Edge case: taxa zero evita divisão por zero na fórmula matemática.
     */
    @Test
    @DisplayName("Deve calcular parcela corretamente para empréstimo sem juros")
    void deveCalcularParcelaParaEmprestimoSemJuros() {
        BigDecimal valorEmprestimo = new BigDecimal("12000.00");
        BigDecimal taxaZero = BigDecimal.ZERO;
        int prazoMeses = 12;

        BigDecimal parcela = LoanCalculator.calculateMonthlyPayment(
                valorEmprestimo, taxaZero, prazoMeses
        );

        BigDecimal esperado = new BigDecimal("1000.00");
        assertEquals(esperado, parcela);
    }

    /**
     * Testa precisão: sempre 2 casas decimais com HALF_UP.
     */
    @Test
    @DisplayName("Deve arredondar parcela corretamente para centavos")
    void deveArredondarParcelaCorretamente() {
        BigDecimal valor = new BigDecimal("10000.00");
        BigDecimal taxa = new BigDecimal("4.44"); // Taxa que força arredondamento
        int meses = 13;

        BigDecimal parcela = LoanCalculator.calculateMonthlyPayment(
                valor, taxa, meses
        );

        assertEquals(2, parcela.scale(), "Deve ter exatamente 2 casas decimais");
        assertNotNull(parcela);
        assertTrue(parcela.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Testa consistência entre métodos: total = parcela × meses, juros = total - principal.
     */
    @Test
    @DisplayName("Deve calcular valor total e juros consistentemente")
    void deveCalcularValorTotalEJurosConsistentemente() {
        BigDecimal valorEmprestimo = new BigDecimal("30000.00");
        BigDecimal taxaAnual = new BigDecimal("3.5");
        int prazoMeses = 48;

        BigDecimal parcela = LoanCalculator.calculateMonthlyPayment(
                valorEmprestimo, taxaAnual, prazoMeses
        );
        BigDecimal valorTotal = LoanCalculator.calculateTotalAmount(
                parcela, prazoMeses
        );
        BigDecimal totalJuros = LoanCalculator.calculateTotalInterest(
                valorTotal, valorEmprestimo
        );

        // Verificar consistência matemática
        BigDecimal totalCalculado = parcela.multiply(BigDecimal.valueOf(prazoMeses));
        assertEquals(0, valorTotal.compareTo(totalCalculado));

        BigDecimal jurosCalculado = valorTotal.subtract(valorEmprestimo);
        assertEquals(0, totalJuros.compareTo(jurosCalculado));

        assertTrue(totalJuros.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(totalJuros.compareTo(valorEmprestimo) < 0);
    }

    /**
     * Performance test: 10.000 cálculos em <1s para suportar batch processing.
     */
    @Test
    @DisplayName("Deve calcular múltiplas parcelas rapidamente")
    void deveCalcularMultiplasParcelasRapidamente() {
        int numeroCalculos = 10000;
        BigDecimal valor = new BigDecimal("50000.00");
        BigDecimal taxa = new BigDecimal("3.5");
        int meses = 36;

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < numeroCalculos; i++) {
            LoanCalculator.calculateMonthlyPayment(valor, taxa, meses);
        }

        long tempoTotal = System.currentTimeMillis() - inicio;

        assertTrue(tempoTotal < 1000,
                String.format("10.000 cálculos devem levar menos de 1 segundo. Levou: %d ms", tempoTotal));

        double calculosPorSegundo = (numeroCalculos * 1000.0) / tempoTotal;
        System.out.printf("Performance: %.0f cálculos por segundo%n", calculosPorSegundo);
    }
}