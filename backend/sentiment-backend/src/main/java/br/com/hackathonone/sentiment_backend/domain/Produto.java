package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
// import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "produto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private BigDecimal preco;

    @Column(name = "imagem_url")
    private String imagemUrl;

    private String categoria;

    private String tags;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_vendedor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente clienteVendedor;

    @OneToMany(mappedBy = "produto")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comentario> comentarios;
}
