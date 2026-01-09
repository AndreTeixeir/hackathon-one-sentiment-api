package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "modelo_ml")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeloML {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String versao;

    @Column(name = "tipo_modelo")
    private String tipoModelo;

    @Column(name = "caminho_arquivo")
    private String caminhoArquivo;

    @Column(name = "f1_score", precision = 4, scale = 3)
    private BigDecimal f1Score;

    @Column(precision = 4, scale = 3)
    private BigDecimal acuracia;

    @Column(name = "data_treinamento")
    private LocalDateTime dataTreinamento;

    private Boolean ativo;

    @OneToMany(mappedBy = "modelo")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ResultadoAnalise> resultados;
}
