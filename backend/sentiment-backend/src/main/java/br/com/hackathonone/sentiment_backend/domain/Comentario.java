package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "texto_original", columnDefinition = "TEXT", nullable = false)
    private String textoOriginal;

    // nota de 1 a 5 (pode ser nula se o cliente enviar só texto)
    private Short nota;

    private String origem;   // SITE / APP / OUTRO

    private String idioma;   // ex.: 'pt-BR'

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_comprador_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente clienteComprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Produto produto;

    // No MVP, 1:1 com ResultadoAnalise (um resultado por comentário)
    @OneToOne(mappedBy = "comentario")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ResultadoAnalise resultadoAnalise;
}
