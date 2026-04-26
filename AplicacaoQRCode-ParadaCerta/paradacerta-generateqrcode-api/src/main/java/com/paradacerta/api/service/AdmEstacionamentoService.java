package com.paradacerta.api.service;

import com.paradacerta.api.dto.AdmDTO;
import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.AdmEstacionamento;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.QrCodeEntrada;
import com.paradacerta.api.repository.AdmEstacionamentoRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.QrCodeEntradaRepository;
import com.paradacerta.api.repository.SessaoEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdmEstacionamentoService {

    private final AdmEstacionamentoRepository admRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final QrCodeEntradaRepository qrCodeRepository;
    private final SessaoEstacionamentoRepository sessaoRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public AdmDTO.LoginResponse login(AdmDTO.LoginRequest request) {
        AdmEstacionamento adm = admRepository.findByUsuarioAndAtivoTrue(request.getUsuario())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos"));

        if (!BCrypt.checkpw(request.getSenha(), adm.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }

        Estacionamento est = estacionamentoRepository.findById(adm.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        return AdmDTO.LoginResponse.builder()
                .admId(adm.getId())
                .nomeCompleto(adm.getNomeCompleto())
                .estacionamentoId(est.getId())
                .nomeEstacionamento(est.getNome())
                .vagasDisponiveis(est.getQtdVagasDisponiveis())
                .vagasTotais(est.getQtdVagasTotais())
                .build();
    }

    @Transactional
    public AdmDTO.GerarQrCodeResponse gerarQrCode(AdmDTO.GerarQrCodeRequest request) {
        AdmEstacionamento adm = admRepository.findById(request.getAdmId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Administrador não encontrado"));

        if (!adm.getEstacionamentoId().equals(request.getEstacionamentoId())) {
            throw new RequisicaoInvalidaException("Administrador não pertence a este estacionamento");
        }

        Estacionamento est = estacionamentoRepository.findById(request.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        if (est.getQtdVagasDisponiveis() <= 0) {
            throw new RequisicaoInvalidaException("Estacionamento lotado — nenhuma vaga disponível");
        }

        qrCodeRepository.invalidarTokensDisponiveis(request.getEstacionamentoId());

        String token = UUID.randomUUID().toString();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime expiracao = agora.plusMinutes(30);

        QrCodeEntrada qrCode = QrCodeEntrada.builder()
                .token(token)
                .estacionamentoId(request.getEstacionamentoId())
                .geradoPor(request.getAdmId())
                .geradoEm(agora)
                .expiradoEm(expiracao)
                .status("DISPONIVEL")
                .build();

        qrCodeRepository.save(qrCode);

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
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        long sessoesAtivas = sessaoRepository.countByEstacionamentoIdAndStatus(
                estacionamentoId, "ATIVA");

        return AdmDTO.StatusResponse.builder()
                .vagasDisponiveis(est.getQtdVagasDisponiveis())
                .vagasTotais(est.getQtdVagasTotais())
                .sessoesAtivas(sessoesAtivas)
                .build();
    }
}
