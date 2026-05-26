package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CRUD de operadores do kiosk. Diferente do administrador web (AdmEstacionamento),
 * o operador autentica apenas na aplicação de balcão (ParadaCertaManager.html).
 *
 * LGPD: logs SEMPRE registram apenas id e/ou usuario do operador — nunca CPF,
 * email, telefone ou endereço. CPF é mascarado em listagens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperadorService {

    private static final Pattern EMAIL_REGEX = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final OperadorEstacionamentoRepository operadorRepository;
    private final EstacionamentoRepository estacionamentoRepository;

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OperadorResponse> listarPorEstacionamento(Integer estacionamentoId) {
        return operadorRepository.findByEstacionamentoIdOrderByIdAsc(estacionamentoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Listagem com CPF mascarado (LGPD). Para detalhes completos use {@link #detalhe(Integer)}. */
    @Transactional(readOnly = true)
    public OperadorResponse buscarPorId(Integer id) {
        return toResponse(carregar(id));
    }

    /** Detalhe completo — CPF visível. Usar apenas na tela de edição. */
    @Transactional(readOnly = true)
    public OperadorDetailResponse detalhe(Integer id) {
        return toDetailResponse(carregar(id));
    }

    // ── Criação ──────────────────────────────────────────────────────────────

    @Transactional
    public OperadorResponse criar(OperadorRequest req) {
        if (!estacionamentoRepository.existsById(req.getEstacionamentoId())) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        String usuario = req.getUsuario().trim();
        if (operadorRepository.existsByUsuario(usuario)) {
            throw new ConflictException("Já existe um operador com este nome de usuário");
        }

        String cpf = somenteDigitos(req.getCpf());
        validarCpf(cpf);
        if (operadorRepository.existsByCpf(cpf)) {
            throw new ConflictException("Já existe um operador cadastrado com este CPF");
        }

        validarEmail(req.getEmail());

        OperadorEstacionamento op = new OperadorEstacionamento();
        op.setEstacionamentoId(req.getEstacionamentoId());
        op.setNome(req.getNome().trim());
        op.setUsuario(usuario);
        op.setSenhaHash(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt(10)));
        op.setAtivo(true);
        op.setCriadoEm(LocalDateTime.now());

        // Dados pessoais
        op.setCpf(cpf);
        op.setEmail(req.getEmail().trim());
        op.setTelefone(somenteDigitos(req.getTelefone()));

        // Endereço
        op.setCep(somenteDigitos(req.getCep()));
        op.setLogradouro(trimOuNull(req.getLogradouro()));
        op.setNumero(trimOuNull(req.getNumero()));
        op.setComplemento(trimOuNull(req.getComplemento()));
        op.setBairro(trimOuNull(req.getBairro()));
        op.setCidade(trimOuNull(req.getCidade()));
        op.setUf(req.getUf() == null ? null : req.getUf().trim().toUpperCase());
        validarCidadeSaoPaulo(op.getCidade());

        OperadorEstacionamento salvo = operadorRepository.save(op);
        log.info("Operador criado: id={}, usuario={}, estacionamentoId={}",
                salvo.getId(), salvo.getUsuario(), salvo.getEstacionamentoId());
        return toResponse(salvo);
    }

    /**
     * Operador inicial criado em conjunto com o cadastro do estacionamento.
     * Não recebe ainda os dados pessoais completos (preenchidos depois pelo admin
     * via PUT /api/operador/{id}).
     */
    @Transactional
    public OperadorEstacionamento criarOperadorInicial(
            Integer estacionamentoId,
            String nome,
            String usuario,
            String senha
    ) {
        String usr = usuario == null ? null : usuario.trim();
        if (usr == null || usr.isEmpty()) {
            throw new ConflictException("Usuário do operador inicial é obrigatório");
        }
        if (operadorRepository.existsByUsuario(usr)) {
            throw new ConflictException("Já existe um operador com este nome de usuário");
        }

        OperadorEstacionamento op = new OperadorEstacionamento();
        op.setEstacionamentoId(estacionamentoId);
        op.setNome(nome.trim());
        op.setUsuario(usr);
        op.setSenhaHash(BCrypt.hashpw(senha, BCrypt.gensalt(10)));
        op.setAtivo(true);
        op.setCriadoEm(LocalDateTime.now());
        OperadorEstacionamento salvo = operadorRepository.save(op);
        log.info("Operador inicial criado: id={}, usuario={}, estacionamentoId={}",
                salvo.getId(), salvo.getUsuario(), estacionamentoId);
        return salvo;
    }

    // ── Atualização ──────────────────────────────────────────────────────────

    @Transactional
    public OperadorResponse atualizar(Integer id, OperadorUpdateRequest req) {
        OperadorEstacionamento op = carregar(id);

        if (req.getNome() != null && !req.getNome().isBlank()) {
            op.setNome(req.getNome().trim());
        }
        if (req.getUsuario() != null && !req.getUsuario().isBlank()) {
            String novo = req.getUsuario().trim();
            if (!novo.equals(op.getUsuario()) && operadorRepository.existsByUsuario(novo)) {
                throw new ConflictException("Já existe um operador com este nome de usuário");
            }
            op.setUsuario(novo);
        }
        if (req.getAtivo() != null) {
            op.setAtivo(req.getAtivo());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            validarEmail(req.getEmail());
            op.setEmail(req.getEmail().trim());
        }
        if (req.getTelefone() != null) {
            op.setTelefone(somenteDigitos(req.getTelefone()));
        }

        // Endereço
        if (req.getCep() != null)         op.setCep(somenteDigitos(req.getCep()));
        if (req.getLogradouro() != null)  op.setLogradouro(trimOuNull(req.getLogradouro()));
        if (req.getNumero() != null)      op.setNumero(trimOuNull(req.getNumero()));
        if (req.getComplemento() != null) op.setComplemento(trimOuNull(req.getComplemento()));
        if (req.getBairro() != null)      op.setBairro(trimOuNull(req.getBairro()));
        if (req.getCidade() != null)      op.setCidade(trimOuNull(req.getCidade()));
        if (req.getUf() != null)          op.setUf(req.getUf().trim().toUpperCase());
        if (req.getCidade() != null)      validarCidadeSaoPaulo(op.getCidade());

        OperadorEstacionamento salvo = operadorRepository.save(op);
        log.info("Operador atualizado: id={}, usuario={}", salvo.getId(), salvo.getUsuario());
        return toResponse(salvo);
    }

    @Transactional
    public void trocarSenha(Integer id, OperadorSenhaRequest req) {
        OperadorEstacionamento op = carregar(id);
        op.setSenhaHash(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt(10)));
        operadorRepository.save(op);
        log.info("Senha do operador trocada: id={}", id);
    }

    /** Soft delete: ativo = false. Mantém o histórico (FK em sessões etc.). */
    @Transactional
    public void desativar(Integer id) {
        OperadorEstacionamento op = carregar(id);
        op.setAtivo(false);
        operadorRepository.save(op);
        log.info("Operador desativado: id={}", id);
    }

    // ── Validações ───────────────────────────────────────────────────────────

    /**
     * Algoritmo oficial do CPF: 11 dígitos, rejeita sequências repetidas e valida
     * os dois dígitos verificadores. Pacote-privado para permitir testes unitários.
     */
    static void validarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            throw new RequisicaoInvalidaException("CPF inválido: deve ter 11 dígitos");
        }
        for (int i = 0; i < 11; i++) {
            if (!Character.isDigit(cpf.charAt(i))) {
                throw new RequisicaoInvalidaException("CPF inválido: contém caracteres não numéricos");
            }
        }
        // Rejeita sequências repetidas (000...000, 111...111, ..., 999...999)
        boolean todosIguais = true;
        for (int i = 1; i < 11; i++) {
            if (cpf.charAt(i) != cpf.charAt(0)) { todosIguais = false; break; }
        }
        if (todosIguais) {
            throw new RequisicaoInvalidaException("CPF inválido");
        }

        // Primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += Character.digit(cpf.charAt(i), 10) * (10 - i);
        }
        int resto = soma % 11;
        int dig1 = (resto < 2) ? 0 : 11 - resto;
        if (dig1 != Character.digit(cpf.charAt(9), 10)) {
            throw new RequisicaoInvalidaException("CPF inválido");
        }

        // Segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.digit(cpf.charAt(i), 10) * (11 - i);
        }
        resto = soma % 11;
        int dig2 = (resto < 2) ? 0 : 11 - resto;
        if (dig2 != Character.digit(cpf.charAt(10), 10)) {
            throw new RequisicaoInvalidaException("CPF inválido");
        }
    }

    private static void validarEmail(String email) {
        if (email == null || !EMAIL_REGEX.matcher(email.trim()).matches()) {
            throw new RequisicaoInvalidaException("E-mail inválido");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private OperadorEstacionamento carregar(Integer id) {
        return operadorRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Operador não encontrado"));
    }

    private static String trimOuNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String somenteDigitos(String v) {
        if (v == null) return null;
        String d = v.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    /** Mascaramento LGPD: "***.***.***-NN" (mostra só os 2 últimos dígitos). */
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

    private OperadorResponse toResponse(OperadorEstacionamento op) {
        return new OperadorResponse(
                op.getId(),
                op.getEstacionamentoId(),
                op.getNome(),
                op.getUsuario(),
                op.getAtivo(),
                op.getCriadoEm(),
                mascararCpf(op.getCpf()),
                op.getEmail(),
                op.getTelefone(),
                op.getCidade(),
                op.getUf()
        );
    }

    private OperadorDetailResponse toDetailResponse(OperadorEstacionamento op) {
        return new OperadorDetailResponse(
                op.getId(),
                op.getEstacionamentoId(),
                op.getNome(),
                op.getUsuario(),
                op.getAtivo(),
                op.getCriadoEm(),
                op.getCpf(),
                op.getEmail(),
                op.getTelefone(),
                op.getCep(),
                op.getLogradouro(),
                op.getNumero(),
                op.getComplemento(),
                op.getBairro(),
                op.getCidade(),
                op.getUf()
        );
    }
}
