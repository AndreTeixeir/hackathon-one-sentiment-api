package br.com.hackathonone.sentiment_backend.domain;

import br.com.hackathonone.sentiment_backend.domain.enums.Sentimento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "resultado_analise")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Sentimento sentimento;

    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal probabilidade;

    @Column(name = "eh_critico", nullable = false)
    private Boolean ehCritico;

    @Column(name = "data_analise", nullable = false)
    private LocalDateTime dataAnalise;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Comentario comentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ModeloML modelo;

    @OneToMany(mappedBy = "resultadoAnalise")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Notificacao> notificacoes;
}
