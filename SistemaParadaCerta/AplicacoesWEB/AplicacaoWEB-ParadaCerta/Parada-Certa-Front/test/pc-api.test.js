const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

function createStorage() {
  const data = new Map();
  return {
    getItem: (key) => data.has(key) ? data.get(key) : null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key),
    clear: () => data.clear()
  };
}

function loadPcApi(overrides = {}) {
  const storage = createStorage();
  const documentListeners = {};
  const context = {
    console: { warn() {} },
    fetch: overrides.fetch || (() => Promise.reject(new Error("fetch nao configurado"))),
    FormData: class FormData {},
    localStorage: storage,
    setTimeout: () => 1,
    window: { location: { href: "" } },
    document: {
      readyState: "loading",
      body: { appendChild() {} },
      addEventListener(event, handler) {
        documentListeners[event] = handler;
      },
      getElementById: () => null,
      createElement: () => ({
        style: {},
        appendChild() {},
        set textContent(value) { this._textContent = value; },
        set innerText(value) { this._innerText = value; }
      })
    }
  };

  vm.createContext(context);
  const source = fs.readFileSync(path.join(__dirname, "..", "pc-api.js"), "utf8");
  vm.runInContext(source, context);
  return { context, storage, documentListeners };
}

test("pcSetSession persiste apenas sessoes administrativas validas", () => {
  const { context, storage } = loadPcApi();

  context.pcSetSession({ id: 7, usuario: "admin", estacionamentoId: 3, estacionamentoNome: "Centro" });
  const sessao = JSON.parse(storage.getItem("adminLogado"));

  assert.equal(sessao.id, 7);
  assert.equal(sessao.estacionamentoId, 3);
  assert.equal(sessao.estacionamentoNome, "Centro");
  assert.match(sessao.logadoEm, /^\d{4}-\d{2}-\d{2}T/);

  context.pcSetSession({ id: 7 });
  assert.equal(storage.getItem("adminLogado"), null);
});

test("pcGetSession ignora JSON invalido e dados incompletos", () => {
  const { context, storage } = loadPcApi();

  storage.setItem("adminLogado", "{");
  assert.equal(context.pcGetSession(), null);

  storage.setItem("adminLogado", JSON.stringify({ id: 1 }));
  assert.equal(context.pcGetSession(), null);
});

test("pcFetch adiciona base URL, content-type e retorna JSON", async () => {
  let capturedUrl;
  let capturedOptions;
  const { context } = loadPcApi({
    fetch: async (url, options) => {
      capturedUrl = url;
      capturedOptions = options;
      return {
        ok: true,
        status: 200,
        headers: { get: () => "application/json" },
        json: async () => ({ sucesso: true }),
        text: async () => ""
      };
    }
  });

  const body = await context.pcFetch("/api/teste", { method: "POST", body: JSON.stringify({ ok: true }) });

  assert.equal(capturedUrl, "http://localhost:8080/api/teste");
  assert.equal(capturedOptions.headers.Accept, "application/json");
  assert.equal(capturedOptions.headers["Content-Type"], "application/json");
  assert.deepEqual(body, { sucesso: true });
});

test("pcFetch usa mensagem padrao da API ao rejeitar resposta", async () => {
  const { context } = loadPcApi({
    fetch: async () => ({
      ok: false,
      status: 400,
      headers: { get: () => "application/json" },
      json: async () => ({ sucesso: false, mensagem: "Dados invalidos" }),
      text: async () => ""
    })
  });

  await assert.rejects(
    () => context.pcFetch("/api/teste"),
    (err) => err.message === "Dados invalidos" && err.status === 400
  );
});

test("helpers de formatacao tratam valores ausentes e enderecos parciais", () => {
  const { context } = loadPcApi();

  assert.equal(context.pcFormatarMoeda(12.5).replace(/\s/u, " "), "R$ 12,50");
  assert.equal(context.pcFormatarHora("08:30:00"), "08:30");
  assert.equal(context.pcFormatarDataHora(null), "\u2014");
  assert.equal(context.pcAtivoValor("true"), true);
  assert.equal(context.pcAtivoValor("false"), false);
  assert.equal(
    context.pcMontarEndereco({
      logradouro: "Av Paulista",
      numero: "1000",
      bairro: "Bela Vista",
      cidade: "Sao Paulo",
      uf: "sp"
    }),
    "Av Paulista, 1000 - Bela Vista, Sao Paulo - SP"
  );
});
