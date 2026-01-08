package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    // Relacionamento: Vários produtos podem pertencer a um vendedor (Cliente)
    // Aqui simplificamos dizendo que um Cliente também pode ser vendedor.
    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private Cliente vendedor;
}