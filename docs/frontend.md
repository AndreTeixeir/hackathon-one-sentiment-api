# Frontend
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Visão geral

Este documento descreve o **frontend web** do projeto **Hackathon One Sentiment API**.

O foco do frontend é:

- oferecer uma UI simples para:
    - **Cliente - Comprador** enviar comentários sobre produtos;
    - **Cliente - Vendedor** cadastrar produtos e visualizar insights (dashboard, notificações, export);
- integrar com a **API Java (Spring Boot)** via HTTP/JSON;
- manter a experiência de uso clara, sem expor detalhes internos de ML para o comprador.

A ideia é ter um frontend **leve**, organizado por arquivos HTML/CSS/JS, fácil de rodar em ambiente local e de integrar futuramente com o backend hospedado (ex.: na OCI).

---

## 2. Objetivos do frontend

### 2.1. Para o Comprador

- Ver uma lista de produtos disponíveis.
- Ver detalhes de um produto (imagem, nome, preço, descrição, tags).
- Enviar um comentário (texto) e, opcionalmente, uma nota (1 a 5).
- Receber uma mensagem clara de confirmação do envio do comentário.

> O Comprador **não** vê o resultado de sentimento. Essa informação é interna e usada apenas pelo Vendedor / dashboard.

### 2.2. Para o Vendedor

- Cadastrar novos produtos.
- Consultar a lista de produtos cadastrados.
- Acessar um **dashboard** com:
    - estatísticas de sentimentos (positivos, negativos, neutros);
    - lista de comentários analisados;
    - notificações de comentários críticos.
- Marcar notificações como lidas.
- Exportar os dados de comentários em formato **JSON**.

### 2.3. Para o time técnico

- Ter um frontend simples o bastante para qualquer pessoa da equipe conseguir subir e testar.
- Facilitar a integração com a API através de:
    - um único ponto de configuração (`API_BASE_URL`);
    - contratos de JSON claros e estáveis.

---

## 3. Estrutura de pastas do frontend

Estrutura sugerida em `/frontend`:

```text
frontend/
├── web/
│   ├── login.html          # Tela de entrada (escolha de perfil)
│   ├── comprador.html      # Tela para Cliente - Comprador
│   ├── vendedor.html       # Tela para Cliente - Vendedor
│   ├── css/
│   │   └── styles.css      # Estilos globais da UI
│   └── js/
│       ├── config.js       # API_BASE_URL, enums, helpers globais
│       ├── login.js        # Lógica da tela de login
│       ├── comprador.js    # Lógica da tela do comprador
│       └── vendedor.js     # Lógica da tela do vendedor
└── streamlit/
    └── app.py              # (opcional) UI alternativa em Streamlit
````

* O frontend **web** é estático: pode ser servido por qualquer servidor HTTP simples ou aberto direto no navegador em ambiente de desenvolvimento.
* O arquivo `config.js` é o ponto central para alterar a URL da API.

---

## 4. Tecnologias usadas

* **HTML5** – Estrutura das páginas.
* **CSS3** – Estilo visual. Tudo concentrado em `css/styles.css`.
* **JavaScript (ES6+)** – Requisições à API, manipulação de DOM, lógica de fluxo.
* **Fetch API** – para chamadas HTTP à API Java.

Não está previsto, no MVP, uso de frameworks como React/Vue. Se o projeto evoluir, o frontend atual já serve de base lógica para uma migração.

---

## 5. Configuração de integração com a API

### 5.1. Ponto único de configuração

Arquivo: `frontend/web/js/config.js`

Exemplo:

```js
// URL base da API do backend (ambiente de desenvolvimento local)
const API_BASE_URL = "http://localhost:8080/api/v1";

// Tipos de cliente, alinhados ao enum TipoCliente no backend
const TipoCliente = {
  CLIENTE_COMPRADOR: "CLIENTE_COMPRADOR",
  CLIENTE_VENDEDOR: "CLIENTE_VENDEDOR",
  ADMIN: "ADMIN"
};
```

Se a API for publicada em outro lugar (por exemplo, na nuvem):

```js
// Exemplo para produção / nuvem
const API_BASE_URL = "https://api.minhaempresa.com/sentiment/api/v1";
```

### 5.2. CORS

O backend deve estar configurado para aceitar requisições originais do frontend (origem local ou domínio na nuvem).
Exemplos:

* Desenvolvimento local:

    * Frontend: `file://` ou `http://localhost:3000`
    * Backend: `http://localhost:8080`
