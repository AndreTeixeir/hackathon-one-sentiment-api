package com.sentimentapi.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes para o enum Sentimento.
 */
class SentimentoTest {

    @ParameterizedTest
    @DisplayName("Deve converter labels positivos corretamente")
    @CsvSource({
            "Positivo, POSITIVO",
            "POSITIVO, POSITIVO",
            "POSITIVE, POSITIVO",
            "POS, POSITIVO",
            "1, POSITIVO"
    })
    void deveConverterLabelsPositivos(String input, Sentimento expected) {
        assertThat(Sentimento.fromLabel(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Deve converter labels negativos corretamente")
    @CsvSource({
            "Negativo, NEGATIVO",
            "NEGATIVO, NEGATIVO",
            "NEGATIVE, NEGATIVO",
            "NEG, NEGATIVO",
            "0, NEGATIVO"
    })
    void deveConverterLabelsNegativos(String input, Sentimento expected) {
        assertThat(Sentimento.fromLabel(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Deve lançar exceção para labels desconhecidos, nulos ou vazios")
    void deveLancarExcecaoParaLabelsDesconhecidos() {
        assertThatThrownBy(() -> Sentimento.fromLabel("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sentimento.fromLabel(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sentimento.fromLabel(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve retornar label correto para cada sentimento")
    void deveRetornarLabelCorreto() {
        assertThat(Sentimento.POSITIVO.getLabel()).isEqualTo("Positivo");
        assertThat(Sentimento.NEGATIVO.getLabel()).isEqualTo("Negativo");
    }
}
