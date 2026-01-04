package br.com.hackathonone.sentiment_backend.dto.ds;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DsPredictRequest {
    private String text;
}