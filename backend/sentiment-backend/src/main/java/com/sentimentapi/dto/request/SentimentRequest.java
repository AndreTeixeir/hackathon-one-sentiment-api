package com.sentimentapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de análise de sentimento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentimentRequest {

    @NotBlank(message = "O campo 'text' é obrigatório")
    @Size(min = 3, max = 2000, message = "O campo 'text' deve ter entre 3 e 2000 caracteres")
    private String text;
}
