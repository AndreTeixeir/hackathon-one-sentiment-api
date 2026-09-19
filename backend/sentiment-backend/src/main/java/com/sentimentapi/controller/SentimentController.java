package com.sentimentapi.controller;

import com.sentimentapi.dto.request.BatchSentimentRequest;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.BatchSentimentResponse;
import com.sentimentapi.dto.response.SentimentResponse;
import com.sentimentapi.service.SentimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para análise de sentimento.
 * Implementa o MVP do Hackathon One.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sentiment", description = "Endpoints para análise de sentimento")
public class SentimentController {

    private final SentimentService sentimentService;

    /**
     * Endpoint principal de análise de sentimento (MVP).
     * Recebe um texto e retorna a previsão com probabilidade.
     */
    @PostMapping("/sentiment")
    @Operation(
            summary = "Analisar sentimento de um texto",
            description = "Recebe um texto e retorna a classificação de sentimento (Positivo/Negativo) com a probabilidade associada"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Análise realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = SentimentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "503", description = "Serviço de ML indisponível")
    })
    public ResponseEntity<SentimentResponse> analisarSentimento(
            @Valid @RequestBody SentimentRequest request) {

        log.info("Recebida requisição de análise: {} caracteres", request.getText().length());

        SentimentResponse response = sentimentService.analisar(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para análise de sentimento em lote (batch processing).
     * Recebe múltiplos textos e retorna as previsões de todos.
     */
    @PostMapping("/sentiment/batch")
    @Operation(
            summary = "Analisar sentimento de múltiplos textos",
            description = "Recebe uma lista de textos e retorna a classificação de sentimento para cada um"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Análise em lote realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = BatchSentimentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "503", description = "Serviço de ML indisponível")
    })
    public ResponseEntity<BatchSentimentResponse> analisarBatch(
            @Valid @RequestBody BatchSentimentRequest request) {

        log.info("Recebida requisição de batch: {} textos", request.getTexts().size());

        BatchSentimentResponse response = sentimentService.analisarBatch(request);

        return ResponseEntity.ok(response);
    }
}
