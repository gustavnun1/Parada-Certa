package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final SessaoRepository        sessaoRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final ClienteRepository        clienteRepository;
    private final VeiculoRepository        veiculoRepository;

    // ── Criar reserva ─────────────────────────────────────────────────────────

    @Transactional
    public ReservaResponse criarReserva(ReservaRequest req) {

        // Verifica se o cliente existe e é premium
        Cliente cliente = clienteRepository.findByCpf(req.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        if (!cliente.isPremium()) {
            throw new RequisicaoInvalidaException("Reserva de vagas é exclusiva para assinantes Premium");
        }

        Long clienteId = cliente.getId();

        // Verifica se o estacionamento existe e permite reservas
        Estacionamento est = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!Boolean.TRUE.equals(est.getPermiteReserva())) {
            throw new RequisicaoInvalidaException("Este estacionamento não oferece reservas de vagas");
        }

        if (est.getQtdVagasReservaveis() <= 0) {
            throw new RequisicaoInvalidaException("Nenhuma vaga disponível para reserva neste estacionamento");
        }

        if (est.getQtdVagasDisponiveis() <= 0) {
            throw new ConflictException("Estacionamento sem vagas disponíveis no momento");
        }

        // Verifica se o usuário já tem sessão ativa (inclusive reserva pendente)
        if (sessaoRepository.existsByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA)) {
            throw new ConflictException("Você já possui uma sessão ou reserva ativa");
        }

        // Cria a sessão de reserva
        LocalDateTime agora = LocalDateTime.now();
        String qrCode = java.util.UUID.randomUUID().toString();

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(clienteId);
        sessao.setEstacionamentoId(est.getId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(qrCode);
        sessao.setReservado(true);
        sessao.setValorPago(est.getPrecoHora());
        sessao.setHoraPagamento(agora);

        sessaoRepository.save(sessao);

        // Monta a resposta
        String modeloVeiculo = null;
        if (cliente.getPlaca() != null) {
            modeloVeiculo = veiculoRepository.findById(cliente.getPlaca())
                    .map(Veiculo::getNome)
                    .orElse(null);
        }

        long horaEntradaMs = agora
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return new ReservaResponse(
                String.valueOf(sessao.getId()),
                est.getId(),
                est.getNome(),
                est.getPixKey(),
                horaEntradaMs,
                est.getPrecoHora(),
                cliente.getPlaca(),
                modeloVeiculo
        );
    }

    // ── Cancelar reserva (reembolso de 90%) ──────────────────────────────────

    @Transactional
    public ApiResponse cancelarReserva(Long sessaoId) {

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        // Calcula reembolso de 90%
        BigDecimal valorPago   = sessao.getValorPago() != null ? sessao.getValorPago() : BigDecimal.ZERO;
        BigDecimal reembolso   = valorPago.multiply(new BigDecimal("0.9")).setScale(2, RoundingMode.HALF_UP);

        sessao.setStatus(SessaoStatus.CANCELADA);
        sessao.setHoraSaida(LocalDateTime.now());
        sessaoRepository.save(sessao);

        return ApiResponse.ok(
                String.format("Reserva cancelada. Reembolso de R$ %.2f processado.", reembolso)
        );
    }

    // ── Finalizar reserva (usuário chegou ao estacionamento) ─────────────────

    @Transactional
    public ApiResponse finalizarReserva(Long sessaoId) {

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(LocalDateTime.now());
        sessaoRepository.save(sessao);

        return ApiResponse.ok("Reserva finalizada. Boa permanência!");
    }
}
