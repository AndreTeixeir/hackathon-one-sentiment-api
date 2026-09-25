package com.sentimentapi.service;

import com.sentimentapi.domain.enums.Sentimento;
import com.sentimentapi.dto.response.StatsResponse;
import com.sentimentapi.repository.AnaliseResultadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de StatsService — sem contexto Spring, repository mockado.
 * StatsService foi alterado na Etapa 3 (remoção do campo neutros) e não
 * tinha nenhum teste próprio; a cobertura de /api/v1/stats vinha só do
 * caminho feliz do teste de integração.
 */
class StatsServiceTest {

    private AnaliseResultadoRepository repository;
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        repository = mock(AnaliseResultadoRepository.class);
        statsService = new StatsService(repository);
    }

    @Test
    @DisplayName("Deve retornar tudo zerado quando não há nenhuma análise")
    void deveRetornarZerosQuandoNaoHaAnalises() {
        when(repository.count()).thenReturn(0L);

        StatsResponse stats = statsService.getStats();

        assertThat(stats.getTotalAnalises()).isZero();
        assertThat(stats.getPositivos()).isZero();
        assertThat(stats.getNegativos()).isZero();
        assertThat(stats.getPercentualPositivos()).isZero();
        assertThat(stats.getPercentualNegativos()).isZero();
        assertThat(stats.getProbabilidadeMediaPositivos()).isZero();
        assertThat(stats.getProbabilidadeMediaNegativos()).isZero();
        assertThat(stats.getTempoMedioProcessamentoMs()).isZero();

        // Caminho de total==0 é um atalho: não deve nem consultar contagem por sentimento.
        verify(repository, never()).countBySentimento(any());
        verify(repository, never()).findAverageProbabilidadeBySentimento(any());
    }

    @Test
    @DisplayName("Deve calcular percentuais corretamente com contagens exatas")
    void deveCalcularPercentuaisCorretamente() {
        when(repository.count()).thenReturn(100L);
        when(repository.countBySentimento(Sentimento.POSITIVO)).thenReturn(70L);
        when(repository.countBySentimento(Sentimento.NEGATIVO)).thenReturn(30L);
        when(repository.findAverageProbabilidadeBySentimento(Sentimento.POSITIVO)).thenReturn(0.85);
        when(repository.findAverageProbabilidadeBySentimento(Sentimento.NEGATIVO)).thenReturn(0.78);
        when(repository.findAverageTempoProcessamento()).thenReturn(42.5);

        StatsResponse stats = statsService.getStats();

        assertThat(stats.getTotalAnalises()).isEqualTo(100L);
        assertThat(stats.getPositivos()).isEqualTo(70L);
        assertThat(stats.getNegativos()).isEqualTo(30L);
        assertThat(stats.getPercentualPositivos()).isEqualTo(70.0);
        assertThat(stats.getPercentualNegativos()).isEqualTo(30.0);
        assertThat(stats.getProbabilidadeMediaPositivos()).isEqualTo(0.85);
        assertThat(stats.getProbabilidadeMediaNegativos()).isEqualTo(0.78);
        assertThat(stats.getTempoMedioProcessamentoMs()).isEqualTo(42.5);
    }

    @Test
    @DisplayName("Deve arredondar percentual para duas casas quando a divisão não é exata")
    void deveArredondarPercentualParaDuasCasas() {
        // 1/3 = 33.333...%, 2/3 = 66.666...% — testa o arredondamento real do
        // calcularPercentual (Math.round(x * 10000.0) / 100.0), não um valor redondo por coincidência.
        when(repository.count()).thenReturn(3L);
        when(repository.countBySentimento(Sentimento.POSITIVO)).thenReturn(1L);
        when(repository.countBySentimento(Sentimento.NEGATIVO)).thenReturn(2L);
        when(repository.findAverageProbabilidadeBySentimento(any())).thenReturn(null);
        when(repository.findAverageTempoProcessamento()).thenReturn(null);

        StatsResponse stats = statsService.getStats();

        assertThat(stats.getPercentualPositivos()).isEqualTo(33.33);
        assertThat(stats.getPercentualNegativos()).isEqualTo(66.67);
    }

    @Test
    @DisplayName("Deve tratar médias nulas do repositório como 0.0, sem lançar NPE")
    void deveTratarMediasNulasComoZero() {
        // Cenário real: total > 0, mas nenhuma análise ainda de uma das classes —
        // AVG(...) sobre zero linhas retorna NULL em SQL, não 0.
        when(repository.count()).thenReturn(5L);
        when(repository.countBySentimento(Sentimento.POSITIVO)).thenReturn(5L);
        when(repository.countBySentimento(Sentimento.NEGATIVO)).thenReturn(0L);
        when(repository.findAverageProbabilidadeBySentimento(Sentimento.POSITIVO)).thenReturn(0.9);
        when(repository.findAverageProbabilidadeBySentimento(Sentimento.NEGATIVO)).thenReturn(null);
        when(repository.findAverageTempoProcessamento()).thenReturn(null);

        StatsResponse stats = statsService.getStats();

        assertThat(stats.getProbabilidadeMediaPositivos()).isEqualTo(0.9);
        assertThat(stats.getProbabilidadeMediaNegativos()).isEqualTo(0.0);
        assertThat(stats.getTempoMedioProcessamentoMs()).isEqualTo(0.0);
    }
}
