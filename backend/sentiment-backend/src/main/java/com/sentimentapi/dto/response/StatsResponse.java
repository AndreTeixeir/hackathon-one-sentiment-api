package com.sentimentapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de estatísticas de análises de sentimento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsResponse {

    @JsonProperty("total_analises")
    private Long totalAnalises;

    @JsonProperty("positivos")
    private Long positivos;

    @JsonProperty("negativos")
    private Long negativos;

    @JsonProperty("percentual_positivos")
    private Double percentualPositivos;

    @JsonProperty("percentual_negativos")
    private Double percentualNegativos;

    @JsonProperty("probabilidade_media_positivos")
    private Double probabilidadeMediaPositivos;

    @JsonProperty("probabilidade_media_negativos")
    private Double probabilidadeMediaNegativos;

    @JsonProperty("tempo_medio_processamento_ms")
    private Double tempoMedioProcessamentoMs;
}
