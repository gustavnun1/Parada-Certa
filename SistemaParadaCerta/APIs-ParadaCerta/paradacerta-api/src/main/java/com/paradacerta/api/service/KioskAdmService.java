package com.paradacerta.api.service;

import com.paradacerta.api.dto.KioskAdmDTO;
import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.OperadorEstacionamento;
import com.paradacerta.api.model.SessaoEstacionamento;
import com.paradacerta.api.model.SessaoStatus;
import com.paradacerta.api.model.VagasEstacionamento;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KioskAdmService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final OperadorEstacionamentoRepository operadorRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final SessaoRepository sessaoRepository;

    public KioskAdmDTO.LoginResponse login(KioskAdmDTO.LoginRequest request) {
        if (request == null
                || request.getEstacionamentoId() == null
                || request.getUsuario() == null
                || request.getSenha() == null) {
            throw new CredenciaisInvalidasException("Usuario ou senha invalidos");
        }

        OperadorEstacionamento operador = operadorRepository
                .findByEstacionamentoIdAndUsuarioAndAtivoTrue(
                        request.getEstacionamentoId(),
                        request.getUsuario()
                )
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuario ou senha invalidos"));

        if (!BCrypt.checkpw(request.getSenha(), operador.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuario ou senha invalidos");
        }

        Estacionamento estacionamento = buscarEstacionamento(operador.getEstacionamentoId());
        VagasEstacionamento vagas = buscarVagas(estacionamento.getId());

        return KioskAdmDTO.LoginResponse.builder()
                .admId(operador.getId())
                .nomeCompleto(operador.getNome())
                .estacionamentoId(estacionamento.getId())
                .nomeEstacionamento(estacionamento.getNome())
                .vagasDisponiveis(vagas.getQtdVagasDisponiveis())
                .vagasTotais(vagas.getQtdVagasTotais())
                .build();
    }

    @Transactional
    public KioskAdmDTO.GerarQrCodeResponse gerarQrCode(KioskAdmDTO.GerarQrCodeRequest request) {
        if (request == null || request.getAdmId() == null || request.getEstacionamentoId() == null) {
            throw new RequisicaoInvalidaException("Dados insuficientes para gerar QR Code");
        }

        OperadorEstacionamento operador = operadorRepository.findById(request.getAdmId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Operador nao encontrado"));
        if (Boolean.FALSE.equals(operador.getAtivo())) {
            throw new CredenciaisInvalidasException("Operador desativado");
        }
        if (!request.getEstacionamentoId().equals(operador.getEstacionamentoId())) {
            throw new RequisicaoInvalidaException("Operador nao pertence a este estacionamento");
        }

        Estacionamento estacionamento = buscarEstacionamento(request.getEstacionamentoId());
        VagasEstacionamento vagas = buscarVagas(estacionamento.getId());
        if (vagas.getQtdVagasDisponiveis() == null || vagas.getQtdVagasDisponiveis() <= 0) {
            throw new RequisicaoInvalidaException("Estacionamento lotado - nenhuma vaga disponivel");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime agora = LocalDateTime.now(ZONE_SAO_PAULO);
        LocalDateTime expiracao = agora.plusMinutes(30);

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(null);
        sessao.setEstacionamentoId(estacionamento.getId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(token);
        sessao.setReservado(false);
        sessao.setValorPago(estacionamento.getPrecoHora() != null ? estacionamento.getPrecoHora() : BigDecimal.ZERO);
        sessaoRepository.save(sessao);

        String payload = String.format(
                "{\"v\":1,\"app\":\"paradacerta\",\"type\":\"entrada\",\"estacionamentoId\":%d,\"token\":\"%s\"}",
                estacionamento.getId(), token
        );

        return KioskAdmDTO.GerarQrCodeResponse.builder()
                .token(token)
                .estacionamentoId(estacionamento.getId())
                .nomeEstacionamento(estacionamento.getNome())
                .geradoEm(agora.format(FORMATTER))
                .expiradoEm(expiracao.format(FORMATTER))
                .qrCodePayload(payload)
                .build();
    }

    public KioskAdmDTO.StatusResponse buscarStatus(Integer estacionamentoId) {
        Estacionamento estacionamento = buscarEstacionamento(estacionamentoId);
        VagasEstacionamento vagas = buscarVagas(estacionamento.getId());
        long sessoesAtivas = sessaoRepository.countByEstacionamentoIdAndStatus(estacionamento.getId(), SessaoStatus.ATIVA);

        return KioskAdmDTO.StatusResponse.builder()
                .vagasDisponiveis(vagas.getQtdVagasDisponiveis())
                .vagasTotais(vagas.getQtdVagasTotais())
                .sessoesAtivas(sessoesAtivas)
                .build();
    }

    private Estacionamento buscarEstacionamento(Integer estacionamentoId) {
        return estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento nao encontrado"));
    }

    private VagasEstacionamento buscarVagas(Integer estacionamentoId) {
        return vagasRepository.findByEstacionamentoId(estacionamentoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("Controle de vagas nao encontrado para este estacionamento"));
    }
}
