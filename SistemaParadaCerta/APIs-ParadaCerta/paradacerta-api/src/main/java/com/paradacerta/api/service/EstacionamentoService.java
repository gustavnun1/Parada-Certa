package com.paradacerta.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstacionamentoService {

    private final EstacionamentoRepository estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final ObjectMapper objectMapper;

    public List<Estacionamento> buscarTodos() {
        List<Estacionamento> lista = estacionamentoRepository.findByAtivoTrue();
        popularVagas(lista);
        return lista;
    }

    /** Lista apenas os estacionamentos vinculados ao administrador informado. */
    public List<Estacionamento> buscarPorAdmin(Integer admId) {
        List<Estacionamento> lista = estacionamentoRepository.findEstacionamentosDoAdmin(admId);
        popularVagas(lista);
        return lista;
    }

    public List<Estacionamento> buscarProximos(Double latitude, Double longitude, Double raioKm) {
        List<Estacionamento> lista = estacionamentoRepository.findEstacionamentosProximos(latitude, longitude, raioKm);
        popularVagas(lista);
        return lista;
    }

    public Optional<Estacionamento> buscarPorId(Integer id) {
        Optional<Estacionamento> opt = estacionamentoRepository.findById(id);
        opt.ifPresent(e -> popularVagas(List.of(e)));
        return opt;
    }

    @Transactional
    public void decrementarVaga(Integer id) {
        vagasRepository.decrementarVaga(id);
    }

    @Transactional
    public void incrementarVaga(Integer id) {
        vagasRepository.incrementarVaga(id);
    }

    // ── CRUD admin web ───────────────────────────────────────────────────────

    @Transactional
    public Estacionamento criar(EstacionamentoRequest req) {
        Integer reservaveis = req.getQtdVagasReservaveis() == null ? 0 : req.getQtdVagasReservaveis();
        boolean permiteReserva = Boolean.TRUE.equals(req.getPermiteReserva());
        if (!permiteReserva) reservaveis = 0;
        if (permiteReserva && reservaveis <= 0) {
            throw new RequisicaoInvalidaException("Informe quantas vagas serao reservaveis");
        }
        if (reservaveis > req.getQtdVagasTotais()) {
            throw new RequisicaoInvalidaException("Vagas reserváveis não pode ser maior que o total de vagas");
        }

        Estacionamento est = new Estacionamento();
        String cnpj = DocumentoValidator.somenteDigitos(req.getCnpj());
        DocumentoValidator.validarCnpj(cnpj);
        if (estacionamentoRepository.existsByCnpj(cnpj)) {
            throw new ConflictException("Ja existe um estacionamento cadastrado com este CNPJ");
        }

        est.setCnpj(cnpj);
        est.setRazaoSocial(req.getRazaoSocial().trim());
        est.setNomeFantasia(trimOuNull(req.getNomeFantasia()));
        est.setNome(nomeExibicao(req.getNome(), req.getNomeFantasia(), req.getRazaoSocial()));

        // Endereço detalhado (preenche colunas novas e deriva o texto livre quando aplicável)
        est.setCep(normalizarCep(req.getCep()));
        est.setLogradouro(trimOuNull(req.getLogradouro()));
        est.setNumero(trimOuNull(req.getNumero()));
        est.setComplemento(trimOuNull(req.getComplemento()));
        est.setBairro(trimOuNull(req.getBairro()));
        est.setCidade(trimOuNull(req.getCidade()));
        est.setUf(req.getUf() == null ? null : req.getUf().trim().toUpperCase());
        validarCidadeSaoPaulo(est.getCidade());
        String enderecoFinal = resolverEnderecoTextual(req.getEndereco(), est);
        if (enderecoFinal == null || enderecoFinal.isBlank()) {
            throw new RequisicaoInvalidaException("Informe ao menos o endereço (CEP/logradouro/número/bairro/cidade/UF ou texto livre)");
        }
        est.setEndereco(enderecoFinal);

        est.setPrecoHora(req.getPrecoHora());
        est.setHorarioAbertura(req.getHorarioAbertura());
        est.setHorarioFechamento(req.getHorarioFechamento());
        est.setDescricao(req.getDescricao());
        est.setLatitude(req.getLatitude()  != null ? req.getLatitude()  : BigDecimal.ZERO);
        est.setLongitude(req.getLongitude() != null ? req.getLongitude() : BigDecimal.ZERO);
        est.setAvaliacaoMedia(BigDecimal.ZERO);
        est.setAtivo(req.getAtivo() == null ? Boolean.TRUE : req.getAtivo());
        est.setPermiteReserva(permiteReserva);
        est.setPixKey(req.getPixKey());

        // Novo estacionamento entra em BASIC (trial de 30 dias) — Script 07-ALTER.
        LocalDateTime agora = LocalDateTime.now();
        est.setPlano(PlanoTipo.BASIC);
        est.setPlanoCobranca(PlanoCobranca.TRIAL);
        est.setPlanoInicio(agora);
        est.setPlanoFim(agora.plusDays(30));

        est = estacionamentoRepository.save(est);

        VagasEstacionamento vagas = new VagasEstacionamento();
        vagas.setEstacionamentoId(est.getId());
        vagas.setQtdVagasTotais(req.getQtdVagasTotais());
        vagas.setQtdVagasDisponiveis(req.getQtdVagasTotais());
        vagas.setQtdVagasReservaveis(reservaveis);
        vagas.setQtdVagasReservadas(0);
        vagasRepository.save(vagas);

        popularVagas(List.of(est));
        return est;
    }

    @Transactional
    public Estacionamento atualizar(Integer id, EstacionamentoUpdateRequest req) {
        Estacionamento est = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (req.getCnpj() != null && !req.getCnpj().isBlank()) {
            String cnpj = DocumentoValidator.somenteDigitos(req.getCnpj());
            DocumentoValidator.validarCnpj(cnpj);
            if (!cnpj.equals(est.getCnpj()) && estacionamentoRepository.existsByCnpj(cnpj)) {
                throw new ConflictException("CNPJ ja em uso");
            }
            est.setCnpj(cnpj);
        }
        if (req.getRazaoSocial() != null && !req.getRazaoSocial().isBlank()) est.setRazaoSocial(req.getRazaoSocial().trim());
        if (req.getNomeFantasia() != null)                                  est.setNomeFantasia(trimOuNull(req.getNomeFantasia()));
        if (req.getNome() != null && !req.getNome().isBlank())              est.setNome(req.getNome().trim());
        else if (req.getRazaoSocial() != null || req.getNomeFantasia() != null) {
            est.setNome(nomeExibicao(est.getNome(), est.getNomeFantasia(), est.getRazaoSocial()));
        }
        if (req.getPrecoHora() != null)                                  est.setPrecoHora(req.getPrecoHora());
        if (req.getHorarioAbertura() != null)                            est.setHorarioAbertura(req.getHorarioAbertura());
        if (req.getHorarioFechamento() != null)                          est.setHorarioFechamento(req.getHorarioFechamento());
        if (req.getDescricao() != null)                                  est.setDescricao(req.getDescricao());
        if (req.getLatitude() != null)                                   est.setLatitude(req.getLatitude());
        if (req.getLongitude() != null)                                  est.setLongitude(req.getLongitude());
        if (req.getAtivo() != null)                                      est.setAtivo(req.getAtivo());
        if (req.getPermiteReserva() != null) {
            if (Boolean.FALSE.equals(req.getPermiteReserva())) {
                VagasEstacionamento vagasAtuais = vagasRepository.findByEstacionamentoId(id).orElse(null);
                if (vagasAtuais != null && vagasAtuais.getQtdVagasReservadas() > 0) {
                    throw new ConflictException("Nao e possivel desativar reservas com reservas ativas");
                }
            }
            est.setPermiteReserva(req.getPermiteReserva());
        }
        if (req.getPixKey() != null)                                     est.setPixKey(req.getPixKey());

        // Endereço detalhado (qualquer campo informado atualiza a coluna correspondente)
        boolean enderecoTocado = false;
        if (req.getCep() != null)         { est.setCep(normalizarCep(req.getCep()));        enderecoTocado = true; }
        if (req.getLogradouro() != null)  { est.setLogradouro(trimOuNull(req.getLogradouro())); enderecoTocado = true; }
        if (req.getNumero() != null)      { est.setNumero(trimOuNull(req.getNumero()));     enderecoTocado = true; }
        if (req.getComplemento() != null) { est.setComplemento(trimOuNull(req.getComplemento())); enderecoTocado = true; }
        if (req.getBairro() != null)      { est.setBairro(trimOuNull(req.getBairro()));     enderecoTocado = true; }
        if (req.getCidade() != null)      { est.setCidade(trimOuNull(req.getCidade()));     enderecoTocado = true; }
        if (req.getUf() != null)          {
            est.setUf(req.getUf().trim().isEmpty() ? null : req.getUf().trim().toUpperCase());
            enderecoTocado = true;
        }
        if (enderecoTocado) {
            validarCidadeSaoPaulo(est.getCidade());
        }

        // Se o cliente enviou o texto livre, ele tem prioridade — senão re-deriva.
        if (req.getEndereco() != null && !req.getEndereco().isBlank()) {
            est.setEndereco(req.getEndereco().trim());
        } else if (enderecoTocado) {
            String derivado = derivarEnderecoTextual(est);
            if (derivado != null) est.setEndereco(derivado);
        }

        est = estacionamentoRepository.save(est);
        popularVagas(List.of(est));
        return est;
    }

    @Transactional
    public Estacionamento atualizarVagas(Integer id, VagasUpdateRequest req) {
        Estacionamento est = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        VagasEstacionamento vagas = vagasRepository.findByEstacionamentoId(id)
                .orElseGet(() -> {
                    VagasEstacionamento novo = new VagasEstacionamento();
                    novo.setEstacionamentoId(id);
                    novo.setQtdVagasTotais(0);
                    novo.setQtdVagasDisponiveis(0);
                    novo.setQtdVagasReservaveis(0);
                    novo.setQtdVagasReservadas(0);
                    return novo;
                });

        if (Boolean.TRUE.equals(est.getPermiteReserva()) && req.getQtdVagasReservaveis() <= 0) {
            throw new RequisicaoInvalidaException("Informe quantas vagas serao reservaveis");
        }
        if (Boolean.FALSE.equals(est.getPermiteReserva()) && req.getQtdVagasReservaveis() > 0) {
            throw new RequisicaoInvalidaException("Ative a permissao de reservas antes de informar vagas reservaveis");
        }
        if (req.getQtdVagasReservaveis() > req.getQtdVagasTotais()) {
            throw new RequisicaoInvalidaException("Vagas reserváveis não pode ser maior que o total de vagas");
        }
        if (req.getQtdVagasReservaveis() < vagas.getQtdVagasReservadas()) {
            throw new ConflictException(
                "Não é possível reduzir vagas reserváveis abaixo das já reservadas (" + vagas.getQtdVagasReservadas() + ")"
            );
        }

        int diferenca = req.getQtdVagasTotais() - vagas.getQtdVagasTotais();
        int novoDisponiveis = vagas.getQtdVagasDisponiveis() + diferenca;
        if (novoDisponiveis < 0) {
            throw new ConflictException("Total de vagas menor que o número de sessões ativas atuais");
        }

        vagas.setQtdVagasTotais(req.getQtdVagasTotais());
        vagas.setQtdVagasReservaveis(req.getQtdVagasReservaveis());
        vagas.setQtdVagasDisponiveis(Math.min(novoDisponiveis, req.getQtdVagasTotais()));
        vagasRepository.save(vagas);

        popularVagas(List.of(est));
        return est;
    }

    @Transactional
    public void desativar(Integer id) {
        Estacionamento est = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
        est.setAtivo(false);
        estacionamentoRepository.save(est);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void popularVagas(List<Estacionamento> estacionamentos) {
        if (estacionamentos.isEmpty()) return;
        List<Integer> ids = estacionamentos.stream().map(Estacionamento::getId).toList();
        Map<Integer, VagasEstacionamento> vagasMap = vagasRepository.findAllByEstacionamentoIdIn(ids)
                .stream()
                .collect(Collectors.toMap(VagasEstacionamento::getEstacionamentoId, v -> v));
        for (Estacionamento e : estacionamentos) {
            VagasEstacionamento v = vagasMap.get(e.getId());
            if (v != null) {
                e.setQtdVagasTotais(v.getQtdVagasTotais());
                e.setQtdVagasDisponiveis(v.getQtdVagasDisponiveis());
                e.setQtdVagasReservaveis(v.getQtdVagasReservaveis());
                e.setQtdVagasReservadas(v.getQtdVagasReservadas());
            }
        }
    }

    // ── Helpers de endereço ──────────────────────────────────────────────────

    private static String trimOuNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    /** Remove tudo que não é dígito; aceita null. Garante <= 8 chars. */
    private static String normalizarCep(String cep) {
        if (cep == null) return null;
        String digits = cep.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        if (digits.length() > 8) digits = digits.substring(0, 8);
        return digits;
    }

    private static String nomeExibicao(String nome, String nomeFantasia, String razaoSocial) {
        String fantasia = trimOuNull(nomeFantasia);
        if (fantasia != null) return fantasia.length() > 100 ? fantasia.substring(0, 100) : fantasia;
        String razao = trimOuNull(razaoSocial);
        if (razao != null) return razao.length() > 100 ? razao.substring(0, 100) : razao;
        String n = trimOuNull(nome);
        if (n == null) return null;
        return n.length() > 100 ? n.substring(0, 100) : n;
    }

    private static void validarCidadeSaoPaulo(String cidade) {
        if (!ehSaoPaulo(cidade)) {
            throw new RequisicaoInvalidaException("No momento, atendemos apenas estacionamentos na cidade de Sao Paulo.");
        }
    }

    private static boolean ehSaoPaulo(String cidade) {
        if (cidade == null) return false;
        String normalizada = Normalizer.normalize(cidade.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return "sao paulo".equals(normalizada);
    }

    /**
     * Escolhe o texto livre do endereço: prioriza o que o cliente enviou;
     * se vazio, deriva a partir das partes detalhadas.
     */
    private static String resolverEnderecoTextual(String enderecoLivre, Estacionamento est) {
        if (enderecoLivre != null && !enderecoLivre.isBlank()) {
            return enderecoLivre.trim();
        }
        String derivado = derivarEnderecoTextual(est);
        return derivado != null ? derivado : "";
    }

    /**
     * Monta "logradouro, numero - bairro, cidade - UF" a partir das partes presentes.
     * Esse formato deve casar com pcMontarEndereco() do frontend (pc-api.js).
     * Retorna null se não houver nenhuma parte útil.
     */
    private static String derivarEnderecoTextual(Estacionamento est) {
        String logradouro = trimOuNull(est.getLogradouro());
        String numero     = trimOuNull(est.getNumero());
        String bairro     = trimOuNull(est.getBairro());
        String cidade     = trimOuNull(est.getCidade());
        String uf         = est.getUf() == null ? null : est.getUf().trim().toUpperCase();
        if (uf != null && uf.isEmpty()) uf = null;

        StringBuilder inicio = new StringBuilder();
        if (logradouro != null) inicio.append(logradouro);
        if (numero != null) {
            if (inicio.length() > 0) inicio.append(", ");
            inicio.append(numero);
        }

        StringBuilder fim = new StringBuilder();
        if (cidade != null) fim.append(cidade);
        if (uf != null) {
            if (fim.length() > 0) fim.append(" - ");
            fim.append(uf);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(inicio);
        if (bairro != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(bairro);
        }
        if (fim.length() > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(fim);
        }

        String s = sb.toString().trim();
        return s.isEmpty() ? null : (s.length() > 300 ? s.substring(0, 300) : s);
    }

    public QrResponse gerarQrEntrada(Integer id) {
        Estacionamento e = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        Map<String, Object> qrMap = new LinkedHashMap<>();
        qrMap.put("tipo", "ENTRADA");
        qrMap.put("id", e.getId());
        qrMap.put("nome", e.getNome());
        qrMap.put("precoHora", e.getPrecoHora());

        try {
            return new QrResponse(objectMapper.writeValueAsString(qrMap));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Erro ao gerar JSON do QR Code", ex);
        }
    }
}