* Produção:

    * Frontend: `https://app.minhaempresa.com`
    * Backend: `https://api.minhaempresa.com`

A configuração de CORS fica no backend, mas é importante o frontend saber que:

* se der erro de CORS, geralmente a chamada nem chega na API;
* erros de CORS precisam ser tratados/exibidos de forma amigável para o usuário (“Erro ao conectar com o servidor”).

---

## 6. Páginas e comportamento

### 6.1. login.html

#### 6.1.1. Objetivo

* Permitir escolher o perfil de uso:

    * Cliente - Comprador
    * Cliente - Vendedor
* Fazer um “cadastro simples” de cliente, se a API já estiver pronta pra isso, ou simular essa escolha no `localStorage`.

#### 6.1.2. Comportamento esperado

* Campos:

    * nome (opcional ou obrigatório, dependendo do backend)
    * email (opcional/obrigatório)
    * tipo de cliente (radio/button ou select):

        * **Cliente - Comprador**
        * **Cliente - Vendedor**
* Ao clicar em “Continuar”:

    * Se a API de clientes existir:

        * Enviar `POST /api/v1/clientes` com `{nome, email, tipoCliente}`.
        * Salvar o `id` retornado + `tipoCliente` no `localStorage` como `clienteAtual`.
    * Se a API ainda não existir:

        * Criar um objeto falso em memória (`id: 1`, por exemplo) só para teste local.
* Redirecionar:

    * Se `tipoCliente = CLIENTE_COMPRADOR` → `comprador.html`
    * Se `tipoCliente = CLIENTE_VENDEDOR` → `vendedor.html`

#### 6.1.3. Exemplo de JSON esperado / retornado

Requisição (quando existe API de cliente):

```http
POST {API_BASE_URL}/clientes
Content-Type: application/json
```

```json
{
  "nome": "Ana Silva",
  "email": "ana@example.com",
  "tipoCliente": "CLIENTE_COMPRADOR"
}
```

Resposta:

```json
{
  "id": 1,
  "nome": "Ana Silva",
  "email": "ana@example.com",
  "tipoCliente": "CLIENTE_COMPRADOR",
  "criadoEm": "2025-01-01T10:00:00"
}
```

O `login.js` deve:

* guardar esse objeto (ou pelo menos `id` e `tipoCliente`) no `localStorage`:

```js
localStorage.setItem("clienteAtual", JSON.stringify(respostaJson));
```

* fazer `window.location.href = "comprador.html";` ou `"vendedor.html"`.

---

### 6.2. comprador.html

#### 6.2.1. Objetivo

* Mostrar a lista de produtos disponíveis.
* Permitir que o comprador abra o detalhe de um produto.
* Permitir que ele envie um comentário e nota sobre o produto.
* **Não** exibir o sentimento previsto. Apenas agradecer pelo feedback.

#### 6.2.2. Elementos principais

* Header com:

    * nome/saudação do cliente (se disponível no `localStorage`);
    * botão “Sair” que limpa `localStorage` e volta para `login.html`.
* Seção “Produtos”:

    * cards com nome, preço, imagem, categoria, tags.
    * botão “Ver detalhes”.
* Modal ou seção de “Detalhes do Produto”:

    * imagem maior,
    * descrição,
    * campo de texto para comentário,
    * seleção de nota (1 a 5, ex: estrelas),
    * botão “Enviar”.

#### 6.2.3. Endpoints usados

1. **Listar produtos**

   ```http
   GET {API_BASE_URL}/produtos
   ```

   Resposta (exemplo):

   ```json
   [
     {
       "id": 10,
       "nome": "Fone de Ouvido Bluetooth",
       "preco": 249.9,
       "imagemUrl": "https://via.placeholder.com/400x260?text=Fone",
       "categoria": "Eletrônicos",
       "tags": "Bluetooth,Confortável",
       "descricao": "Fone leve, bateria de longa duração.",
       "clienteVendedorId": 2
     }
   ]
   ```

2. **Enviar comentário**

   ```http
   POST {API_BASE_URL}/comentarios
   Content-Type: application/json
   ```

   Corpo esperado:

   ```json
   {
     "texto": "Gostei muito, chegou rápido.",
     "nota": 5,
     "origem": "SITE",
     "idioma": "pt-BR",
     "produtoId": 10,
     "clienteCompradorId": 1
   }
   ```

   Resposta esperada para o comprador (MVP):

   ```json
   {
     "mensagem": "Comentário registrado com sucesso."
   }
   ```

   > A análise de sentimento acontece **no backend**, e o resultado é usado apenas internamente (para notificações e dashboard do vendedor).

