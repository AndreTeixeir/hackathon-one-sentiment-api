package br.com.hackathonone.sentiment_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SentimentRequest {

    @NotBlank(message = "O campo 'text' é obrigatório.")
    @Size(min = 3, message = "O texto deve ter no mínimo 3 caracteres.")
    private String text;
}