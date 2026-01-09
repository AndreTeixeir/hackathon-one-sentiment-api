const API_BASE_URL = "http://localhost:8080/api/v1";

const vendedorInfo = document.getElementById("vendedor-info");
const produtoForm = document.getElementById("produto-form");
const produtoError = document.getElementById("produto-error");
const btnLimparProduto = document.getElementById("btn-limpar-produto");
const vendorProductsList = document.getElementById("vendor-products-list");

const btnAtualizarDash = document.getElementById("btn-atualizar-dashboard");
const btnExportarJson = document.getElementById("btn-exportar-json");
const exportJsonPreview = document.getElementById("export-json-preview");
const commentsTbody = document.getElementById("comments-tbody");
const notifList = document.getElementById("notif-list");

const statPosCount = document.getElementById("stat-pos-count");
const statNegCount = document.getElementById("stat-neg-count");
const statNeuCount = document.getElementById("stat-neu-count");
const statPosPercent = document.getElementById("stat-pos-percent");
const statNegPercent = document.getElementById("stat-neg-percent");
const statNeuPercent = document.getElementById("stat-neu-percent");

let clienteAtual = null;

function carregarVendedorAtual() {
  const raw = localStorage.getItem("clienteAtual");
  if (!raw) {
    vendedorInfo.innerHTML = 'Não autenticado · <a href="login.html">Fazer cadastro como Cliente - Vendedor</a>';
    return;
  }
  try {
    clienteAtual = JSON.parse(raw);
    if (clienteAtual.tipoCliente !== "CLIENTE_VENDEDOR") {
      vendedorInfo.innerHTML = `Logado como ${clienteAtual.tipoCliente || "Outro"} · <a href="login.html">Trocar cliente</a>`;
    } else {
      vendedorInfo.innerHTML = `Logado como Cliente - Vendedor: <strong>${clienteAtual.nome || "Sem nome"}</strong> · <a href="login.html">Trocar</a>`;
    }
  } catch {
    vendedorInfo.innerHTML = 'Erro ao ler cliente atual · <a href="login.html">Trocar cliente</a>';
  }
}

async function carregarProdutosDoVendedor() {
  vendorProductsList.innerHTML = "<div class='helper'>Carregando seus anúncios...</div>";
  if (!clienteAtual || !clienteAtual.id) {
    vendorProductsList.innerHTML = "<div class='helper'>Nenhum Cliente - Vendedor autenticado. Faça login em <a href='login.html'>login.html</a>.</div>";
    return;
  }

  try {
    const url = API_BASE_URL + "/produtos?clienteVendedorId=" + encodeURIComponent(clienteAtual.id);
    const resp = await fetch(url);
    if (!resp.ok) throw new Error("Erro ao buscar produtos do vendedor");
    const data = await resp.json();

    if (!Array.isArray(data) || data.length === 0) {
      vendorProductsList.innerHTML = "<div class='helper'>Você ainda não cadastrou nenhum anúncio.</div>";
      return;
    }

    vendorProductsList.innerHTML = "";
    data.forEach((p) => {
      const div = document.createElement("div");
      div.classList.add("vendor-product-item");

      const preco = typeof p.preco === "number" ? p.preco : 0;
      const categoria = p.categoria || "Produto";

      div.innerHTML = `
        <div class="vendor-product-main">
          <div class="vendor-product-name">${p.nome || "Produto sem nome"}</div>
          <div class="vendor-product-meta">
            R$ ${preco.toFixed(2).replace('.', ',')} · ${categoria}
          </div>
        </div>
        <div class="vendor-product-meta">
          ID: ${p.id ?? ""}
        </div>
      `;

      vendorProductsList.appendChild(div);
    });
  } catch (err) {
    vendorProductsList.innerHTML = "<div class='helper'>Erro ao carregar seus anúncios.</div>";
  }
}

produtoForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  produtoError.style.display = "none";
  produtoError.textContent = "";

  if (!clienteAtual || clienteAtual.tipoCliente !== "CLIENTE_VENDEDOR" || !clienteAtual.id) {
    produtoError.textContent = "É necessário estar autenticado como Cliente - Vendedor para cadastrar produtos.";
    produtoError.style.display = "block";
    return;
  }

  const nome = document.getElementById("nome-produto").value.trim();
  const precoStr = document.getElementById("preco-produto").value.trim();
  const imagemUrl = document.getElementById("imagem-url").value.trim();
  const categoria = document.getElementById("categoria-produto").value.trim();
  const tags = document.getElementById("tags-produto").value.trim();
  const descricao = document.getElementById("descricao-produto").value.trim();

  if (!nome) {
    produtoError.textContent = "Informe o nome do produto.";
    produtoError.style.display = "block";
    return;
  }
  if (!precoStr || isNaN(parseFloat(precoStr))) {
    produtoError.textContent = "Informe um preço válido.";
    produtoError.style.display = "block";
    return;
  }

  const preco = parseFloat(precoStr);

  const payload = {
    nome: nome,
    preco: preco,
    imagemUrl: imagemUrl || null,
    categoria: categoria || null,
    tags: tags || null,          // string com vírgulas
    descricao: descricao || null,
    clienteVendedorId: clienteAtual.id
  };

  try {
    const resp = await fetch(API_BASE_URL + "/produtos", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      const msg = err.erro || err.message || "Erro ao cadastrar produto.";
      throw new Error(msg);
    }

    await resp.json().catch(() => ({})); // não precisamos do retorno completo aqui
    produtoForm.reset();
    carregarProdutosDoVendedor();
  } catch (err) {
    produtoError.textContent = err.message || "Erro inesperado ao comunicar com a API ao cadastrar produto.";
    produtoError.style.display = "block";
  }
});

btnLimparProduto.addEventListener("click", () => {
  produtoForm.reset();
  produtoError.style.display = "none";
  produtoError.textContent = "";
});

// ---------- Dashboard (stats, comentários, notificações, export) ----------

btnAtualizarDash.addEventListener("click", () => {
  carregarDashboard();
});

async function carregarDashboard() {
  await Promise.all([
    carregarStats(),
    carregarComentarios(),
    carregarNotificacoes()
  ]);
}

async function carregarStats() {
  try {
    const resp = await fetch(API_BASE_URL + "/stats");
    if (!resp.ok) throw new Error("Erro ao buscar /stats");
    const data = await resp.json();

    const total = data.total || 0;
    const pos = data.positivos || 0;
    const neg = data.negativos || 0;
    const neu = data.neutros || 0;

    statPosCount.textContent = pos;
    statNegCount.textContent = neg;
    statNeuCount.textContent = neu;

    const pct = (v) => (total > 0 ? ((v / total) * 100).toFixed(1) + "%" : "–");

    statPosPercent.textContent = total > 0 ? pct(pos) + " do total" : "Sem dados";
    statNegPercent.textContent = total > 0 ? pct(neg) + " do total" : "Sem dados";
    statNeuPercent.textContent = total > 0 ? pct(neu) + " do total" : "Sem dados";
  } catch (err) {
    statPosCount.textContent = "–";
    statNegCount.textContent = "–";
    statNeuCount.textContent = "–";
    statPosPercent.textContent = "Erro ao carregar /stats";
    statNegPercent.textContent = "";
    statNeuPercent.textContent = "";
  }
}

