// =====================================================================
// pc-api.js
// Helpers compartilhados pelo painel web do Parada Certa.
// - Base URL da API Java
// - Sessao do administrador (persistida em localStorage com a chave 'adminLogado')
// - Wrapper de fetch que extrai mensagens de erro padrao da API
// =====================================================================

const PC_API_BASE = (
  window.PC_API_BASE ||
  (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : "")
);
const PC_SESSION_KEY = "adminLogado";

function pcGetSession() {
  try {
    const raw = localStorage.getItem(PC_SESSION_KEY);
    if (!raw) return null;
    const obj = JSON.parse(raw);
    if (!obj || !obj.id || !obj.estacionamentoId) return null;
    return obj;
  } catch (_) {
    return null;
  }
}

function pcSetSession(login) {
  if (!login || !login.id || !login.estacionamentoId) {
    localStorage.removeItem(PC_SESSION_KEY);
    return;
  }
  const sess = {
    id: login.id,
    usuario: login.usuario,
    nomeCompleto: login.nomeCompleto,
    email: login.email,
    telefone: login.telefone,
    estacionamentoId: login.estacionamentoId,
    estacionamentoNome: login.estacionamentoNome,
    logadoEm: new Date().toISOString()
  };
  localStorage.setItem(PC_SESSION_KEY, JSON.stringify(sess));
}

function pcClearSession() {
  localStorage.removeItem(PC_SESSION_KEY);
  localStorage.removeItem("planoTipoAtual");
  localStorage.removeItem("planoAtivo");
}

/**
 * Redireciona para o login caso nao haja sessao valida.
 * Use no topo das paginas autenticadas.
 */
function pcRequireSession() {
  const sess = pcGetSession();
  if (!sess) {
    window.location.href = "login-admin.html";
    throw new Error("Sessao ausente");
  }
  return sess;
}

/**
 * Realiza fetch contra a API e trata erros padrao { sucesso, mensagem } do GlobalExceptionHandler.
 * Retorna o JSON da resposta (ou null em 204). Em erro, lanca Error(mensagem).
 */
async function pcFetch(path, options = {}) {
  const opts = Object.assign({}, options);
  opts.headers = Object.assign(
    { "Accept": "application/json" },
    options.headers || {}
  );
  if (opts.body && !(opts.body instanceof FormData) && !opts.headers["Content-Type"]) {
    opts.headers["Content-Type"] = "application/json";
  }

  const url = path.startsWith("http") ? path : PC_API_BASE + path;
  let resp;
  try {
    resp = await fetch(url, opts);
  } catch (e) {
    throw new Error("Sem conexao com a API (" + PC_API_BASE + "). Verifique se o backend Java esta rodando.");
  }

  if (resp.status === 204) return null;

  let body = null;
  const ctype = resp.headers.get("content-type") || "";
  if (ctype.includes("application/json")) {
    try { body = await resp.json(); } catch (_) { body = null; }
  } else {
    try { body = await resp.text(); } catch (_) { body = null; }
  }

  if (!resp.ok) {
    let msg = "Erro " + resp.status;
    if (body && typeof body === "object" && body.mensagem) {
      msg = body.mensagem;
    } else if (typeof body === "string" && body.length) {
      msg = body;
    }
    const err = new Error(msg);
    err.status = resp.status;
    err.body = body;
    throw err;
  }

  return body;
}

