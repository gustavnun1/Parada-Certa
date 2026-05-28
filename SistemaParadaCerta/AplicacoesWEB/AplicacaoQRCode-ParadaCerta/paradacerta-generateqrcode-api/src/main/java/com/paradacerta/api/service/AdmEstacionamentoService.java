package com.paradacerta.api.service;

import com.paradacerta.api.dto.AdmDTO;
import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.OperadorEstacionamento;
import com.paradacerta.api.model.QrCodeEntrada;
import com.paradacerta.api.model.SessaoEstacionamento;
import com.paradacerta.api.model.VagasEstacionamento;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import com.paradacerta.api.repository.QrCodeEntradaRepository;
import com.paradacerta.api.repository.SessaoEstacionamentoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdmEstacionamentoService {

    private final OperadorEstacionamentoRepository operadorRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final QrCodeEntradaRepository qrCodeRepository;
    private final SessaoEstacionamentoRepository sessaoRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String MSG_CEP_INVALIDO = "CEP invalido ou nao encontrado. Verifique o CEP informado e tente novamente.";
    private static final String MSG_SOMENTE_SP = "No momento, o Parada Certa aceita apenas estacionamentos localizados na cidade de Sao Paulo.";

    public AdmDTO.LoginResponse login(AdmDTO.LoginRequest request) {
        OperadorEstacionamento op = operadorRepository.findByUsuarioAndAtivoTrue(request.getUsuario())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuario ou senha invalidos"));

        if (!BCrypt.checkpw(request.getSenha(), op.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuario ou senha invalidos");
        }

        Estacionamento est = estacionamentoRepository.findById(op.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
        VagasEstacionamento vagas = buscarVagas(est.getId());

        return AdmDTO.LoginResponse.builder()
                .admId(op.getId())
                .nomeCompleto(op.getNome())
                .estacionamentoId(est.getId())
                .nomeEstacionamento(est.getNome())
                .vagasDisponiveis(vagas.getQtdVagasDisponiveis())
                .vagasTotais(vagas.getQtdVagasTotais())
                .build();
    }

    @Transactional
    public AdmDTO.GerarQrCodeResponse gerarQrCode(AdmDTO.GerarQrCodeRequest request) {
        OperadorEstacionamento op = operadorRepository.findById(request.getAdmId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Operador nao encontrado"));

        if (Boolean.FALSE.equals(op.getAtivo())) {
            throw new CredenciaisInvalidasException("Operador desativado");
        }

        if (!op.getEstacionamentoId().equals(request.getEstacionamentoId())) {
            throw new RequisicaoInvalidaException("Operador nao pertence a este estacionamento");
        }

        Estacionamento est = estacionamentoRepository.findById(request.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
        VagasEstacionamento vagas = buscarVagas(est.getId());

        if (vagas.getQtdVagasDisponiveis() <= 0) {
            throw new RequisicaoInvalidaException("Estacionamento lotado - nenhuma vaga disponivel");
        }

        qrCodeRepository.invalidarTokensDisponiveis(request.getEstacionamentoId());

        String token = UUID.randomUUID().toString();
        LocalDateTime agora = LocalDateTime.now(ZONE_SAO_PAULO);
        LocalDateTime expiracao = agora.plusMinutes(30);
        BigDecimal valorMinimo = est.getPrecoHora() != null ? est.getPrecoHora() : BigDecimal.ZERO;

        QrCodeEntrada qrCode = QrCodeEntrada.builder()
                .token(token)
                .estacionamentoId(request.getEstacionamentoId())
                .geradoPor(request.getAdmId())
                .geradoEm(agora)
                .expiradoEm(expiracao)
                .status("UTILIZADO")
                .build();

        qrCodeRepository.save(qrCode);

        SessaoEstacionamento sessao = SessaoEstacionamento.builder()
                .clienteId(null)
                .estacionamentoId(request.getEstacionamentoId())
                .horaEntrada(agora)
                .status(SessaoEstacionamento.STATUS_ATIVA)
                .qrCode(token)
                .reservado(false)
                .valorPago(valorMinimo)
                .build();

        sessaoRepository.save(sessao);

        String payload = String.format(
                "{\"v\":1,\"app\":\"paradacerta\",\"type\":\"entrada\",\"estacionamentoId\":%d,\"token\":\"%s\"}",
                request.getEstacionamentoId(), token
        );

        return AdmDTO.GerarQrCodeResponse.builder()
                .token(token)
                .estacionamentoId(request.getEstacionamentoId())
                .nomeEstacionamento(est.getNome())
                .geradoEm(agora.format(FORMATTER))
                .expiradoEm(expiracao.format(FORMATTER))
                .qrCodePayload(payload)
                .build();
    }

    public AdmDTO.StatusResponse getStatus(Integer estacionamentoId) {
        Estacionamento est = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
        VagasEstacionamento vagas = buscarVagas(est.getId());

        long sessoesAtivas = sessaoRepository.countByEstacionamentoIdAndStatus(
                estacionamentoId, SessaoEstacionamento.STATUS_ATIVA);

        return AdmDTO.StatusResponse.builder()
                .vagasDisponiveis(vagas.getQtdVagasDisponiveis())
                .vagasTotais(vagas.getQtdVagasTotais())
                .sessoesAtivas(sessoesAtivas)
                .build();
    }

    public List<AdmDTO.EstacionamentoResponse> listarEstacionamentosDoAdmin(Integer adminId) {
        OperadorEstacionamento op = buscarOperadorAtivo(adminId);
        Estacionamento est = estacionamentoRepository.findById(op.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
        return List.of(toResponse(est, vagasRepository.findByEstacionamentoId(est.getId()).orElse(null)));
    }

    public AdmDTO.EstacionamentoResponse buscarEstacionamento(Integer id, Integer adminId) {
        if (adminId != null) {
            validarOperadorPodeAcessar(adminId, id);
        }
        Estacionamento est = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
        return toResponse(est, vagasRepository.findByEstacionamentoId(est.getId()).orElse(null));
    }

    @Transactional
    public AdmDTO.EstacionamentoResponse atualizarEstacionamento(Integer id, AdmDTO.AtualizarEstacionamentoRequest request) {
        Integer adminId = request != null ? request.getAdminId() : null;
        validarOperadorPodeAcessar(adminId, id);
        Estacionamento est = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));

        if (request == null) {
            throw new RequisicaoInvalidaException("Dados de atualizacao ausentes");
        }

        copiarSePreenchido(request.getNome(), est::setNome);
        copiarSePreenchido(request.getEndereco(), est::setEndereco);
        if (request.getPrecoHora() != null) est.setPrecoHora(request.getPrecoHora());
        if (request.getHorarioAbertura() != null) est.setHorarioAbertura(request.getHorarioAbertura());
        if (request.getHorarioFechamento() != null) est.setHorarioFechamento(request.getHorarioFechamento());
        if (request.getDescricao() != null) est.setDescricao(request.getDescricao().trim());
        if (request.getAtivo() != null) est.setAtivo(request.getAtivo());
        if (request.getPermiteReserva() != null) est.setPermiteReserva(request.getPermiteReserva());
        if (request.getPixKey() != null) est.setPixKey(vazioParaNull(request.getPixKey()));

        String cep = somenteDigitos(request.getCep());
        boolean recebeuCep = request.getCep() != null && !cep.isBlank();
        boolean recebeuEnderecoDetalhado = temTexto(request.getLogradouro()) || temTexto(request.getCidade()) || temTexto(request.getUf());

        if (recebeuCep) {
            aplicarEnderecoPorCep(est, request, cep);
        } else if (recebeuEnderecoDetalhado) {
            aplicarEnderecoDetalhado(est, request);
        }

        Estacionamento salvo = estacionamentoRepository.save(est);
        return toResponse(salvo, vagasRepository.findByEstacionamentoId(salvo.getId()).orElse(null));
    }

    @Transactional
    public void atualizarVagas(Integer estacionamentoId, AdmDTO.AtualizarVagasRequest request) {
        Integer adminId = request != null ? request.getAdminId() : null;
        validarOperadorPodeAcessar(adminId, estacionamentoId);
        VagasEstacionamento vagas = buscarVagas(estacionamentoId);
        if (request.getQtdVagasTotais() != null) {
            int total = Math.max(0, request.getQtdVagasTotais());
            int usadas = Math.max(0, vagas.getQtdVagasTotais() - vagas.getQtdVagasDisponiveis());
            vagas.setQtdVagasTotais(total);
            vagas.setQtdVagasDisponiveis(Math.max(0, total - usadas));
        }
        if (request.getQtdVagasReservaveis() != null) {
            vagas.setQtdVagasReservaveis(Math.max(0, request.getQtdVagasReservaveis()));
        }
        vagasRepository.save(vagas);
    }

    @Transactional
    public AdmDTO.ExclusaoContaResponse excluirPropriaConta(Integer adminId) {
        OperadorEstacionamento op = buscarOperadorAtivo(adminId);
        Integer estacionamentoId = op.getEstacionamentoId();

        List<Integer> estacionamentosExcluidos = new ArrayList<>();
        estacionamentosExcluidos.add(estacionamentoId);
        int operadoresExcluidos = operadorRepository.findByEstacionamentoId(estacionamentoId).size();

        excluirDependenciasOpcionais(estacionamentoId);
        sessaoRepository.deleteByEstacionamentoId(estacionamentoId);
        qrCodeRepository.deleteByEstacionamentoId(estacionamentoId);
        vagasRepository.deleteByEstacionamentoId(estacionamentoId);
        operadorRepository.deleteByEstacionamentoId(estacionamentoId);
        estacionamentoRepository.deleteById(estacionamentoId);

        return AdmDTO.ExclusaoContaResponse.builder()
                .adminId(adminId)
                .estacionamentosExcluidos(estacionamentosExcluidos)
                .operadoresExcluidos(operadoresExcluidos)
                .build();
    }

    private OperadorEstacionamento buscarOperadorAtivo(Integer adminId) {
        if (adminId == null) {
            throw new CredenciaisInvalidasException("Sessao administrativa invalida");
        }
        OperadorEstacionamento op = operadorRepository.findById(adminId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Operador nao encontrado"));
        if (Boolean.FALSE.equals(op.getAtivo())) {
            throw new CredenciaisInvalidasException("Operador desativado");
        }
        return op;
    }

    private void validarOperadorPodeAcessar(Integer adminId, Integer estacionamentoId) {
        OperadorEstacionamento op = buscarOperadorAtivo(adminId);
        if (!op.getEstacionamentoId().equals(estacionamentoId)) {
            throw new RequisicaoInvalidaException("Operador nao pertence a este estacionamento");
        }
    }

    private void aplicarEnderecoPorCep(Estacionamento est, AdmDTO.AtualizarEstacionamentoRequest request, String cep) {
        if (cep.length() != 8) {
            throw new RequisicaoInvalidaException(MSG_CEP_INVALIDO);
        }

        Map<String, Object> viaCep = consultarViaCep(cep);
        if (viaCep == null || Boolean.TRUE.equals(viaCep.get("erro"))) {
            throw new RequisicaoInvalidaException(MSG_CEP_INVALIDO);
        }

        String cidade = str(viaCep.get("localidade"));
        if (!ehCidadeSaoPaulo(cidade)) {
            throw new RequisicaoInvalidaException(MSG_SOMENTE_SP);
        }

        String logradouro = primeiroTexto(request.getLogradouro(), str(viaCep.get("logradouro")));
        String bairro = primeiroTexto(request.getBairro(), str(viaCep.get("bairro")));
        String uf = primeiroTexto(request.getUf(), str(viaCep.get("uf"))).toUpperCase();
        String numero = vazioParaNull(request.getNumero());
        String complemento = primeiroTexto(request.getComplemento(), str(viaCep.get("complemento")));

        if (!temTexto(logradouro) || !temTexto(bairro) || !temTexto(cidade) || !temTexto(uf)) {
            throw new RequisicaoInvalidaException(MSG_CEP_INVALIDO);
        }

        est.setCep(cep);
        est.setLogradouro(logradouro);
        est.setNumero(numero);
        est.setComplemento(vazioParaNull(complemento));
        est.setBairro(bairro);
        est.setCidade(cidade);
        est.setUf(uf);
        est.setEndereco(montarEndereco(logradouro, numero, bairro, cidade, uf));

        recalcularCoordenadas(est);
    }

    private void aplicarEnderecoDetalhado(Estacionamento est, AdmDTO.AtualizarEstacionamentoRequest request) {
        String cidade = primeiroTexto(request.getCidade(), est.getCidade());
        if (!ehCidadeSaoPaulo(cidade)) {
            throw new RequisicaoInvalidaException(MSG_SOMENTE_SP);
        }
        copiarSePreenchido(request.getLogradouro(), est::setLogradouro);
        copiarSePreenchido(request.getNumero(), est::setNumero);
        if (request.getComplemento() != null) est.setComplemento(vazioParaNull(request.getComplemento()));
        copiarSePreenchido(request.getBairro(), est::setBairro);
        copiarSePreenchido(request.getCidade(), est::setCidade);
        copiarSePreenchido(request.getUf(), v -> est.setUf(v.toUpperCase()));
        est.setEndereco(montarEndereco(est.getLogradouro(), est.getNumero(), est.getBairro(), est.getCidade(), est.getUf()));
        recalcularCoordenadas(est);
    }

    private Map<String, Object> consultarViaCep(String cep) {
        try {
            RestTemplate rest = new RestTemplate();
            return rest.getForObject("https://viacep.com.br/ws/{cep}/json/", Map.class, cep);
        } catch (RestClientException e) {
            throw new RequisicaoInvalidaException(MSG_CEP_INVALIDO);
        }
    }

    private void recalcularCoordenadas(Estacionamento est) {
        if (!temTexto(est.getLogradouro()) || !temTexto(est.getCidade()) || !temTexto(est.getNumero())) {
            return;
        }
        try {
            String contato = "gustavon.o.alegria@gmail.com";
            URI uri = UriComponentsBuilder.fromUriString("https://nominatim.openstreetmap.org/search")
                    .queryParam("format", "jsonv2")
                    .queryParam("limit", "1")
                    .queryParam("countrycodes", "br")
                    .queryParam("addressdetails", "0")
                    .queryParam("email", contato)
                    .queryParam("q", String.format("%s, %s, %s, %s, %s, Brasil",
                            est.getLogradouro(), est.getNumero(), est.getBairro(), est.getCidade(), est.getUf()))
                    .build()
                    .encode()
                    .toUri();

            RestTemplate rest = new RestTemplate();
            ResponseEntity<List<Map<String, Object>>> resp = rest.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = resp.getBody();
            if (body == null || body.isEmpty()) {
                return;
            }
            BigDecimal lat = new BigDecimal(String.valueOf(body.get(0).get("lat")));
            BigDecimal lon = new BigDecimal(String.valueOf(body.get(0).get("lon")));
            est.setLatitude(lat);
            est.setLongitude(lon);
        } catch (Exception ignored) {
            // Mantem as coordenadas salvas se o servico externo estiver indisponivel.
        }
    }

    private void excluirDependenciasOpcionais(Integer estacionamentoId) {
        excluirOpcional("Avaliacao", estacionamentoId);
        excluirOpcional("Reserva", estacionamentoId);
        excluirOpcional("PagamentoPlano", estacionamentoId);
        excluirOpcional("EstacionamentoFoto", estacionamentoId);
        excluirOpcional("FotoEstacionamento", estacionamentoId);
        excluirOpcional("PlanoEstacionamento", estacionamentoId);
    }

    private void excluirOpcional(String tabela, Integer estacionamentoId) {
        try {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN OBJECT_ID(?, 'U') IS NULL THEN 0 ELSE 1 END",
                    Integer.class,
                    tabela
            );
            if (existe != null && existe == 1) {
                jdbcTemplate.update("DELETE FROM " + tabela + " WHERE estacionamentoId = ?", estacionamentoId);
            }
        } catch (Exception ignored) {
            // Tabelas opcionais variam entre ambientes; as dependencias mapeadas acima seguem com JPA.
        }
    }

    private AdmDTO.EstacionamentoResponse toResponse(Estacionamento e, VagasEstacionamento v) {
        return AdmDTO.EstacionamentoResponse.builder()
                .id(e.getId())
                .nome(e.getNome())
                .avaliacaoMedia(e.getAvaliacaoMedia())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .endereco(e.getEndereco())
                .precoHora(e.getPrecoHora())
                .horarioAbertura(e.getHorarioAbertura())
                .horarioFechamento(e.getHorarioFechamento())
                .fotoPrincipal(e.getFotoPrincipal())
                .descricao(e.getDescricao())
                .ativo(e.getAtivo())
                .pixKey(e.getPixKey())
                .permiteReserva(e.getPermiteReserva())
                .cep(e.getCep())
                .logradouro(e.getLogradouro())
                .numero(e.getNumero())
                .complemento(e.getComplemento())
                .bairro(e.getBairro())
                .cidade(e.getCidade())
                .uf(e.getUf())
                .qtdVagasTotais(v != null ? v.getQtdVagasTotais() : null)
                .qtdVagasDisponiveis(v != null ? v.getQtdVagasDisponiveis() : null)
                .qtdVagasReservaveis(v != null ? v.getQtdVagasReservaveis() : null)
                .qtdVagasReservadas(v != null ? v.getQtdVagasReservadas() : null)
                .build();
    }

    private VagasEstacionamento buscarVagas(Integer estacionamentoId) {
        return vagasRepository.findByEstacionamentoId(estacionamentoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("Controle de vagas nao encontrado para este estacionamento"));
    }

    private String montarEndereco(String logradouro, String numero, String bairro, String cidade, String uf) {
        String inicio = logradouro == null ? "" : logradouro.trim();
        if (temTexto(numero)) inicio += (inicio.isBlank() ? "" : ", ") + numero.trim();
        String endereco = inicio;
        if (temTexto(bairro)) endereco += (endereco.isBlank() ? "" : " - ") + bairro.trim();
        if (temTexto(cidade)) endereco += (endereco.isBlank() ? "" : ", ") + cidade.trim();
        if (temTexto(uf)) endereco += " - " + uf.trim().toUpperCase();
        return endereco;
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.trim().isBlank();
    }

    private String vazioParaNull(String valor) {
        return temTexto(valor) ? valor.trim() : null;
    }

    private String str(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private String primeiroTexto(String preferido, String fallback) {
        return temTexto(preferido) ? preferido.trim() : str(fallback);
    }

    private boolean ehCidadeSaoPaulo(String cidade) {
        String normalizada = Normalizer.normalize(str(cidade), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        return "sao paulo".equals(normalizada);
    }

    private void copiarSePreenchido(String valor, java.util.function.Consumer<String> setter) {
        if (temTexto(valor)) setter.accept(valor.trim());
    }
}
