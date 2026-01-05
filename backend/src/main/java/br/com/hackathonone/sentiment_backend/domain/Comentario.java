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
@Table(name = "tb_comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT") // Permite textos longos
    private String texto;

    private LocalDateTime dataCriacao;

    // Quem escreveu?
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Sobre qual produto?
    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;
}