#### 6.2.4. Tratamento de erro

* Se a API retornar 400 (validação):

    * mostrar mensagem amigável:

        * ex.: “Seu comentário precisa ter pelo menos 5 caracteres.”
* Se a API estiver indisponível / erro 500:

    * mostrar mensagem:

        * ex.: “Não foi possível enviar seu comentário. Tente novamente em alguns instantes.”

---

### 6.3. vendedor.html

#### 6.3.1. Objetivo

* Ser a “central” do **Cliente - Vendedor**:

    * Cadastro de produtos.
    * Visualização de produtos cadastrados.
    * Dashboard de sentimentos.
    * Lista de comentários.
    * Notificações (comentários críticos).
    * Exportação de dados em JSON.

#### 6.3.2. Seções da página

Sugestão de layout:

* **Header**:

    * nome do vendedor,
    * atalho para “Meus Produtos” / “Dashboard”,
    * botão de logout.
* **Aba “Meus Produtos”**:

    * Tabela/lista com produtos do vendedor.
    * Botão “Novo Produto”.
* **Aba “Dashboard”**:

    * Cards com contagem:

        * total de comentários,
        * % positivos, % negativos, % neutros,
        * número de comentários críticos.
    * Gráficos simples (se tiver tempo) ou cards com números.
* **Aba “Comentários”**:

    * Lista com:

        * texto do comentário,
        * nota,
        * data,
        * sentimento previsto,
        * probabilidade,
        * produto relacionado.
* **Aba “Notificações”**:

    * Lista de notificações com:

        * mensagem,
        * status (PENDENTE / LIDA),
        * data de criação,
        * botão “Marcar como lida”.
* **Botão “Exportar JSON”**:

    * Faz download dos dados em um arquivo `.json`.

#### 6.3.3. Endpoints usados

Assumindo que o vendedor atual foi salvo no `localStorage` como:

```json
{
  "id": 2,
  "tipoCliente": "CLIENTE_VENDEDOR",
  "nome": "Loja XYZ"
}
```

Vamos usar `vendedorId = 2` (ou `clienteVendedorId` nos parâmetros de query).

1. **Listar produtos do vendedor**

   ```http
   GET {API_BASE_URL}/produtos?vendedorId=2
   ```

   Resposta (exemplo):

   ```json
   [
     {
       "id": 10,
       "nome": "Fone de Ouvido Bluetooth",
       "preco": 249.9,
       "imagemUrl": "https://via.placeholder.com/400x260?text=Fone",
       "categoria": "Eletrônicos",
       "tags": "Bluetooth,Confortável",
       "descricao": "Fone leve...",
       "clienteVendedorId": 2
     }
   ]
   ```

2. **Cadastrar novo produto**

   ```http
   POST {API_BASE_URL}/produtos
   Content-Type: application/json
   ```

   Corpo:

   ```json
   {
     "nome": "Fone de Ouvido Bluetooth",
     "preco": 249.9,
     "imagemUrl": "https://via.placeholder.com/400x260?text=Fone",
     "categoria": "Eletrônicos",
     "tags": "Bluetooth,Confortável",
     "descricao": "Fone leve, bateria longa.",
     "clienteVendedorId": 2
   }
   ```

3. **Dashboard / estatísticas de sentimentos**

   ```http
   GET {API_BASE_URL}/stats?vendedorId=2
   ```

   Resposta (exemplo):

   ```json
   {
     "totalComentarios": 120,
     "positivos": 80,
     "negativos": 30,
     "neutros": 10,
     "percentualPositivos": 0.67,
     "percentualNegativos": 0.25,
     "percentualNeutros": 0.08,
     "comentariosCriticos": 5
   }
   ```

4. **Lista de comentários do vendedor**

   ```http
   GET {API_BASE_URL}/comments?vendedorId=2
   ```

   Resposta (exemplo):

   ```json
   [
     {
       "comentarioId": 42,
       "textoOriginal": "Chegou atrasado e a embalagem veio rasgada.",
       "nota": 2,
       "dataCriacao": "2025-01-05T14:22:00",
       "produtoId": 10,
       "produtoNome": "Fone de Ouvido Bluetooth",
       "sentimento": "NEGATIVO",
       "probabilidade": 0.91,
       "ehCritico": true
     }
   ]
   ```

