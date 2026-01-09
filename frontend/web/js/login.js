const API_BASE_URL = "http://localhost:8080/api/v1";

const roleButtons = document.querySelectorAll(".role-btn");
const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const btnSkip = document.getElementById("btn-skip");

let tipoClienteSelecionado = "CLIENTE_COMPRADOR";

roleButtons.forEach((btn) => {
  btn.addEventListener("click", () => {
    roleButtons.forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    tipoClienteSelecionado = btn.dataset.role;
  });
});

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  loginError.style.display = "none";
  loginError.textContent = "";

  const nome = document.getElementById("nome").value.trim();
  const email = document.getElementById("email").value.trim();
  const senha = document.getElementById("senha").value.trim();

  if (!nome || !email || !senha) {
    loginError.textContent = "Preencha nome, e-mail e senha para prosseguir.";
    loginError.style.display = "block";
    return;
  }

  const payload = {
    nome: nome,
    email: email,
    tipoCliente: tipoClienteSelecionado
  };

  try {
    const resp = await fetch(API_BASE_URL + "/clientes", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      const msg = err.erro || err.message || "Erro ao cadastrar cliente.";
      throw new Error(msg);
    }

    const data = await resp.json();

    // Guardar cliente atual no localStorage para uso em comprador.html / vendedor.html
    localStorage.setItem("clienteAtual", JSON.stringify(data));

    if (data.tipoCliente === "CLIENTE_COMPRADOR") {
      window.location.href = "comprador.html";
    } else if (data.tipoCliente === "CLIENTE_VENDEDOR") {
      window.location.href = "vendedor.html";
    } else {
      loginError.textContent = "Tipo de cliente desconhecido retornado pelo servidor.";
      loginError.style.display = "block";
    }
  } catch (err) {
    loginError.textContent = err.message || "Erro inesperado ao comunicar com a API.";
    loginError.style.display = "block";
  }
});

// Modo demo: não chama backend, só marca no localStorage e redireciona
btnSkip.addEventListener("click", () => {
  const fakeCliente = {
    id: null,
    nome: "Demo",
    email: "demo@example.com",
    tipoCliente: tipoClienteSelecionado
  };
  localStorage.setItem("clienteAtual", JSON.stringify(fakeCliente));

  if (tipoClienteSelecionado === "CLIENTE_VENDEDOR") {
    window.location.href = "vendedor.html";
  } else {
    window.location.href = "comprador.html";
  }
});