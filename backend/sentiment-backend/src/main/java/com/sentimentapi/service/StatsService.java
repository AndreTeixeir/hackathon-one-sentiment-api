package com.sentimentapi.service;

import com.sentimentapi.domain.enums.Sentimento;
import com.sentimentapi.dto.response.StatsResponse;
import com.sentimentapi.repository.AnaliseResultadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço para cálculo de estatísticas de análises de sentimento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final AnaliseResultadoRepository analiseRepository;

    /**
     * Calcula estatísticas gerais de todas as análises.
     *
     * @return Estatísticas consolidadas
     */
    public StatsResponse getStats() {
        long total = analiseRepository.count();

        if (total == 0) {
            return StatsResponse.builder()
                    .totalAnalises(0L)
                    .positivos(0L)
                    .negativos(0L)
                    .percentualPositivos(0.0)
                    .percentualNegativos(0.0)
                    .probabilidadeMediaPositivos(0.0)
                    .probabilidadeMediaNegativos(0.0)
                    .tempoMedioProcessamentoMs(0.0)
                    .build();
        }

        long positivos = analiseRepository.countBySentimento(Sentimento.POSITIVO);
        long negativos = analiseRepository.countBySentimento(Sentimento.NEGATIVO);

        Double avgProbPositivos = analiseRepository.findAverageProbabilidadeBySentimento(Sentimento.POSITIVO);
        Double avgProbNegativos = analiseRepository.findAverageProbabilidadeBySentimento(Sentimento.NEGATIVO);
        Double avgTempo = analiseRepository.findAverageTempoProcessamento();

        return StatsResponse.builder()
                .totalAnalises(total)
                .positivos(positivos)
                .negativos(negativos)
                .percentualPositivos(calcularPercentual(positivos, total))
                .percentualNegativos(calcularPercentual(negativos, total))
                .probabilidadeMediaPositivos(avgProbPositivos != null ? avgProbPositivos : 0.0)
                .probabilidadeMediaNegativos(avgProbNegativos != null ? avgProbNegativos : 0.0)
                .tempoMedioProcessamentoMs(avgTempo != null ? avgTempo : 0.0)
                .build();
    }

    private Double calcularPercentual(long parte, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) parte / total * 10000.0) / 100.0;
    }
}
