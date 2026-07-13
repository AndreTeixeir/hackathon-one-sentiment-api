# Sentiment API - Hackathon One 🚀

API para análise de sentimentos em textos utilizando Machine Learning, desenvolvida para a etapa final do Hackathon One.

## 📋 Visão Geral

Este projeto implementa uma solução completa de análise de sentimentos que classifica comentários (de e-commerce, redes sociais, etc.) como **Positivo**, **Negativo** ou **Neutro**.

A solução integra um backend robusto em **Java/Spring Boot** com um microserviço de **Data Science em Python**, orquestrados via Docker.

## 🔗 Demonstração Online (Live Demo)

O projeto está implantado e acessível na Oracle Cloud Infrastructure (OCI).

> Infraestrutura e deploy em OCI (provisionamento da instância e scripts de automação): **André Teixeira**.

| Componente | URL |
| :--- | :--- |
| **API & Backend** | [http://152.67.61.11:8080/](http://152.67.61.11:8080/) |
| **Documentação da API (Swagger)** | [http://152.67.61.11:8080/swagger-ui.html](http://152.67.61.11:8080/swagger-ui.html) |
| **Frontend** | [http://152.67.61.11](http://152.67.61.11) |

## 📺 Vídeo de Demonstração

Assista ao vídeo de apresentação e demonstração do projeto:

[![Demonstração Sentiment API](http://img.youtube.com/vi/aOJWGQSNn5k/0.jpg)](https://www.youtube.com/watch?v=aOJWGQSNn5k)

---

## 🛠️ Tecnologias Utilizadas

| Camada | Tecnologias |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.2, Spring Data JPA, Lombok |
| **Data Science** | Python 3.11, FastAPI, Scikit-learn, Pandas |
| **Banco de Dados** | PostgreSQL 15 |
| **Infraestrutura** | Docker, Docker Compose, Oracle Cloud (OCI) |

## 🏗️ Arquitetura do Sistema

O diagrama a seguir ilustra a arquitetura de microsserviços do projeto:

```mermaid
graph TD
    User[Cliente/Frontend] -->|HTTP/REST| Backend[Backend Spring Boot :8080]
    Backend -->|Persistência| DB[(PostgreSQL :5432)]
    Backend -->|Inferência ML| DS[DS Service Python :8000]
```

## 🚀 Como Executar Localmente

### Pré-requisitos

Certifique-se de ter o **Docker** e o **Docker Compose** instalados em sua máquina, além do **Git**.

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/AndreTeixeir/hackathon-one-sentiment-api.git
    cd hackathon-one-sentiment-api
    ```

2.  **Suba os containers:**
    ```bash
    docker-compose up -d --build
    ```

3.  **Acesse os serviços:**
    *   **API:** `http://localhost:8080/api/v1/sentiment`
    *   **DS Service Health:** `http://localhost:8000/health`

## 🔌 Endpoints Principais

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/sentiment` | Analisa um texto avulso e retorna o sentimento (Positivo, Negativo, Neutro). |
| `POST` | `/api/v1/comentarios` | Regista um comentário no banco de dados e o classifica automaticamente. |
| `GET` | `/api/v1/dashboard/stats/{id}` | Retorna estatísticas agregadas de sentimentos para um vendedor específico. |

> Para a lista completa de endpoints, consulte a documentação Swagger na URL de demonstração.

## 📂 Estrutura do Projeto

*   `/backend`: Código fonte da API principal em Java/Spring Boot.
*   `/ds-service`: Microserviço Python de Machine Learning (FastAPI).
*   `/datascience`: Notebooks (Jupyter) de treino do modelo e datasets.
*   `/docs`: Documentação técnica detalhada (Arquitetura, Requisitos, Diagramas).
*   `/scripts`: Scripts de automação para deploy na OCI.

## 👥 A Equipa (Participantes)

| Participante | LinkedIn |
| :--- | :--- |
| **Eiky Oliveira Albuquerque** | [Perfil LinkedIn](https://www.linkedin.com/in/eikyalbuquerque) |
| **Brena Stephany Chagas Paula** | [Perfil LinkedIn](https://www.linkedin.com/in/brena-stephany) |
| **Luiz Carlos Tannous Del Nero** | [Perfil LinkedIn](https://www.linkedin.com/in/luiz-carlos-tannous-del-nero-b44166255) |
| **Letícia de Almeida Ferreira** | [Perfil LinkedIn](http://linkedin.com/in/leticia-de-almeida-ferreira-18086a180) |
| **André Teixeira** | [Perfil LinkedIn](https://www.linkedin.com/in/andreteixeir) |

## 📄 Licença

Este projeto está sob a licença [MIT](https://opensource.org/licenses/MIT).