function pcFormatarMoeda(valor) {
  const numero = Number(valor || 0);
  return numero.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function pcFormatarHora(valor) {
  if (!valor) return "";
  return String(valor).substring(0, 5);
}

function pcFormatarDataHora(valor) {
  if (!valor) return "—";
  try {
    const d = new Date(valor);
    if (isNaN(d.getTime())) return String(valor);
    return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
  } catch (_) {
    return String(valor);
  }
}

function pcFormatarData(valor) {
  if (!valor) return "—";
  try {
    const d = new Date(valor);
    if (isNaN(d.getTime())) return String(valor);
    return d.toLocaleDateString("pt-BR");
  } catch (_) {
    return String(valor);
  }
}

function pcAtivoValor(valor) {
  return valor === true || valor === 1 || valor === "true";
}

// =====================================================================
// Plano vigente do estacionamento
// Cache em memoria por estacionamentoId para evitar refetch a cada navegacao.
// O cache e invalidado ao chamar pcInvalidarPlano() ou ao sair (pcSair).
// =====================================================================
const _PC_PLANO_CACHE = new Map();

async function pcCarregarPlano(estacionamentoId) {
  if (!estacionamentoId) return null;
  if (_PC_PLANO_CACHE.has(estacionamentoId)) {
    return _PC_PLANO_CACHE.get(estacionamentoId);
  }
  try {
    const plano = await pcFetch("/api/estacionamentos/" + estacionamentoId + "/plano");
    if (plano && plano.plano) {
      localStorage.setItem("planoTipoAtual", String(plano.plano).toUpperCase());
      localStorage.setItem("planoAtivo", plano.ativo ? "true" : "false");
    }
    _PC_PLANO_CACHE.set(estacionamentoId, plano);
    return plano;
  } catch (e) {
    console.warn("pcCarregarPlano falhou:", e.message);
    return null;
  }
}

function pcInvalidarPlano(estacionamentoId) {
  if (estacionamentoId == null) {
    _PC_PLANO_CACHE.clear();
    localStorage.removeItem("planoTipoAtual");
  } else {
    _PC_PLANO_CACHE.delete(estacionamentoId);
  }
}

function pcSair() {
  pcClearSession();
  pcInvalidarPlano();
  window.location.href = "escolha-login.html";
}

function pcToggleMobileNav() {
  const menu = document.getElementById("pc-mobile-menu");
  const botao = document.getElementById("pc-mobile-menu-button");
  if (!menu || !botao) return;
  const abrir = menu.classList.contains("hidden");
  menu.classList.toggle("hidden", !abrir);
  botao.setAttribute("aria-expanded", abrir ? "true" : "false");
}

function pcCloseMobileNav() {
  const menu = document.getElementById("pc-mobile-menu");
  const botao = document.getElementById("pc-mobile-menu-button");
  if (!menu || !botao) return;
  menu.classList.add("hidden");
  botao.setAttribute("aria-expanded", "false");
}

// =====================================================================
// Navbar unica do painel admin
// Garante que toda pagina autenticada exibe o mesmo conjunto de abas,
// evitando o bug em que itens do menu "sumiam" ao navegar entre paginas.
// Uso: coloque <nav id="pc-navbar" data-page="dashboard"></nav> no header.
// =====================================================================
const PC_NAV_ITEMS = [
  { id: "dashboard",     label: "Dashboard",       href: "admin-dashboard.html"    },
  { id: "estacionamentos", label: "Estacionamentos", href: "admin-vagas.html"      },
  { id: "reservas",      label: "Painel de Operação", href: "reserva.html"         },
  { id: "financeiro",    label: "Financeiro",      href: "pagamento.html"          },
  { id: "avaliacoes",    label: "Avaliações",      href: "avaliacoes.html"         },
  { id: "precos",        label: "Preços",          href: "admin-precos.html"       },
  { id: "relatorios",    label: "Relatórios",      href: "admin-relatorios.html"   },
  { id: "usuarios",      label: "Usuários",        href: "usuarios.html"           },
  { id: "configuracoes", label: "Configurações",   href: "admin-configuracoes.html"}
];

/**
 * Renderiza o menu administrativo dentro do <nav id="pc-navbar">.
 * O atributo data-page indica qual aba esta ativa (recebe destaque verde).
 * Tambem injeta o botao Premium e o botao Sair.
 */
function pcRenderNavbar() {
  const nav = document.getElementById("pc-navbar");
  if (!nav) return;

  const pageAtiva = (nav.dataset.page || "").toLowerCase();
  const planoTipoAtual = (localStorage.getItem("planoTipoAtual") || "").toUpperCase();
  const planoAtivo = localStorage.getItem("planoAtivo") === "true";

  const linksHtml = PC_NAV_ITEMS.map(item => {
    const ativo = item.id === pageAtiva;
    const cls = ativo
      ? "text-green-600 font-bold"
      : "text-slate-700 hover:text-green-600 font-medium";
    return `<a href="${item.href}" class="${cls}">${item.label}</a>`;
  }).join("");

  const mobileLinksHtml = PC_NAV_ITEMS.map(item => {
    const ativo = item.id === pageAtiva;
    const cls = ativo
      ? "block px-4 py-3 text-green-700 bg-green-50 font-bold"
      : "block px-4 py-3 text-slate-700 hover:bg-slate-50 hover:text-green-700 font-medium";
    return `<a href="${item.href}" class="${cls}" onclick="pcCloseMobileNav()">${item.label}</a>`;
  }).join("");

  const premiumLabel = planoTipoAtual === "PREMIUM" && planoAtivo
    ? "Premium ativo"
    : (planoTipoAtual === "STANDARD" && planoAtivo ? "Plano Standard" : "Planos");
  const premiumCls = planoTipoAtual === "PREMIUM" && planoAtivo
    ? "bg-yellow-100 text-yellow-700 border border-yellow-200 font-bold px-4 py-2 rounded-xl"
    : (planoTipoAtual === "STANDARD" && planoAtivo
      ? "bg-green-100 text-green-700 border border-green-200 font-bold px-4 py-2 rounded-xl"
      : "bg-green-600 hover:bg-green-700 text-white font-bold px-4 py-2 rounded-xl transition");

  nav.className = "relative flex justify-end md:justify-start text-sm items-center";
  nav.innerHTML = `
    <div class="hidden md:flex flex-wrap gap-4 text-sm items-center">
      ${linksHtml}
      <a id="botaoPremiumMenu" href="plano-premium.html" class="${premiumCls}">${premiumLabel}</a>
      <button onclick="pcSair()" class="bg-red-500 hover:bg-red-600 text-white text-sm font-semibold px-4 py-2 rounded-xl transition">Sair</button>
    </div>

    <button
      id="pc-mobile-menu-button"
      type="button"
      class="md:hidden inline-flex h-11 w-11 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-700 shadow-sm"
      aria-label="Abrir menu"
      aria-expanded="false"
      onclick="pcToggleMobileNav()"
    >
      <span class="flex flex-col gap-1.5" aria-hidden="true">
        <span class="block h-0.5 w-5 rounded bg-current"></span>
        <span class="block h-0.5 w-5 rounded bg-current"></span>
        <span class="block h-0.5 w-5 rounded bg-current"></span>
      </span>
    </button>

    <div id="pc-mobile-menu" class="hidden md:hidden absolute right-0 top-12 z-50 w-72 max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-slate-200 bg-white shadow-xl">
      ${mobileLinksHtml}
      <div class="border-t border-slate-100 p-3 space-y-2">
        <a id="botaoPremiumMenuMobile" href="plano-premium.html" onclick="pcCloseMobileNav()" class="block text-center ${premiumCls}">${premiumLabel}</a>
        <button onclick="pcSair()" class="w-full bg-red-500 hover:bg-red-600 text-white text-sm font-semibold px-4 py-2 rounded-lg transition">Sair</button>
      </div>
    </div>
  `;
}

document.addEventListener("click", function(event) {
  const nav = document.getElementById("pc-navbar");
  const menu = document.getElementById("pc-mobile-menu");
  if (!nav || !menu || menu.classList.contains("hidden")) return;
  if (!nav.contains(event.target)) {
    pcCloseMobileNav();
  }
});

// Renderiza automaticamente quando o DOM esta pronto.
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", pcRenderNavbar);
} else {
  pcRenderNavbar();
}

