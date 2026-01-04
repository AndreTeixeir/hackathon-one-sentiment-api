package br.com.hackathonone.sentiment_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SentimentResponse {
    private String previsao;      // Mapeado de 'label'
    private Double probabilidade; // Mapeado de 'probability'
}