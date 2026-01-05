package br.com.hackathonone.sentiment_backend.service;

import br.com.hackathonone.sentiment_backend.client.SentimentDsClient;
import br.com.hackathonone.sentiment_backend.domain.Comentario;
import br.com.hackathonone.sentiment_backend.domain.ResultadoAnalise;
import br.com.hackathonone.sentiment_backend.dto.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.SentimentResponse;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictRequest;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictResponse;
import br.com.hackathonone.sentiment_backend.repository.ComentarioRepository;
import br.com.hackathonone.sentiment_backend.repository.ResultadoAnaliseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor // Cria o construtor automaticamente com as dependências (Repositories e Client)
public class SentimentService {

    private final SentimentDsClient sentimentDsClient;
    private final ComentarioRepository comentarioRepository;
    private final ResultadoAnaliseRepository resultadoAnaliseRepository;

    public SentimentResponse analisarSentimento(SentimentRequest request) {
        // 1. Salvar o Comentário no Banco de Dados (Antes mesmo de analisar)
        Comentario comentarioSalvo = salvarComentario(request.getTexto());

        // 2. Chamar a API de Data Science (A "IA")
        DsPredictRequest dsRequest = new DsPredictRequest();
        dsRequest.setText(request.getTexto());

        DsPredictResponse dsResponse = sentimentDsClient.predict(dsRequest);

        // 3. Salvar o Resultado da Análise no Banco de Dados
        salvarResultado(comentarioSalvo, dsResponse);

        // 4. Retornar a resposta para quem chamou (Controller/Frontend)
        return SentimentResponse.builder()
                .textoOriginal(request.getTexto())
                .sentimento(dsResponse.getPrediction())
                .probabilidade(dsResponse.getProbability())
                .build();
    }

    private Comentario salvarComentario(String texto) {
        Comentario comentario = Comentario.builder()
                .texto(texto)
                .dataCriacao(LocalDateTime.now())
                // .cliente(...) -> Futuramente podemos vincular um cliente aqui
                // .produto(...) -> Futuramente podemos vincular um produto aqui
                .build();
        return comentarioRepository.save(comentario);
    }

    private void salvarResultado(Comentario comentario, DsPredictResponse dsResponse) {
        ResultadoAnalise resultado = ResultadoAnalise.builder()
                .previsao(dsResponse.getPrediction())      // Ex: "Positivo"
                .probabilidade(dsResponse.getProbability()) // Ex: 0.98
                .dataAnalise(LocalDateTime.now())
                .comentario(comentario) // Aqui fazemos o vínculo (Join)
                .build();
        resultadoAnaliseRepository.save(resultado);
    }
}