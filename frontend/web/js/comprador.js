const API_BASE_URL = "http://localhost:8080/api/v1";

const clienteInfo = document.getElementById("cliente-info");
const productsGrid = document.getElementById("products-grid");
const detailEmpty = document.getElementById("product-detail-empty");
const detailSection = document.getElementById("product-detail");
const detailSubtitle = document.getElementById("detail-subtitle");
const detailImage = document.getElementById("detail-image");
const detailName = document.getElementById("detail-name");
const detailPrice = document.getElementById("detail-price");
const detailMeta = document.getElementById("detail-meta");
const detailTags = document.getElementById("detail-tags");
const detailDescription = document.getElementById("detail-description");
const btnComprar = document.getElementById("btn-comprar");
const avaliacaoForm = document.getElementById("avaliacao-form");
const avaliacaoError = document.getElementById("avaliacao-error");
const starsContainer = document.getElementById("stars-container");
const resultadoSentimento = document.getElementById("resultado-sentimento");

let clienteAtual = null;
let produtoSelecionado = null;
let notaSelecionada = 0;

function carregarClienteAtual() {
  const raw = localStorage.getItem("clienteAtual");
  if (!raw) {
    clienteInfo.innerHTML = 'Não autenticado · <a href="login.html">Fazer cadastro</a>';
    return;
  }
  try {
    clienteAtual = JSON.parse(raw);
    if (clienteAtual.tipoCliente !== "CLIENTE_COMPRADOR") {
      clienteInfo.innerHTML = `Logado como ${clienteAtual.tipoCliente || "Outro"} · <a href="login.html">Trocar cliente</a>`;
    } else {
      clienteInfo.innerHTML = `Logado como Cliente - Comprador: <strong>${clienteAtual.nome || "Sem nome"}</strong>`;
    }
  } catch {
    clienteInfo.innerHTML = 'Erro ao ler cliente atual · <a href="login.html">Fazer cadastro</a>';
  }
}

async function carregarProdutos() {
  productsGrid.innerHTML = "<div class='helper'>Carregando anúncios...</div>";
  try {
    const resp = await fetch(API_BASE_URL + "/produtos");
    if (!resp.ok) throw new Error("Erro ao buscar /produtos");
    const data = await resp.json();

    if (!Array.isArray(data) || data.length === 0) {
      productsGrid.innerHTML = "<div class='helper'>Nenhum anúncio cadastrado por Clientes - Vendedores.</div>";
      return;
    }

    productsGrid.innerHTML = "";
    data.forEach((p) => {
      const card = document.createElement("div");
      card.classList.add("product-card");
      card.dataset.id = p.id;

      const imagemUrl = p.imagemUrl || p.imagem_url || "https://via.placeholder.com/400x260?text=Produto";
      const categoria = p.categoria || "Produto";
      const preco = typeof p.preco === "number" ? p.preco : 0;

      card.innerHTML = `
        <img src="${imagemUrl}" alt="${p.nome || ""}">
        <div class="product-overlay">
          <div>
            <div class="product-name">${p.nome || "Produto sem nome"}</div>
            <div class="product-price">R$ ${preco.toFixed(2).replace('.', ',')}</div>
            <div class="product-tags">${categoria}</div>
          </div>
        </div>
      `;

      card.addEventListener("click", () => {
        selecionarProduto(p);
      });

      productsGrid.appendChild(card);
    });
  } catch (err) {
    productsGrid.innerHTML = "<div class='helper'>Erro ao carregar anúncios.</div>";
  }
}

function selecionarProduto(produto) {
  produtoSelecionado = produto;
  detailEmpty.classList.add("hidden");
  detailSection.classList.remove("hidden");
  detailSubtitle.textContent = "Avalie o produto e veja a análise de sentimento da sua opinião.";

  const imagemUrl = produto.imagemUrl || produto.imagem_url || "https://via.placeholder.com/400x260?text=Produto";
  const preco = typeof produto.preco === "number" ? produto.preco : 0;
  const categoria = produto.categoria || "Produto";
  const tagsStr = produto.tags || produto.tagsCsv || "";

  detailImage.src = imagemUrl;
  detailImage.alt = produto.nome || "";
  detailName.textContent = produto.nome || "Produto sem nome";
  detailPrice.textContent = "R$ " + preco.toFixed(2).replace('.', ',');
  detailMeta.textContent = categoria;
  detailTags.textContent = tagsStr ? tagsStr.split(",").map(t => t.trim()).join(" · ") : "";
  detailDescription.textContent = produto.descricao || produto.descricaoCompleta || "Sem descrição detalhada.";

  resultadoSentimento.innerHTML = "";
  avaliacaoError.style.display = "none";
  avaliacaoError.textContent = "";
  document.getElementById("texto-avaliacao").value = "";
  notaSelecionada = 0;
  atualizarEstrelas();
}

