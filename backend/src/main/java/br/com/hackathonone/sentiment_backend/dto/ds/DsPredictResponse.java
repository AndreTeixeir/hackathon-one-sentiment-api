package br.com.hackathonone.sentiment_backend.dto.ds;

import lombok.Data;

@Data
public class DsPredictResponse {
    private String label;
    private Double probability;
}