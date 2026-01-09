package br.com.hackathonone.sentiment_backend.domain;

import br.com.hackathonone.sentiment_backend.domain.enums.NivelLog;
import br.com.hackathonone.sentiment_backend.domain.enums.OrigemLog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_evento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NivelLog nivel;

    @Enumerated(EnumType.STRING)
    private OrigemLog origem;

    private String mensagem;

    @Column(name = "detalhe_json", columnDefinition = "TEXT")
    private String detalheJson;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Comentario comentario;
}
