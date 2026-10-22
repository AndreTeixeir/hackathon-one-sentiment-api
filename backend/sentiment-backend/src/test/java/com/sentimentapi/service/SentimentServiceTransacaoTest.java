package com.sentimentapi.service;

import com.sentimentapi.domain.entity.AnaliseResultado;
import com.sentimentapi.dto.DsServiceResponse;
import com.sentimentapi.dto.request.BatchSentimentRequest;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.BatchSentimentResponse;
import com.sentimentapi.exception.DsServiceException;
import com.sentimentapi.repository.AnaliseResultadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Garante que a chamada ao ds-service acontece sem transação aberta e que
 * cada item do lote é gravado na sua própria transação curta. Usa o contexto
 * completo e o H2 real: um mock de repositório não reproduz o rollback.
 */
@SpringBootTest
@ActiveProfiles("dev")
class SentimentServiceTransacaoTest {

    @Autowired
    private SentimentService sentimentService;

    @Autowired
    private AnaliseResultadoRepository repository;

    @MockBean
    private DsServiceClient dsServiceClient;

    @Test
    @DisplayName("analisar: não há transação ativa durante a chamada ao ds-service, e o resultado é gravado")
    void analisarChamaDsServiceSemTransacao() {
        List<Boolean> transacaoAtivaNasChamadas = new ArrayList<>();
        when(dsServiceClient.predict(anyString())).thenAnswer(invocacao -> {
            transacaoAtivaNasChamadas.add(TransactionSynchronizationManager.isActualTransactionActive());
            return new DsServiceResponse("Positivo", 0.9);
        });
        long antes = repository.count();

        sentimentService.analisar(new SentimentRequest("Atendimento excelente, voltarei com certeza"));

        assertThat(transacaoAtivaNasChamadas).containsExactly(false);
        assertThat(repository.count()).isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("analisarBatch: não há transação ativa durante a chamada ao ds-service de nenhum item")
    void analisarBatchChamaDsServiceSemTransacao() {
        List<Boolean> transacaoAtivaNasChamadas = new ArrayList<>();
        when(dsServiceClient.predict(anyString())).thenAnswer(invocacao -> {
            transacaoAtivaNasChamadas.add(TransactionSynchronizationManager.isActualTransactionActive());
            return new DsServiceResponse("Negativo", 0.8);
        });

        BatchSentimentResponse resposta = sentimentService.analisarBatch(batchDe(
                "Primeiro texto do lote sem transação",
                "Segundo texto do lote sem transação",
                "Terceiro texto do lote sem transação"));

        assertThat(resposta.getTotal()).isEqualTo(3);
        assertThat(transacaoAtivaNasChamadas).containsExactly(false, false, false);
        assertThat(repository.findByBatchId(resposta.getBatchId())).hasSize(3);
    }

    @Test
    @DisplayName("analisarBatch: item cuja gravação falha vira ERRO e os demais continuam gravados")
    void itemComFalhaDeGravacaoViraErroEOsOutrosSaoGravados() {
        String bom1 = "Lote com falha de gravação: primeiro item bom";
        String ruim = "Lote com falha de gravação: item que o banco recusa";
        String bom2 = "Lote com falha de gravação: terceiro item bom";
        when(dsServiceClient.predict(bom1)).thenReturn(new DsServiceResponse("Positivo", 0.91));
        // probabilidade nula viola o NOT NULL da coluna: o banco recusa só este INSERT
        when(dsServiceClient.predict(ruim)).thenReturn(new DsServiceResponse("Negativo", null));
        when(dsServiceClient.predict(bom2)).thenReturn(new DsServiceResponse("Negativo", 0.77));

        BatchSentimentResponse resposta = sentimentService.analisarBatch(batchDe(bom1, ruim, bom2));

        assertThat(resposta.getTotal()).isEqualTo(3);
        assertThat(resposta.getResultados()).extracting(BatchSentimentResponse.BatchItemResponse::getPrevisao)
                .containsExactly("Positivo", "ERRO", "Negativo");
        assertThat(repository.findByBatchId(resposta.getBatchId()))
                .extracting(AnaliseResultado::getTextoOriginal)
                .containsExactlyInAnyOrder(bom1, bom2);
    }

    @Test
    @DisplayName("analisarBatch: item que falha no ds-service vira ERRO e os demais continuam gravados")
    void itemComFalhaNoDsServiceViraErroEOsOutrosSaoGravados() {
        String bom1 = "Lote com ds-service fora: primeiro item bom";
        String falha = "Lote com ds-service fora: item que derruba a chamada";
        String bom2 = "Lote com ds-service fora: terceiro item bom";
        when(dsServiceClient.predict(bom1)).thenReturn(new DsServiceResponse("Positivo", 0.88));
        when(dsServiceClient.predict(falha)).thenThrow(new DsServiceException("ds-service indisponível"));
        when(dsServiceClient.predict(bom2)).thenReturn(new DsServiceResponse("Positivo", 0.66));

        BatchSentimentResponse resposta = sentimentService.analisarBatch(batchDe(bom1, falha, bom2));

        assertThat(resposta.getResultados()).extracting(BatchSentimentResponse.BatchItemResponse::getPrevisao)
                .containsExactly("Positivo", "ERRO", "Positivo");
        assertThat(repository.findByBatchId(resposta.getBatchId()))
                .extracting(AnaliseResultado::getTextoOriginal)
                .containsExactlyInAnyOrder(bom1, bom2);
    }

    private static BatchSentimentRequest batchDe(String... textos) {
        List<SentimentRequest> itens = new ArrayList<>();
        for (String texto : textos) {
            itens.add(new SentimentRequest(texto));
        }
        return BatchSentimentRequest.builder().texts(itens).build();
    }
}
