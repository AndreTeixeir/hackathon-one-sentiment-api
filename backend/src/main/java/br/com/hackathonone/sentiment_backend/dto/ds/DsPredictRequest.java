package br.com.hackathonone.sentiment_backend.dto.ds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // <--- Essa é a anotação que resolve o seu erro (permite o construtor vazio)
@AllArgsConstructor
public class DsPredictRequest {
    // A API de Data Science espera receber um JSON com o campo "text"
    private String text;
}