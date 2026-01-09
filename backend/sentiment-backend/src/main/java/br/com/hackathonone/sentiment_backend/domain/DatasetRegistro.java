package br.com.hackathonone.sentiment_backend.domain;

import br.com.hackathonone.sentiment_backend.domain.enums.SplitDataset;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    private Short nota;

    @Column(name = "rotulo_original", nullable = false)
    private String rotuloOriginal; // POS / NEG / NEU

    private String fonte;

    @Enumerated(EnumType.STRING)
    private SplitDataset split;

    @Column(name = "data_importacao", nullable = false)
    private LocalDateTime dataImportacao;

    @Column(name = "id_externo")
    private String idExterno;
}
