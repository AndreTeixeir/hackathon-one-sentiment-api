CREATE TABLE cliente (
                         id            BIGSERIAL PRIMARY KEY,
                         nome          VARCHAR(100) NOT NULL,
                         email         VARCHAR(150),
                         tipo_cliente  VARCHAR(30)  NOT NULL, -- CLIENTE_COMPRADOR / CLIENTE_VENDEDOR / ADMIN
                         criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uq_cliente_email UNIQUE (email),
                         CONSTRAINT ck_cliente_tipo_cliente
                             CHECK (tipo_cliente IN ('CLIENTE_COMPRADOR', 'CLIENTE_VENDEDOR', 'ADMIN'))
);

CREATE TABLE produto (
                         id                   BIGSERIAL PRIMARY KEY,
                         nome                 VARCHAR(150) NOT NULL,
                         preco                NUMERIC(10,2) NOT NULL,
                         imagem_url           VARCHAR(255),
                         categoria            VARCHAR(100),
                         tags                 VARCHAR(255),
                         descricao            TEXT,
                         cliente_vendedor_id  BIGINT NOT NULL,

                         CONSTRAINT fk_produto_cliente_vendedor
                             FOREIGN KEY (cliente_vendedor_id) REFERENCES cliente(id)
                                 ON DELETE CASCADE
);

CREATE TABLE comentario (
                            id                    BIGSERIAL PRIMARY KEY,
                            texto_original        TEXT        NOT NULL,
                            nota                  SMALLINT,              -- 1 a 5
                            origem                VARCHAR(50),           -- SITE / APP / OUTRO
                            idioma                VARCHAR(10) DEFAULT 'pt-BR',
                            data_criacao          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            cliente_comprador_id  BIGINT,
                            produto_id            BIGINT      NOT NULL,

                            CONSTRAINT fk_comentario_cliente_comprador
                                FOREIGN KEY (cliente_comprador_id) REFERENCES cliente(id)
                                    ON DELETE SET NULL,
                            CONSTRAINT fk_comentario_produto
                                FOREIGN KEY (produto_id) REFERENCES produto(id)
                                    ON DELETE CASCADE,
                            CONSTRAINT ck_comentario_nota
                                CHECK (nota IS NULL OR (nota >= 1 AND nota <= 5))
);

CREATE TABLE modelo_ml (
                           id               BIGSERIAL PRIMARY KEY,
                           nome             VARCHAR(100) NOT NULL,
                           versao           VARCHAR(20)  NOT NULL,
                           tipo_modelo      VARCHAR(50)  NOT NULL,
                           caminho_arquivo  VARCHAR(255) NOT NULL,
                           f1_score         NUMERIC(4,3),
                           acuracia         NUMERIC(4,3),
                           data_treinamento TIMESTAMP,
                           ativo            BOOLEAN      NOT NULL DEFAULT FALSE,

                           CONSTRAINT uq_modelo_nome_versao UNIQUE (nome, versao)
);

CREATE TABLE resultado_analise (
                                   id            BIGSERIAL PRIMARY KEY,
                                   sentimento    VARCHAR(20)  NOT NULL,           -- POSITIVO / NEGATIVO / NEUTRO
                                   probabilidade NUMERIC(3,2) NOT NULL,           -- 0.00 a 1.00
                                   eh_critico    BOOLEAN      NOT NULL DEFAULT FALSE,
                                   data_analise  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   comentario_id BIGINT       NOT NULL,
                                   modelo_id     BIGINT,

                                   CONSTRAINT fk_resultado_comentario
                                       FOREIGN KEY (comentario_id) REFERENCES comentario(id)
                                           ON DELETE CASCADE,
                                   CONSTRAINT fk_resultado_modelo
                                       FOREIGN KEY (modelo_id) REFERENCES modelo_ml(id)
                                           ON DELETE SET NULL,
                                   CONSTRAINT ck_resultado_sentimento
                                       CHECK (sentimento IN ('POSITIVO', 'NEGATIVO', 'NEUTRO')),
                                   CONSTRAINT ck_resultado_probabilidade
                                       CHECK (probabilidade >= 0.0 AND probabilidade <= 1.0)
);

