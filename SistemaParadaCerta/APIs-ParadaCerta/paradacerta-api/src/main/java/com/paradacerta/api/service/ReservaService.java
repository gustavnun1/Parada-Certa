package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReservaService {
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final SessaoRepository              sessaoRepository;
    private final EstacionamentoRepository      estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final ClienteRepository             clienteRepository;
    private final VeiculoRepository             veiculoRepository;

    // ── Criar reserva ─────────────────────────────────────────────────────────

    @Transactional
    public ReservaResponse criarReserva(ReservaRequest req) {

        Cliente cliente = clienteRepository.findByCpf(req.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        Long clienteId = cliente.getId();

        // Verifica se o estacionamento existe e permite reservas
        Estacionamento est = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!Boolean.TRUE.equals(est.getPermiteReserva())) {
            throw new RequisicaoInvalidaException("Este estacionamento não oferece reservas de vagas");
        }

        VagasEstacionamento vagas = vagasRepository.findByEstacionamentoId(est.getId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Dados de vagas não encontrados para este estacionamento"));

        if (vagas.getQtdVagasReservaveis() <= 0 || vagas.getQtdVagasReservadas() >= vagas.getQtdVagasReservaveis()) {
            throw new RequisicaoInvalidaException("Nenhuma vaga disponível para reserva neste estacionamento");
        }

        if (vagas.getQtdVagasDisponiveis() <= 0) {
            throw new ConflictException("Estacionamento sem vagas disponíveis no momento");
        }

        // Regra 1: não pode ter reserva ativa no mesmo estacionamento
        if (sessaoRepository.existsByClienteIdAndEstacionamentoIdAndReservadoTrueAndStatus(
                clienteId, est.getId(), SessaoStatus.ATIVA)) {
            throw new ConflictException("Você já possui uma reserva ativa neste estacionamento");
        }

        // Regra 2: não pode ter qualquer sessão/reserva ativa em paralelo (períodos sobrepostos)
        if (sessaoRepository.existsByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA)) {
            throw new ConflictException("Você já possui uma sessão ou reserva ativa em outro estacionamento. Encerre-a antes de realizar uma nova reserva");
        }

        // Cria a sessão de reserva
        LocalDateTime agora = nowSaoPaulo();
        String qrCode = java.util.UUID.randomUUID().toString();

        String placa = req.getPlaca() != null ? req.getPlaca().toUpperCase() : null;

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(clienteId);
        sessao.setEstacionamentoId(est.getId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(qrCode);
        sessao.setReservado(true);
        sessao.setValorPago(est.getPrecoHora());
        sessao.setHoraPagamento(agora);
        sessao.setPlaca(placa);

        sessaoRepository.save(sessao);

        // Monta a resposta
        String modeloVeiculo = null;
        if (placa != null) {
            modeloVeiculo = veiculoRepository.findById(placa)
                    .map(Veiculo::getNome)
                    .orElse(null);
        }

        long horaEntradaMs = toEpochMillisSaoPaulo(agora);

        return new ReservaResponse(
                String.valueOf(sessao.getId()),
                est.getId(),
                est.getNome(),
                est.getPixKey(),
                horaEntradaMs,
                est.getPrecoHora(),
                placa,
                modeloVeiculo
        );
    }

    // ── Cancelar reserva (cancelamento pelo motorista via app) ───────────────

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

        BigDecimal valorPago = sessao.getValorPago() != null ? sessao.getValorPago() : BigDecimal.ZERO;
        BigDecimal reembolso = valorPago.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);

        sessao.setStatus(SessaoStatus.CANCELADA);
        sessao.setHoraSaida(nowSaoPaulo());
        // Reserva cancelada não gera receita no painel admin: o valorPago é
        // zerado para que os totais financeiros (dashboard, financeiro, ticket
        // médio, gráficos) não contabilizem cancelamentos. A multa contratual
        // (85% retido junto à plataforma/operação financeira externa) é
        // tratada fora do dashboard do estacionamento.
        sessao.setValorPago(BigDecimal.ZERO);
        sessaoRepository.save(sessao);

        return ApiResponse.ok(
                String.format("Reserva cancelada. Reembolso de R$ %.2f (15%%) processado ao cliente.", reembolso)
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

        // Verifica se há tempo excedente além da 1 hora coberta
        long extraMinutos = calcularExtraMinutos(sessao.getHoraEntrada());
        if (extraMinutos > 15) {
            Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
            BigDecimal valorExtra = calcularValorExtra(extraMinutos, est.getPrecoHora());
            throw new RequisicaoInvalidaException(
                    String.format(
                            "Cobrança adicional de R$ %.2f pelo tempo excedente (%d min além da 1ª hora). " +
                            "Realize o pagamento via POST /api/sessao/encerrar/%d?valorPago=%.2f",
                            valorExtra, extraMinutos, sessaoId, valorExtra)
            );
        }

        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(nowSaoPaulo());
        sessaoRepository.save(sessao);

        return ApiResponse.ok("Reserva finalizada. Boa permanência!");
    }

    // ── Calcular cobrança extra (consulta sem modificar a sessão) ────────────

    @Transactional(readOnly = true)
    public CalculoExtraResponse calcularExtra(Long sessaoId) {

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        long extraMinutos = calcularExtraMinutos(sessao.getHoraEntrada());

        if (extraMinutos <= 15) {
            return new CalculoExtraResponse(false, 0L, BigDecimal.ZERO);
        }

        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
        BigDecimal valorExtra = calcularValorExtra(extraMinutos, est.getPrecoHora());

        return new CalculoExtraResponse(true, extraMinutos, valorExtra);
    }

    // ── Helpers de cálculo de tempo extra ────────────────────────────────────

    /**
     * Retorna os minutos excedentes além de 1 hora desde horaEntrada.
     * Valor negativo indica que ainda está dentro do período coberto.
     */
    private long calcularExtraMinutos(LocalDateTime horaEntrada) {
        long totalMinutos = Duration.between(horaEntrada, nowSaoPaulo()).toMinutes();
        return totalMinutos - 60; // desconta a 1 hora coberta pela reserva
    }

    private LocalDateTime nowSaoPaulo() {
        return LocalDateTime.now(ZONE_SAO_PAULO);
    }

    private long toEpochMillisSaoPaulo(LocalDateTime dataHora) {
        return dataHora.atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli();
    }

    /**
     * Calcula o valor extra a cobrar: horas excedentes arredondadas para cima × precoHora.
     * Exemplo: 75 min extra → ceil(75/60) = 2 h → 2 × precoHora.
     */
    private BigDecimal calcularValorExtra(long extraMinutos, BigDecimal precoHora) {
        long horasExtras = (long) Math.ceil(extraMinutos / 60.0);
        return precoHora.multiply(BigDecimal.valueOf(horasExtras))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
