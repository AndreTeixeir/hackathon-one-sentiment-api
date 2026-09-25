package com.sentimentapi.service;

import com.sentimentapi.domain.entity.AnaliseResultado;
import com.sentimentapi.domain.enums.Sentimento;
import com.sentimentapi.dto.DsServiceResponse;
import com.sentimentapi.dto.request.BatchSentimentRequest;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.BatchSentimentResponse;
import com.sentimentapi.dto.response.SentimentResponse;
import com.sentimentapi.exception.DsServiceException;
import com.sentimentapi.repository.AnaliseResultadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço principal para análise de sentimento.
 * Integra com o DS Service e persiste os resultados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentService {

    private final DsServiceClient dsServiceClient;
    private final AnaliseResultadoRepository analiseRepository;

    /**
     * Analisa o sentimento de um texto.
     *
     * @param request Requisição com o texto
     * @return Resposta com previsão e probabilidade
     */
    @Transactional
    public SentimentResponse analisar(SentimentRequest request) {
        return analisar(request, "API", null);
    }

    /**
     * Analisa o sentimento de um texto com metadados adicionais.
     *
     * @param request Requisição com o texto
     * @param origem  Origem da requisição (API, BATCH, WEB)
     * @param batchId ID do batch (se aplicável)
     * @return Resposta com previsão e probabilidade
     */
    @Transactional
    public SentimentResponse analisar(SentimentRequest request, String origem, String batchId) {
        long startTime = System.currentTimeMillis();

        // Chama o DS Service
        DsServiceResponse dsResponse = dsServiceClient.predict(request.getText());

        long processingTime = System.currentTimeMillis() - startTime;

        // Converte o label para o enum. Um rótulo não reconhecido é uma
        // violação de contrato do DS Service (não um erro interno nosso) —
        // reempacotado como DsServiceException para responder 503, não 500.
        Sentimento sentimento;
        try {
            sentimento = Sentimento.fromLabel(dsResponse.getLabel());
        } catch (IllegalArgumentException e) {
            throw new DsServiceException(
                    "DS Service devolveu um rótulo de sentimento não reconhecido: " + e.getMessage(), e);
        }

        // Persiste o resultado
        AnaliseResultado resultado = AnaliseResultado.builder()
                .textoOriginal(request.getText())
                .sentimento(sentimento)
                .probabilidade(dsResponse.getProbability())
                .dataAnalise(LocalDateTime.now())
                .tempoProcessamentoMs(processingTime)
                .origem(origem)
                .batchId(batchId)
                .build();

        analiseRepository.save(resultado);

        log.info("Análise concluída: sentimento={}, probabilidade={}, tempo={}ms",
                sentimento, dsResponse.getProbability(), processingTime);

        // Retorna a resposta no formato do hackathon
        return SentimentResponse.builder()
                .previsao(sentimento.getLabel())
                .probabilidade(dsResponse.getProbability())
                .build();
    }

    /**
     * Analisa múltiplos textos em lote (batch processing).
     *
     * @param request Requisição com lista de textos
     * @return Resposta com resultados de todos os textos
     */
    @Transactional
    public BatchSentimentResponse analisarBatch(BatchSentimentRequest request) {
        long startTime = System.currentTimeMillis();
        String batchId = UUID.randomUUID().toString();

        List<BatchSentimentResponse.BatchItemResponse> resultados = new ArrayList<>();

        for (SentimentRequest item : request.getTexts()) {
            try {
                SentimentResponse response = analisar(item, "BATCH", batchId);

                resultados.add(BatchSentimentResponse.BatchItemResponse.builder()
                        .texto(item.getText())
                        .previsao(response.getPrevisao())
                        .probabilidade(response.getProbabilidade())
                        .build());

            } catch (Exception e) {
                log.error("Erro ao processar item do batch: {}", e.getMessage());
                resultados.add(BatchSentimentResponse.BatchItemResponse.builder()
                        .texto(item.getText())
                        .previsao("ERRO")
                        .probabilidade(0.0)
                        .build());
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("Batch concluído: {} itens processados em {}ms", resultados.size(), totalTime);

        return BatchSentimentResponse.builder()
                .batchId(batchId)
                .total(resultados.size())
                .resultados(resultados)
                .tempoTotalMs(totalTime)
                .build();
    }
}
