package br.com.hackathonone.sentiment_backend.dto.ds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsPredictResponse {
    // O Service espera "prediction" e "probability"
    private String prediction;
    private Double probability;
}