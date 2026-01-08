package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_resultado_analise")
public class ResultadoAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String previsao; // Ex: "Positivo", "Negativo"
    private Double probabilidade; // Ex: 0.95
    private LocalDateTime dataAnalise;

    // Cada análise pertence a UM comentário específico
    @OneToOne
    @JoinColumn(name = "comentario_id")
    private Comentario comentario;
}