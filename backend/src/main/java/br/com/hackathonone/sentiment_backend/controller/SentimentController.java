package br.com.hackathonone.sentiment_backend.controller;

import br.com.hackathonone.sentiment_backend.dto.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.SentimentResponse;
import br.com.hackathonone.sentiment_backend.service.SentimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentiments")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentService service;

    @PostMapping
    public ResponseEntity<SentimentResponse> analyze(@RequestBody @Valid SentimentRequest request) {
        // Correção: O nome do método no Service é "analisarSentimento"
        SentimentResponse response = service.analisarSentimento(request);
        return ResponseEntity.ok(response);
    }
}