// =====================================================================
// Toasts / mensagens visuais (substituem alert() nas telas principais)
// =====================================================================

function _pcGarantirToastRoot() {
  let root = document.getElementById("pc-toast-root");
  if (root) return root;
  root = document.createElement("div");
  root.id = "pc-toast-root";
  root.style.cssText = "position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:10px;max-width:360px;pointer-events:none;";
  document.body.appendChild(root);
  return root;
}

/**
 * Exibe um toast no canto superior direito.
 * tipo: "sucesso" | "erro" | "info" | "aviso"
 * mensagem: string a exibir.
 * duracaoMs: tempo até desaparecer; 0 = manter até o usuário fechar.
 */
function pcToast(tipo, mensagem, duracaoMs) {
  const root = _pcGarantirToastRoot();
  const cores = {
    sucesso: { bg: "#dcfce7", border: "#16a34a", text: "#166534" },
    erro:    { bg: "#fee2e2", border: "#dc2626", text: "#7f1d1d" },
    info:    { bg: "#dbeafe", border: "#2563eb", text: "#1e3a8a" },
    aviso:   { bg: "#fef9c3", border: "#ca8a04", text: "#713f12" }
  };
  const c = cores[tipo] || cores.info;
  const toast = document.createElement("div");
  toast.style.cssText =
    "pointer-events:auto;background:" + c.bg + ";border:1px solid " + c.border +
    ";color:" + c.text + ";padding:12px 14px;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,.08);" +
    "font:600 14px/1.35 system-ui,Arial,sans-serif;display:flex;align-items:flex-start;gap:8px;";
  const span = document.createElement("span");
  span.textContent = String(mensagem || "");
  span.style.cssText = "flex:1;white-space:pre-wrap;";
  const fechar = document.createElement("button");
  fechar.type = "button";
  fechar.innerText = "×";
  fechar.style.cssText = "background:transparent;border:0;color:" + c.text + ";font-size:18px;line-height:1;cursor:pointer;font-weight:700;";
  fechar.onclick = () => { try { root.removeChild(toast); } catch(_) {} };
  toast.appendChild(span);
  toast.appendChild(fechar);
  root.appendChild(toast);
  const ms = duracaoMs == null ? (tipo === "erro" ? 6000 : 3500) : duracaoMs;
  if (ms > 0) {
    setTimeout(() => { try { root.removeChild(toast); } catch(_) {} }, ms);
  }
  return toast;
}

