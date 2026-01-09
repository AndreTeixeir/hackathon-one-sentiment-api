package br.com.hackathonone.sentiment_backend.service;

import br.com.hackathonone.sentiment_backend.dto.api.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.api.SentimentResponse;

public interface SentimentService {

    SentimentResponse analisarSentimento(SentimentRequest request);
}