5. **Notificações do vendedor**

   ```http
   GET {API_BASE_URL}/notificacoes?vendedorId=2
   ```

   Resposta (exemplo):

   ```json
   [
     {
       "id": 1,
       "mensagem": "Comentário crítico detectado para o produto Fone de Ouvido Bluetooth.",
       "status": "PENDENTE",
       "canal": "DASHBOARD",
       "dataCriacao": "2025-01-05T14:23:00",
       "dataEnvio": null,
       "resultadoId": 1001
     }
   ]
   ```

6. **Marcar notificação como lida**

   ```http
   PATCH {API_BASE_URL}/notificacoes/1/ler
   ```

   Resposta:

   ```json
   {
     "id": 1,
     "status": "LIDA"
   }
   ```

7. **Exportar dados em JSON**

   ```http
   GET {API_BASE_URL}/export?vendedorId=2
   ```

   Resposta esperada:

    * JSON contendo lista de comentários + resultados + dados mínimos de produto.

   Exemplo simplificado:

   ```json
   {
     "vendedorId": 2,
     "comentarios": [
       {
         "comentarioId": 42,
         "produtoNome": "Fone de Ouvido Bluetooth",
         "textoOriginal": "Chegou atrasado e a embalagem veio rasgada.",
         "nota": 2,
         "sentimento": "NEGATIVO",
         "probabilidade": 0.91,
         "dataComentario": "2025-01-05T14:22:00"
       }
     ]
   }
   ```

---

## 7. Gestão de estado no frontend

### 7.1. `localStorage`

O frontend usa `localStorage` para guardar:

* `clienteAtual`:

    * `id` (do cliente, na tabela `cliente`);
    * `tipoCliente` (`CLIENTE_COMPRADOR` ou `CLIENTE_VENDEDOR`);
    * `nome`.

Exemplo:

```json
{
  "id": 2,
  "tipoCliente": "CLIENTE_VENDEDOR",
  "nome": "Loja XYZ"
}
```

Uso típico:

```js
const clienteAtualStr = localStorage.getItem("clienteAtual");
const clienteAtual = clienteAtualStr ? JSON.parse(clienteAtualStr) : null;
```

Recomendações:

* Se `clienteAtual` não existir, redirecionar para `login.html`.
* Ao fazer logout, fazer:

```js
localStorage.removeItem("clienteAtual");
window.location.href = "login.html";
```

---

## 8. Estilo visual (CSS) e experiência de uso

### 8.1. Arquivo `styles.css`

* Responsável pela definição de:

    * layout base (grid/flex);
    * cores padrão;
    * tipografia;
    * estilos de botões, cards, inputs.

Sugestões:

* Usar uma paleta clara, com contraste suficiente.
* Destacar:

    * notificações pendentes com cores mais fortes;
    * comentários negativos no dashboard com algum ícone ou cor de alerta (sem exagerar).

### 8.2. Acessibilidade básica

* Usar `<label>` para inputs.
* Garantir contraste mínimo nas fontes.
* Evitar depender apenas de cores para transmitir estado (ex.: usar ícones + texto).

---

## 9. Tratamento de erros e feedback ao usuário

O frontend deve sempre:

* Mostrar **carregando** enquanto espera respostas da API em operações mais demoradas (por exemplo, carregamento do dashboard).
* Tratar erros HTTP em chamadas `fetch`:

Exemplo genérico:

```js
async function chamarApi(url, options) {
  try {
    const response = await fetch(url, options);
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const msg = body.erro || `Erro ${response.status} ao chamar a API.`;
      throw new Error(msg);
    }
    return response.json();
  } catch (error) {
    alert(error.message || "Erro inesperado ao comunicar com a API.");
    console.error(error);
    throw error;
  }
}
```

Assim, as páginas (`comprador.js`, `vendedor.js`, `login.js`) podem reutilizar essa função.

---

## 10. Segurança no frontend

Alguns pontos importantes:

* **Nunca** colocar credenciais (usuário/senha de banco, tokens, chaves) no código frontend:

    * o código JS é visível para qualquer pessoa que abrir o site.