async function carregarComentarios() {
  commentsTbody.innerHTML = "<tr><td colspan='7'>Carregando comentários...</td></tr>";

  try {
    const resp = await fetch(API_BASE_URL + "/comments");
    if (!resp.ok) throw new Error("Erro ao buscar /comments");
    const data = await resp.json();

    if (!Array.isArray(data) || data.length === 0) {
      commentsTbody.innerHTML = "<tr><td colspan='7'>Nenhum comentário encontrado.</td></tr>";
      return;
    }

    commentsTbody.innerHTML = "";
    data.forEach((c) => {
      const tr = document.createElement("tr");
      if (c.ehCritico) tr.classList.add("critico");

      const dt = c.dataCriacao || c.data_criacao;
      const dataStr = dt ? new Date(dt).toLocaleString("pt-BR") : "-";
      const nota = typeof c.nota === "number" ? c.nota : "-";
      const sentimento = (c.sentimento || "").toUpperCase();
      const prob = typeof c.probabilidade === "number"
        ? (c.probabilidade * (c.probabilidade <= 1 ? 100 : 1)).toFixed(1) + "%"
        : "-";

      let badgeClass = "neu";
      if (sentimento === "POSITIVO") badgeClass = "pos";
      else if (sentimento === "NEGATIVO") badgeClass = "neg";

      const criticoPill = c.ehCritico
        ? "<span class='pill-small critico'>Crítico</span>"
        : "<span class='pill-small'>Normal</span>";

      tr.innerHTML = `
        <td>${c.id ?? ""}</td>
        <td>${dataStr}</td>
        <td>${nota}</td>
        <td><span class="badge ${badgeClass}">${sentimento || "N/D"}</span></td>
        <td>${prob}</td>
        <td>${criticoPill}</td>
        <td>${(c.textoOriginal || c.texto || "").slice(0, 80)}${(c.textoOriginal || c.texto || "").length > 80 ? "..." : ""}</td>
      `;

      commentsTbody.appendChild(tr);
    });
  } catch (err) {
    commentsTbody.innerHTML = "<tr><td colspan='7'>Erro ao carregar comentários.</td></tr>";
  }
}

async function carregarNotificacoes() {
  notifList.innerHTML = "<div class='card-subtitle'>Carregando notificações...</div>";

  try {
    const resp = await fetch(API_BASE_URL + "/notifications");
    if (!resp.ok) throw new Error("Erro ao buscar /notifications");
    const data = await resp.json();

    if (!Array.isArray(data) || data.length === 0) {
      notifList.innerHTML = "<div class='card-subtitle'>Nenhuma notificação pendente.</div>";
      return;
    }

    notifList.innerHTML = "";
    data.forEach((n) => {
      const div = document.createElement("div");
      div.classList.add("notif-item");

      const dt = n.dataCriacao || n.data_criacao;
      const dataStr = dt ? new Date(dt).toLocaleString("pt-BR") : "-";

      const status = (n.status || "").toUpperCase();
      const canal = (n.canal || "").toUpperCase();
      const mensagem = n.mensagem || "";

      const resultado = n.resultado || {};
      const sent = (resultado.sentimento || "").toUpperCase();
      const comentarioId = resultado.comentarioId || resultado.comentario_id || "";

      div.innerHTML = `
        <div class="notif-header">
          <div>${mensagem}</div>
          <div class="notif-status ${status}">${status}</div>
        </div>
        <div class="notif-time">
          ${dataStr} · Canal: ${canal}
          ${comentarioId ? " · Comentário #" + comentarioId : ""}
          ${sent ? " · Sentimento: " + sent : ""}
        </div>
      `;

      notifList.appendChild(div);
    });
  } catch (err) {
    notifList.innerHTML = "<div class='card-subtitle'>Erro ao carregar notificações.</div>";
  }
}

btnExportarJson.addEventListener("click", async () => {
  exportJsonPreview.textContent = "Carregando /export...";
  try {
    const resp = await fetch(API_BASE_URL + "/export");
    if (!resp.ok) throw new Error("Erro ao chamar /export");
    const data = await resp.json();
    exportJsonPreview.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    exportJsonPreview.textContent = "Erro ao carregar /export: " + err.message;
  }
});

// Inicialização
carregarVendedorAtual();
carregarProdutosDoVendedor();
carregarDashboard();