/** Helpers curtos. */
function pcSucesso(msg, ms) { return pcToast("sucesso", msg, ms); }
function pcErro(msg, ms)    { return pcToast("erro", msg, ms); }
function pcInfo(msg, ms)    { return pcToast("info", msg, ms); }
function pcAviso(msg, ms)   { return pcToast("aviso", msg, ms); }

/**
 * Painel fixo no topo do formulário (mensagem inline persistente).
 * elementoOuId: id do elemento a popular (deve existir no HTML) ou o próprio nó.
 * tipo: "sucesso" | "erro" | "info" | "aviso" | "limpar".
 */
function pcMensagemInline(elementoOuId, tipo, mensagem) {
  const el = typeof elementoOuId === "string" ? document.getElementById(elementoOuId) : elementoOuId;
  if (!el) return;
  if (tipo === "limpar" || !mensagem) {
    el.className = "hidden";
    el.innerText = "";
    return;
  }
  const mapa = {
    sucesso: "mb-6 bg-green-50 border border-green-200 text-green-700 rounded-2xl p-4 text-sm",
    erro:    "mb-6 bg-red-50 border border-red-200 text-red-700 rounded-2xl p-4 text-sm",
    info:    "mb-6 bg-blue-50 border border-blue-200 text-blue-700 rounded-2xl p-4 text-sm",
    aviso:   "mb-6 bg-yellow-50 border border-yellow-200 text-yellow-800 rounded-2xl p-4 text-sm"
  };
  el.className = mapa[tipo] || mapa.info;
  el.innerText = String(mensagem || "");
}

/**
 * Monta o endereço textual no padrão do projeto:
 *   "logradouro, numero - bairro, cidade - UF"
 * Campos ausentes são omitidos com bom-senso (sem deixar separadores soltos).
 */
function pcMontarEndereco(p) {
  const logradouro = (p && p.logradouro || "").trim();
  const numero     = (p && p.numero || "").trim();
  const bairro     = (p && p.bairro || "").trim();
  const cidade     = (p && p.cidade || "").trim();
  const uf         = (p && p.uf || "").trim().toUpperCase();

  let inicio = logradouro;
  if (numero) inicio += (inicio ? ", " : "") + numero;

  let meio = bairro;

  let fim = cidade;
  if (uf) fim += (fim ? " - " : "") + uf;

  let result = inicio;
  if (meio) result += (result ? " - " : "") + meio;
  if (fim)  result += (result ? ", " : "") + fim;
  return result;
}
