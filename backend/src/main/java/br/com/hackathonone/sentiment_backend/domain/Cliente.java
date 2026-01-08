package br.com.hackathonone.sentiment_backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Gera Getters, Setters, toString, etc. automaticamente (Lombok)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity // Diz ao JPA que isso vira uma tabela no banco
@Table(name = "tb_cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
}