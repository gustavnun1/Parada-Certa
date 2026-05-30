package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.AdmEstacionamentoRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo painel web administrativo:
 *  - Cadastro inicial (estabelecimento + responsável) em uma transação.
 *  - Login do operador/admin por e-mail ou CPF + senha (BCrypt).
 *  - CRUD de operadores adicionais por estacionamento.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdmEstacionamentoRepository admRepository;
    private final EstacionamentoRepository    estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final OperadorService operadorService;
    private final FotoService fotoService;

    // ── Login ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest req) {
        String login = req.getEmail() == null ? "" : req.getEmail().trim();
        if (login.isEmpty() || req.getSenha() == null || req.getSenha().isEmpty()) {
            throw new RequisicaoInvalidaException("Informe e-mail/CPF e senha");
        }

        AdmEstacionamento adm = buscarAdminPorLogin(login)
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais incorretas"));

        if (Boolean.FALSE.equals(adm.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário desativado");
        }

        if (!BCrypt.checkpw(req.getSenha(), adm.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Credenciais incorretas");
        }

        Estacionamento est = estacionamentoRepository.findById(adm.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento vinculado não encontrado"));

        return new AdminLoginResponse(
                adm.getId(),
                adm.getUsuario(),
                adm.getNomeCompleto(),
                adm.getEmail(),
                adm.getTelefone(),
                est.getId(),
                est.getNome()
        );
    }

    private Optional<AdmEstacionamento> buscarAdminPorLogin(String login) {
        String cpf = DocumentoValidator.somenteDigitos(login);
        if (!login.contains("@") && cpf != null && cpf.length() == 11) {
            return admRepository.findByCpf(cpf);
        }
        return admRepository.findByEmailIgnoreCase(login);
    }

    // ── Cadastro inicial (estabelecimento + responsável) ─────────────────────

    @Transactional
    public AdminCadastroResponse cadastrar(AdminCadastroRequest req) {
        AdminCadastroRequest.Responsavel resp = req.getResponsavel();
        AdminCadastroRequest.EstacionamentoDados estData = req.getEstacionamento();

        String nomeResp = UserFieldValidator.normalizarNome(resp.getNome());
        java.time.LocalDate dataNascimentoResp = UserFieldValidator.validarDataNascimento(resp.getDataNascimento());
        String email = UserFieldValidator.normalizarEmail(resp.getEmail());
        if (admRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um administrador com este e-mail");
        }

        String cpfAdmin = DocumentoValidator.somenteDigitos(resp.getCpf());
        DocumentoValidator.validarCpf(cpfAdmin);
        if (admRepository.existsByCpf(cpfAdmin)) {
            throw new ConflictException("Ja existe um administrador cadastrado com este CPF");
        }

        String usuario = resp.getUsuario() == null || resp.getUsuario().isBlank()
                ? gerarUsuarioDoEmail(email)
                : resp.getUsuario().trim();

        if (admRepository.existsByUsuario(usuario)) {
            throw new ConflictException("Já existe um administrador com este nome de usuário");
        }

        Integer reservaveis = estData.getQtdVagasReservaveis() == null ? 0 : estData.getQtdVagasReservaveis();
        boolean permiteReserva = Boolean.TRUE.equals(estData.getPermiteReserva());
        if (!permiteReserva) reservaveis = 0;
        if (permiteReserva && reservaveis <= 0) {
            throw new RequisicaoInvalidaException("Informe quantas vagas serao reservaveis");
        }
        if (reservaveis > estData.getQtdVagasTotais()) {
            throw new RequisicaoInvalidaException("Vagas reserváveis não pode ser maior que o total de vagas");
        }

        // 1. Cria o estacionamento (com endereço detalhado + texto livre derivado)
        Estacionamento est = new Estacionamento();
        String cnpj = DocumentoValidator.somenteDigitos(estData.getCnpj());
        DocumentoValidator.validarCnpj(cnpj);
        if (estacionamentoRepository.existsByCnpj(cnpj)) {
            throw new ConflictException("Ja existe um estacionamento cadastrado com este CNPJ");
        }

        est.setCnpj(cnpj);
        est.setRazaoSocial(estData.getRazaoSocial().trim());
        est.setNomeFantasia(trimOuNull(estData.getNomeFantasia()));
        est.setNome(nomeExibicao(estData.getNome(), estData.getNomeFantasia(), estData.getRazaoSocial()));

        est.setCep(normalizarCep(estData.getCep()));
        est.setLogradouro(trimOuNull(estData.getLogradouro()));
        est.setNumero(trimOuNull(estData.getNumero()));
        est.setComplemento(trimOuNull(estData.getComplemento()));
        est.setBairro(trimOuNull(estData.getBairro()));
        est.setCidade(trimOuNull(estData.getCidade()));
        est.setUf(estData.getUf() == null ? null : estData.getUf().trim().toUpperCase());
        validarCidadeSaoPaulo(est.getCidade());

        String enderecoFinal = resolverEnderecoTextual(estData.getEndereco(), est);
        if (enderecoFinal == null || enderecoFinal.isBlank()) {
            throw new RequisicaoInvalidaException("Informe ao menos o endereço (CEP/logradouro/número/bairro/cidade/UF ou texto livre)");
        }
        est.setEndereco(enderecoFinal);

        est.setPrecoHora(estData.getPrecoHora());
        est.setHorarioAbertura(estData.getHorarioAbertura());
        est.setHorarioFechamento(estData.getHorarioFechamento());
        est.setDescricao(estData.getDescricao());
        est.setLatitude(estData.getLatitude()  != null ? estData.getLatitude()  : BigDecimal.ZERO);
        est.setLongitude(estData.getLongitude() != null ? estData.getLongitude() : BigDecimal.ZERO);
        est.setAvaliacaoMedia(BigDecimal.ZERO);
        est.setAtivo(true);
        est.setPermiteReserva(permiteReserva);
        est.setPixKey(estData.getPixKey());

        // Novo estacionamento entra em BASIC (trial de 30 dias) — Script 07-ALTER.
        LocalDateTime agora = LocalDateTime.now();
        est.setPlano(PlanoTipo.BASIC);
        est.setPlanoCobranca(PlanoCobranca.TRIAL);
        est.setPlanoInicio(agora);
        est.setPlanoFim(agora.plusDays(30));

        est = estacionamentoRepository.save(est);

        // 2. Cria a linha de VagasEstacionamento (trigger recalcula disponíveis depois)
        VagasEstacionamento vagas = new VagasEstacionamento();
        vagas.setEstacionamentoId(est.getId());
        vagas.setQtdVagasTotais(estData.getQtdVagasTotais());
        vagas.setQtdVagasDisponiveis(estData.getQtdVagasTotais());
        vagas.setQtdVagasReservaveis(reservaveis);
        vagas.setQtdVagasReservadas(0);
        vagasRepository.save(vagas);

        // 3. Cria o admin responsável (BCrypt)
        AdmEstacionamento adm = new AdmEstacionamento();
        adm.setEstacionamentoId(est.getId());
        adm.setNomeCompleto(nomeResp);
        adm.setEmail(email);
        adm.setTelefone(resp.getTelefone() == null ? null : resp.getTelefone().trim());
        adm.setCpf(cpfAdmin);
        adm.setDataNascimento(dataNascimentoResp);
        adm.setUsuario(usuario);
        adm.setSenhaHash(BCrypt.hashpw(resp.getSenha(), BCrypt.gensalt(10)));
        adm.setAtivo(true);
        adm = admRepository.save(adm);

        // 4. Cria o operador inicial do kiosk (obrigatório) na mesma transação.
        //    Reutiliza OperadorService.criar(...) para não duplicar validação de
        //    CPF (algoritmo dos 2 dígitos), email, duplicidade de CPF/usuário,
        //    hash BCrypt e normalização de telefone/CEP. Se algo falhar, a
        //    @Transactional desta classe garante o rollback do estacionamento,
        //    vagas e admin já criados acima.
        AdminCadastroRequest.OperadorInicial opInicial = req.getOperadorInicial();
        if (opInicial == null) {
            throw new RequisicaoInvalidaException("Operador inicial é obrigatório");
        }

        OperadorRequest opReq = new OperadorRequest();
        opReq.setEstacionamentoId(est.getId());
        opReq.setNome(opInicial.getNome());
        opReq.setUsuario(opInicial.getUsuario());
        opReq.setSenha(opInicial.getSenha());
        opReq.setCpf(opInicial.getCpf());
        opReq.setEmail(opInicial.getEmail());
        opReq.setTelefone(opInicial.getTelefone());
        opReq.setCep(opInicial.getCep());
        opReq.setLogradouro(opInicial.getLogradouro());
        opReq.setNumero(opInicial.getNumero());
        opReq.setComplemento(opInicial.getComplemento());
        opReq.setBairro(opInicial.getBairro());
        opReq.setCidade(opInicial.getCidade());
        opReq.setUf(opInicial.getUf());

        operadorService.criar(opReq);

        return new AdminCadastroResponse(
                adm.getId(),
                adm.getUsuario(),
                adm.getEmail(),
                adm.getNomeCompleto(),
                est.getId(),
                est.getNome()
        );
    }

    @Transactional
    public AdminCadastroResponse cadastrarComFotos(AdminCadastroRequest req, List<MultipartFile> fotos) {
        List<MultipartFile> fotosValidas = fotos == null ? Collections.emptyList() : fotos.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (fotosValidas.size() > 3) {
            throw new RequisicaoInvalidaException("O plano BASIC trial permite ate 3 fotos no cadastro inicial.");
        }

        AdminCadastroResponse resp = cadastrar(req);

        for (int i = 0; i < fotosValidas.size(); i++) {
            fotoService.upload(resp.getEstacionamentoId(), fotosValidas.get(i), i == 0);
        }

        return resp;
    }

    // ── CRUD de operadores adicionais por estacionamento ─────────────────────

    @Transactional(readOnly = true)
    public AdminResponse buscarPorId(Integer id) {
        AdmEstacionamento adm = admRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Administrador não encontrado"));
        return toResponse(adm);
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> listarPorEstacionamento(Integer estacionamentoId) {
        return admRepository.findByEstacionamentoIdOrderByIdAsc(estacionamentoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminResponse cadastrarOperador(OperadorCadastroRequest req) {
        if (!estacionamentoRepository.existsById(req.getEstacionamentoId())) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        String nomeValidado = UserFieldValidator.normalizarNome(req.getNomeCompleto());
        String email = UserFieldValidator.normalizarEmail(req.getEmail());
        if (admRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um administrador com este e-mail");
        }

        String cpf = DocumentoValidator.somenteDigitos(req.getCpf());
        DocumentoValidator.validarCpf(cpf);
        if (admRepository.existsByCpf(cpf)) {
            throw new ConflictException("Ja existe um administrador cadastrado com este CPF");
        }

        String usuario = req.getUsuario() == null || req.getUsuario().isBlank()
                ? gerarUsuarioDoEmail(email)
                : req.getUsuario().trim();
        if (admRepository.existsByUsuario(usuario)) {
            throw new ConflictException("Já existe um administrador com este nome de usuário");
        }

        AdmEstacionamento adm = new AdmEstacionamento();
        adm.setEstacionamentoId(req.getEstacionamentoId());
        adm.setNomeCompleto(nomeValidado);
        adm.setEmail(email);
        adm.setTelefone(req.getTelefone());
        adm.setCpf(cpf);
        adm.setUsuario(usuario);
        adm.setSenhaHash(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt(10)));
        adm.setAtivo(true);
        adm = admRepository.save(adm);

        return toResponse(adm);
    }

    @Transactional
    public AdminResponse atualizar(Integer id, AdminUpdateRequest req) {
        AdmEstacionamento adm = admRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Administrador não encontrado"));

        if (req.getNomeCompleto() != null && !req.getNomeCompleto().isBlank()) {
            adm.setNomeCompleto(UserFieldValidator.normalizarNome(req.getNomeCompleto()));
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String novoEmail = UserFieldValidator.normalizarEmail(req.getEmail());
            if (!novoEmail.equalsIgnoreCase(adm.getEmail() == null ? "" : adm.getEmail())
                    && admRepository.existsByEmailIgnoreCase(novoEmail)) {
                throw new ConflictException("E-mail já em uso");
            }
            adm.setEmail(novoEmail);
        }

        if (req.getTelefone() != null) {
            adm.setTelefone(req.getTelefone().trim());
        }

        if (req.getCpf() != null && !req.getCpf().isBlank()) {
            String cpf = DocumentoValidator.somenteDigitos(req.getCpf());
            DocumentoValidator.validarCpf(cpf);
            if (!cpf.equals(adm.getCpf()) && admRepository.existsByCpf(cpf)) {
                throw new ConflictException("CPF ja em uso");
            }
            adm.setCpf(cpf);
        }

        if (req.getDataNascimento() != null) {
            adm.setDataNascimento(UserFieldValidator.validarDataNascimento(req.getDataNascimento()));
        }

        if (req.getSenha() != null && !req.getSenha().isBlank()) {
            adm.setSenhaHash(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt(10)));
        }

        if (req.getAtivo() != null) {
            adm.setAtivo(req.getAtivo());
        }

        return toResponse(admRepository.save(adm));
    }

    @Transactional
    public void desativar(Integer id) {
        AdmEstacionamento adm = admRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Administrador não encontrado"));
        adm.setAtivo(false);
        admRepository.save(adm);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Gera nome de usuário único a partir do e-mail: parte antes do @, em minúsculo,
     * sem caracteres especiais; acrescenta sufixo numérico se já existir.
     */
    private String gerarUsuarioDoEmail(String email) {
        String base = email.split("@")[0]
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "");
        if (base.length() > 40) base = base.substring(0, 40);

        String candidato = base;
        int suf = 1;
        while (admRepository.existsByUsuario(candidato)) {
            candidato = base + "_" + suf;
            suf++;
            if (suf > 9999) {
                throw new ConflictException("Não foi possível gerar um nome de usuário único");
            }
        }
        return candidato;
    }

    private static String trimOuNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizarCep(String cep) {
        if (cep == null) return null;
        String digits = cep.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        if (digits.length() > 8) digits = digits.substring(0, 8);
        return digits;
    }

    private static String resolverEnderecoTextual(String enderecoLivre, Estacionamento est) {
        if (enderecoLivre != null && !enderecoLivre.isBlank()) return enderecoLivre.trim();
        return derivarEnderecoTextual(est);
    }

    /** Padrão: "logradouro, numero - bairro, cidade - UF". */
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

    private AdminResponse toResponse(AdmEstacionamento adm) {
        return new AdminResponse(
                adm.getId(),
                adm.getUsuario(),
                adm.getNomeCompleto(),
                adm.getEmail(),
                adm.getTelefone(),
                mascararCpf(adm.getCpf()),
                adm.getEstacionamentoId(),
                adm.getAtivo()
        );
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

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return null;
        return "***.***.***-" + cpf.substring(9);
    }
}
