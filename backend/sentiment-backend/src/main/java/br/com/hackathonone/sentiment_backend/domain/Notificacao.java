package br.com.hackathonone.sentiment_backend.domain;

import br.com.hackathonone.sentiment_backend.domain.enums.StatusNotificacao;
import br.com.hackathonone.sentiment_backend.domain.enums.CanalNotificacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mensagem;

    @Enumerated(EnumType.STRING)
    private StatusNotificacao status;

    @Enumerated(EnumType.STRING)
    private CanalNotificacao canal;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultado_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ResultadoAnalise resultadoAnalise;
}