function setupEstrelas() {
  starsContainer.innerHTML = "";
  for (let i = 1; i <= 5; i++) {
    const span = document.createElement("span");
    span.classList.add("star");
    span.textContent = "★";
    span.dataset.value = i;
    span.addEventListener("click", () => {
      notaSelecionada = i;
      atualizarEstrelas();
    });
    starsContainer.appendChild(span);
  }
  atualizarEstrelas();
}

function atualizarEstrelas() {
  const stars = starsContainer.querySelectorAll(".star");
  stars.forEach((star) => {
    const val = parseInt(star.dataset.value, 10);
    if (val <= notaSelecionada) star.classList.add("active");
    else star.classList.remove("active");
  });
}

avaliacaoForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  avaliacaoError.style.display = "none";
  avaliacaoError.textContent = "";
  resultadoSentimento.innerHTML = "";

  if (!produtoSelecionado) {
    avaliacaoError.textContent = "Selecione um anúncio antes de enviar a avaliação.";
    avaliacaoError.style.display = "block";
    return;
  }

  if (!notaSelecionada || notaSelecionada < 1 || notaSelecionada > 5) {
    avaliacaoError.textContent = "Escolha uma nota de 1 a 5 nas estrelas.";
    avaliacaoError.style.display = "block";
    return;
  }

  const texto = document.getElementById("texto-avaliacao").value.trim();
  if (!texto || texto.length < 3) {
    avaliacaoError.textContent = "O campo de comentário é obrigatório e deve ter pelo menos 3 caracteres.";
    avaliacaoError.style.display = "block";
    return;
  }

  const payload = {
    texto: texto,
    nota: notaSelecionada,
    origem: "SITE",
    idioma: "pt-BR",
    produtoId: produtoSelecionado.id,
    clienteCompradorId: clienteAtual && clienteAtual.id ? clienteAtual.id : null
  };

  try {
    const resp = await fetch(API_BASE_URL + "/sentiment", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      const msg = err.erro || err.message || "Erro ao processar a avaliação.";
      throw new Error(msg);
    }

    const data = await resp.json();
    renderResultadoSentimento(data);
  } catch (err) {
    avaliacaoError.textContent = err.message || "Erro inesperado ao comunicar com a API.";
    avaliacaoError.style.display = "block";
  }
});

function renderResultadoSentimento(data) {
  const previsao = (data.previsao || data.label || "").toUpperCase();
  const prob = typeof data.probabilidade !== "undefined"
    ? data.probabilidade
    : data.probability;

  let cssClass = "";
  if (previsao === "POSITIVO") cssClass = "sent-pos";
  else if (previsao === "NEGATIVO") cssClass = "sent-neg";

  const probStr = typeof prob === "number"
    ? (prob * (prob <= 1 ? 100 : 1)).toFixed(1) + "%"
    : "-";

  resultadoSentimento.innerHTML = `
    <div class="result-pill ${cssClass}">
      <div>
        <div style="font-size: 0.8rem; color: var(--muted);">Resultado da análise de sentimento</div>
        <div style="margin-top: 2px;">
          <span class="badge ${previsao === "POSITIVO" ? "pos" : previsao === "NEGATIVO" ? "neg" : "neu"}">
            ${previsao || "N/D"}
          </span>
          <span style="font-size: 0.8rem; margin-left: 8px;">
            Probabilidade estimada: <strong>${probStr}</strong>
          </span>
        </div>
      </div>
      <div style="font-size: 0.7rem; color: var(--muted); text-align: right;">
        Resposta bruta da API:
        <pre class="json-output" style="margin-top: 4px; max-width: 260px;">${JSON.stringify(data, null, 2)}</pre>
      </div>
    </div>
  `;
}

btnComprar.addEventListener("click", () => {
  if (!produtoSelecionado) return;
  alert('Simulação: compra do produto "' + (produtoSelecionado.nome || "Produto") + '" iniciada.');
});

// Inicialização
carregarClienteAtual();
carregarProdutos();
setupEstrelas();