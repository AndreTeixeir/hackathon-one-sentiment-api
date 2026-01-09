package br.com.hackathonone.sentiment_backend.domain;

import br.com.hackathonone.sentiment_backend.domain.enums.TipoCliente;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false, length = 30)
    private TipoCliente tipoCliente;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    // Relacionamentos "de leitura", não obrigatórios no MVP,
    // mas úteis se quiser navegar no domínio:

    @OneToMany(mappedBy = "clienteVendedor")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Produto> produtos;

    @OneToMany(mappedBy = "clienteComprador")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comentario> comentarios;

    @OneToMany(mappedBy = "vendedor")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Notificacao> notificacoes;

    @OneToMany(mappedBy = "cliente")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LogEvento> logs;
}