CREATE TABLE notificacao (
                             id           BIGSERIAL PRIMARY KEY,
                             mensagem     VARCHAR(255) NOT NULL,
                             status       VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',   -- PENDENTE / ENVIADA / LIDA
                             canal        VARCHAR(20)  NOT NULL DEFAULT 'DASHBOARD',  -- DASHBOARD / EMAIL
                             data_criacao TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             data_envio   TIMESTAMP,

                             vendedor_id  BIGINT       NOT NULL,   -- Cliente - Vendedor
                             resultado_id BIGINT       NOT NULL,

                             CONSTRAINT fk_notif_vendedor
                                 FOREIGN KEY (vendedor_id) REFERENCES cliente(id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_notif_resultado
                                 FOREIGN KEY (resultado_id) REFERENCES resultado_analise(id)
                                     ON DELETE CASCADE,
                             CONSTRAINT ck_notif_status
                                 CHECK (status IN ('PENDENTE', 'ENVIADA', 'LIDA')),
                             CONSTRAINT ck_notif_canal
                                 CHECK (canal IN ('DASHBOARD', 'EMAIL'))
);

CREATE TABLE dataset_registro (
                                  id              BIGSERIAL PRIMARY KEY,
                                  texto           TEXT         NOT NULL,
                                  nota            SMALLINT,
                                  rotulo_original VARCHAR(20)  NOT NULL,  -- POS / NEG / NEU
                                  fonte           VARCHAR(100) NOT NULL,
                                  split           VARCHAR(10)  NOT NULL,  -- TRAIN / TEST / VALID
                                  data_importacao TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  id_externo      VARCHAR(50),

                                  CONSTRAINT ck_dataset_split
                                      CHECK (split IN ('TRAIN', 'TEST', 'VALID')),
                                  CONSTRAINT ck_dataset_nota
                                      CHECK (nota IS NULL OR (nota >= 1 AND nota <= 5))
);

CREATE TABLE log_evento (
                            id            BIGSERIAL PRIMARY KEY,
                            nivel         VARCHAR(10)   NOT NULL,   -- INFO / WARN / ERROR
                            origem        VARCHAR(30)   NOT NULL,   -- API / ML_SERVICE / FRONTEND / DB
                            mensagem      VARCHAR(255)  NOT NULL,
                            detalhe_json  TEXT,
                            data_evento   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            cliente_id    BIGINT,
                            comentario_id BIGINT,

                            CONSTRAINT fk_log_cliente
                                FOREIGN KEY (cliente_id) REFERENCES cliente(id)
                                    ON DELETE SET NULL,
                            CONSTRAINT fk_log_comentario
                                FOREIGN KEY (comentario_id) REFERENCES comentario(id)
                                    ON DELETE SET NULL,
                            CONSTRAINT ck_log_nivel
                                CHECK (nivel IN ('INFO', 'WARN', 'ERROR'))
);

-- =========================================================
-- ÍNDICES PARA DESEMPENHO EM CONSULTAS FREQUENTES
-- =========================================================

-- Comentários filtrados por produto e comprador
CREATE INDEX idx_comentario_produto
    ON comentario (produto_id);

CREATE INDEX idx_comentario_cliente
    ON comentario (cliente_comprador_id);

-- Resultados de análise por sentimento e críticos (dashboard / notificações)
CREATE INDEX idx_resultado_sentimento
    ON resultado_analise (sentimento);

CREATE INDEX idx_resultado_eh_critico
    ON resultado_analise (eh_critico);

-- Notificações por status e por vendedor
CREATE INDEX idx_notificacao_status
    ON notificacao (status);

CREATE INDEX idx_notificacao_vendedor
    ON notificacao (vendedor_id);

-- Dataset: consultas por split (TRAIN / TEST / VALID)
CREATE INDEX idx_dataset_split
    ON dataset_registro (split);

-- Logs: consultas por nível, origem e data
CREATE INDEX idx_log_nivel
    ON log_evento (nivel);

CREATE INDEX idx_log_origem
    ON log_evento (origem);

CREATE INDEX idx_log_data_evento
    ON log_evento (data_evento);