* As regras de negócio sensíveis (ex.: quando criar notificação, como calcular `ehCritico`) ficam no backend, não no JS.
* O frontend deve:

    * validar inputs para melhorar UX (ex.: tamanho mínimo de comentário),
    * mas a validação **real** e obrigatória é feita no backend.

---

## 11. Testes do frontend

### 11.1. Testes manuais mínimos

Alguns cenários recomendados (podem ser documentados como “Casos de Teste”):

1. **CT-FRONT-01 – Login como Comprador**

    * Dado: usuário acessa `login.html`.
    * Quando: seleciona “Cliente - Comprador” e clica em “Continuar”.
    * Então: é redirecionado para `comprador.html` e existe `clienteAtual` no `localStorage`.

2. **CT-FRONT-02 – Login como Vendedor**

    * Similar ao anterior, mas indo para `vendedor.html`.

3. **CT-FRONT-03 – Listar produtos na tela do comprador**

    * Dado: comprador em `comprador.html` com a API funcionando.
    * Quando: a página carrega.
    * Então: lista de produtos aparece (mesmo que vazia, mostrar mensagem adequada).

4. **CT-FRONT-04 – Enviar comentário de comprador**

    * Dado: comprador seleciona um produto e escreve um comentário válido.
    * Quando: clica em “Enviar”.
    * Então: recebe mensagem de sucesso.
    * E: o backend registra o comentário (verificado depois via dashboard).

5. **CT-FRONT-05 – Dashboard do vendedor carrega**

    * Dado: vendedor em `vendedor.html`.
    * Quando: abre a aba “Dashboard”.
    * Então: cards de stats aparecem com os números enviados pela API.

6. **CT-FRONT-06 – Marcar notificação como lida**

    * Dado: existe ao menos uma notificação “PENDENTE”.
    * Quando: o vendedor clica em “Marcar como lida”.
    * Então: o status muda visualmente para “LIDA”.

7. **CT-FRONT-07 – Exportar JSON**

    * Dado: vendedor em `vendedor.html`.
    * Quando: clica em “Exportar JSON”.
    * Então: navegador faz download de um arquivo `.json`.

### 11.2. Possível evolução: testes automatizados

Se o projeto for evoluir, podem ser incluídos:

* testes de integração de frontend com bibliotecas como Cypress ou Playwright;
* testes unitários simples para funções JS de chamada de API (por exemplo, usando Jest).

Para o hackathon / MVP, testes manuais bem descritos já ajudam bastante.

---

## 12. Como rodar o frontend em desenvolvimento

### 12.1. Opção 1 – Abrir direto no navegador

1. Certificar-se de que a API Java está rodando em `http://localhost:8080`.
2. Abrir o arquivo `frontend/web/login.html` diretamente no navegador (arrastar e soltar ou `Ctrl+O`).
3. Usar normalmente as telas.

> Dependendo da configuração de CORS e do navegador, chamadas `file://` podem dar restrições. Se isso acontecer, usar a opção 2.

### 12.2. Opção 2 – Servir com um servidor estático simples

Exemplo (usando Node e `npx serve`):

```bash
cd frontend/web
npx serve .
```

Isso deve subir algo como `http://localhost:3000`.

* Aí é só abrir `http://localhost:3000/login.html`.
* Ajustar `API_BASE_URL` se necessário.

---

## 13. Diagrama de navegação

O fluxo entre as telas está descrito também no diagrama:

* `docs/uml/07-diagrama-de-navegacao-de-telas.puml`

Resumo:

* `[Login]` → comprador → `comprador.html`
* `[Login]` → vendedor → `vendedor.html`
* Dentro de `comprador.html`:

    * lista de produtos → detalhe → enviar comentário.
* Dentro de `vendedor.html`:

    * “Meus Produtos” → cadastro de produto;
    * “Dashboard” → stats;
    * “Comentários” → lista detalhada;
    * “Notificações” → listar e marcar como lida;
    * “Exportar JSON” → aciona endpoint de exportação.

---

## 14. Conclusão

O frontend da **Hackathon One Sentiment API** foi pensado para ser:

* simples de rodar;
* fácil de integrar com a API;
* claro para o usuário final (comprador e vendedor);
* coerente com o modelo de domínio e com os diagramas do projeto.

Qualquer pessoa que chegue no time deve conseguir:

1. ler este `frontend.md`,
2. abrir `login.html`,
3. configurar `API_BASE_URL` em `config.js`,
4. e entender como o fluxo de telas conversa com o backend.
