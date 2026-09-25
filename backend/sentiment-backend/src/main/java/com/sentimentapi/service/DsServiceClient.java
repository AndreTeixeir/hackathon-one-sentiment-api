package com.sentimentapi.service;

import com.sentimentapi.dto.DsServiceHealth;
import com.sentimentapi.dto.DsServiceResponse;
import com.sentimentapi.exception.DsServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Cliente para comunicação com o microserviço de Data Science (FastAPI).
 */
@Service
@Slf4j
public class DsServiceClient {

    private final RestTemplate restTemplate;
    private final String dsServiceUrl;

    public DsServiceClient(
            RestTemplate restTemplate,
            @Value("${ds.service.url:http://localhost:8000}") String dsServiceUrl) {
        this.restTemplate = restTemplate;
        this.dsServiceUrl = dsServiceUrl;
    }

    /**
     * Envia texto para análise de sentimento no DS Service.
     *
     * @param text Texto a ser analisado
     * @return Resposta com label e probabilidade
     * @throws DsServiceException Se houver erro na comunicação
     */
    public DsServiceResponse predict(String text) {
        String url = dsServiceUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("text", text);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Enviando requisição para DS Service: {}", url);

            ResponseEntity<DsServiceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    DsServiceResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Resposta do DS Service: {}", response.getBody());
                return response.getBody();
            }

            throw new DsServiceException("Resposta inválida do DS Service: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Erro ao comunicar com DS Service: {}", e.getMessage());
            throw new DsServiceException("Falha na comunicação com o serviço de ML: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta o /health do DS Service, incluindo o modo de operação
     * (modelo real ou fallback heurístico).
     *
     * @return o health do DS Service, ou vazio se o serviço não respondeu
     */
    public Optional<DsServiceHealth> getHealth() {
        String url = dsServiceUrl + "/health";

        try {
            ResponseEntity<DsServiceHealth> response = restTemplate.getForEntity(url, DsServiceHealth.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("DS Service não está disponível: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
