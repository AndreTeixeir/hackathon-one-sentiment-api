package br.com.hackathonone.sentiment_backend.controller;

import br.com.hackathonone.sentiment_backend.dto.api.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.api.SentimentResponse;
import br.com.hackathonone.sentiment_backend.service.SentimentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sentiment")
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @PostMapping
    public ResponseEntity<SentimentResponse> analisar(@Valid @RequestBody SentimentRequest request) {
        SentimentResponse resposta = sentimentService.analisarSentimento(request);
        return ResponseEntity.ok(resposta);
    }
}
