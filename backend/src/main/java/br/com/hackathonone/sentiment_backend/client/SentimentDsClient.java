package br.com.hackathonone.sentiment_backend.client;

import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictRequest;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ds-client", url = "${ds.base-url}")
public interface SentimentDsClient {

    @PostMapping("/predict")
    DsPredictResponse predict(@RequestBody DsPredictRequest request);
}