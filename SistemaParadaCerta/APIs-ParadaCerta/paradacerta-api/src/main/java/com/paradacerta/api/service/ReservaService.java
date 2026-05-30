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
import java.time.LocalTime;
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
    //
    // A reserva é criada com status AGUARDANDO_CONFIRMACAO. O motorista pagou a
    // 1ª hora antecipada e tem direito a cancelar (com reembolso de 15%) até
    // confirmar presencialmente o QR Code de confirmação fornecido pelo
    // estacionamento. Quando o motorista escaneia o QR, a sessão vira EM_USO e
    // só pode ser encerrada via finalizarUso.

    @Transactional
    public ReservaResponse criarReserva(ReservaRequest req) {

        Cliente cliente = clienteRepository.findByCpf(req.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        Long clienteId = cliente.getId();

        Estacionamento est = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!Boolean.TRUE.equals(est.getPermiteReserva())) {
            throw new RequisicaoInvalidaException("Este estacionamento não oferece reservas de vagas");
        }

        validarInicioReserva(req.getInicioReservaPrevisto(), est);

        VagasEstacionamento vagas = vagasRepository.findByEstacionamentoId(est.getId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Dados de vagas não encontrados para este estacionamento"));

        if (vagas.getQtdVagasReservaveis() <= 0 || vagas.getQtdVagasReservadas() >= vagas.getQtdVagasReservaveis()) {
            throw new RequisicaoInvalidaException("Nenhuma vaga disponível para reserva neste estacionamento");
        }

        if (vagas.getQtdVagasDisponiveis() <= 0) {
            throw new ConflictException("Estacionamento sem vagas disponíveis no momento");
        }

        // Trava global: motorista não pode ter NENHUMA sessão "viva" em paralelo
        // (cobre AGUARDANDO_CONFIRMACAO, EM_USO e ATIVA).
        if (sessaoRepository.existsSessaoVivaDoCliente(clienteId)) {
            throw new ConflictException(
                "Você já possui uma reserva em andamento ou está usando uma vaga. "
              + "Finalize ou cancele a sessão atual antes de iniciar uma nova reserva."
            );
        }

        LocalDateTime agora = nowSaoPaulo();
        String qrCode = java.util.UUID.randomUUID().toString();

        String placa = req.getPlaca() != null ? req.getPlaca().toUpperCase() : null;

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(clienteId);
        sessao.setEstacionamentoId(est.getId());
        sessao.setHoraEntrada(agora);
        sessao.setInicioReservaPrevisto(req.getInicioReservaPrevisto());
        sessao.setStatus(SessaoStatus.AGUARDANDO_CONFIRMACAO);
        sessao.setQrCode(qrCode);
        sessao.setReservado(true);
        sessao.setValorPago(est.getPrecoHora()); // valor pago antecipado (1ª hora)
        sessao.setHoraPagamento(agora);
        sessao.setPlaca(placa);

        sessaoRepository.save(sessao);

        String modeloVeiculo = null;
        if (placa != null) {
            modeloVeiculo = veiculoRepository.findById(placa)
                    .map(Veiculo::getNome)
                    .orElse(null);
        }

        long horaEntradaMs = toEpochMillisSaoPaulo(agora);
        long inicioReservaPrevistoMs = toEpochMillisSaoPaulo(req.getInicioReservaPrevisto());

        return new ReservaResponse(
                String.valueOf(sessao.getId()),
                est.getId(),
                est.getNome(),
                est.getPixKey(),
                horaEntradaMs,
                inicioReservaPrevistoMs,
                est.getPrecoHora(),
                placa,
                modeloVeiculo
        );
    }

    // ── Cancelar reserva (cancelamento pelo motorista via app) ───────────────
    //
    // Só é permitido enquanto a reserva está AGUARDANDO_CONFIRMACAO. Depois
    // que o motorista confirma presencialmente (status = EM_USO), não há mais
    // direito de cancelamento — somente finalizar uso.

    @Transactional
    public ApiResponse cancelarReserva(Long sessaoId) {

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (sessao.getStatus() == SessaoStatus.EM_USO) {
            throw new ConflictException(
                "Reserva já confirmada no estacionamento. Use 'Finalizar uso da vaga' para encerrar."
            );
        }

        // Permitido em AGUARDANDO_CONFIRMACAO e ATIVA (compat legado pré-migração)
        if (sessao.getStatus() != SessaoStatus.AGUARDANDO_CONFIRMACAO
                && sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        BigDecimal valorPago = sessao.getValorPago() != null ? sessao.getValorPago() : BigDecimal.ZERO;
        BigDecimal reembolso = valorPago.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);

        sessao.setStatus(SessaoStatus.CANCELADA);
        sessao.setHoraSaida(nowSaoPaulo());
        // Reserva cancelada não gera receita no painel admin
        sessao.setValorPago(BigDecimal.ZERO);
        sessaoRepository.save(sessao);

        return ApiResponse.ok(
                String.format("Reserva cancelada. Reembolso de R$ %.2f (15%%) processado ao cliente.", reembolso)
        );
    }

    // ── Confirmar reserva por QR Code (motorista escaneia no kiosk/admin) ────
    //
    // Valida que o QR pertence a uma reserva AGUARDANDO_CONFIRMACAO do próprio
    // motorista. Marca dataHoraConfirmacao = agora e muda status para EM_USO.
    // A partir daí, cancelamento pelo motorista é bloqueado e o cronômetro
    // de uso começa a contar.

    @Transactional
    public ConfirmacaoReservaResponse confirmarReservaPorQrCode(String qrCode, String cpf) {

        if (qrCode == null || qrCode.isBlank()) {
            throw new RequisicaoInvalidaException("QR Code de confirmação não informado");
        }

        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        SessaoEstacionamento sessao = sessaoRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(
                        "QR Code não corresponde a nenhuma reserva"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException(
                "QR Code inválido para confirmação de reserva"
            );
        }

        if (sessao.getClienteId() == null || !sessao.getClienteId().equals(cliente.getId())) {
            throw new ConflictException("Esta reserva pertence a outro motorista");
        }

        if (sessao.getStatus() == SessaoStatus.EM_USO) {
            throw new ConflictException("Reserva já confirmada");
        }

        if (sessao.getStatus() != SessaoStatus.AGUARDANDO_CONFIRMACAO) {
            throw new ConflictException("Reserva não está aguardando confirmação");
        }

        LocalDateTime agora = nowSaoPaulo();
        sessao.setStatus(SessaoStatus.EM_USO);
        sessao.setDataHoraConfirmacao(agora);
        sessaoRepository.save(sessao);

        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        return new ConfirmacaoReservaResponse(
                String.valueOf(sessao.getId()),
                est.getId(),
                est.getNome(),
                est.getPixKey(),
                toEpochMillisSaoPaulo(agora),
                est.getPrecoHora()
        );
    }

    // ── Finalizar uso da vaga (motorista sai do estacionamento) ──────────────
    //
    // Calcula o tempo total de uso desde dataHoraConfirmacao até agora aplicando
    // as regras: mínimo 1h, arredondamento por blocos de 30min acima de 1h. Se o
    // valor recalculado for maior que valorPago (antecipado), retorna a
    // diferença para o mobile cobrar via Pix antes de efetivar o encerramento.
    // Se for menor ou igual, encerra direto sem cobrança adicional.

    @Transactional(readOnly = true)
    public FinalizacaoUsoResponse calcularFinalizacaoUso(Long sessaoId, String cpf) {
        SessaoEstacionamento sessao = carregarReservaEmUso(sessaoId, cpf);
        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        return montarFinalizacao(sessao, est, nowSaoPaulo());
    }

    @Transactional
    public FinalizacaoUsoResponse finalizarUso(Long sessaoId, String cpf, BigDecimal valorPagoAdicional) {

        SessaoEstacionamento sessao = carregarReservaEmUso(sessaoId, cpf);
        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        LocalDateTime agora = nowSaoPaulo();
        FinalizacaoUsoResponse calculo = montarFinalizacao(sessao, est, agora);

        // Se há saldo a pagar, exige que o mobile envie o valor (após confirmar Pix)
        if (calculo.isExigeCobrancaAdicional()) {
            BigDecimal restante = calculo.getValorRestante();
            BigDecimal pago = valorPagoAdicional != null ? valorPagoAdicional : BigDecimal.ZERO;
            if (pago.compareTo(restante) < 0) {
                throw new RequisicaoInvalidaException(
                    String.format(
                        "Cobrança adicional de R$ %.2f pendente. Realize o pagamento antes de finalizar.",
                        restante
                    )
                );
            }
        }

        BigDecimal valorAntecipado = sessao.getValorPago() != null ? sessao.getValorPago() : BigDecimal.ZERO;
        BigDecimal restanteCobrado = calculo.isExigeCobrancaAdicional()
                ? calculo.getValorRestante()
                : BigDecimal.ZERO;
        BigDecimal valorTotalPago = valorAntecipado.add(restanteCobrado);

        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(agora);
        sessao.setHoraPagamento(agora);
        sessao.setValorFinalCalculado(calculo.getValorFinalCalculado());
        sessao.setValorRestanteCobrado(restanteCobrado);
        sessao.setValorPago(valorTotalPago);
        sessaoRepository.save(sessao);

        return calculo;
    }

    private FinalizacaoUsoResponse montarFinalizacao(
            SessaoEstacionamento sessao, Estacionamento est, LocalDateTime fim) {

        LocalDateTime inicio = inicioDeUsoParaCalculo(sessao);
        long minutosUso = Math.max(0L, Duration.between(inicio, fim).toMinutes());
        long minutosCobrados = arredondarMinutosCobrados(minutosUso);
        BigDecimal valorFinal = calcularValorPorMinutos(est.getPrecoHora(), minutosCobrados);

        BigDecimal valorAntecipado = sessao.getValorPago() != null ? sessao.getValorPago() : BigDecimal.ZERO;
        BigDecimal restante = valorFinal.subtract(valorAntecipado).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        boolean exigeCobranca = restante.compareTo(BigDecimal.ZERO) > 0;

        return new FinalizacaoUsoResponse(
                String.valueOf(sessao.getId()),
                est.getId(),
                est.getNome(),
                est.getPixKey(),
                minutosUso,
                minutosCobrados,
                est.getPrecoHora(),
                valorAntecipado,
                valorFinal,
                restante,
                exigeCobranca
        );
    }

    private SessaoEstacionamento carregarReservaEmUso(Long sessaoId, String cpf) {
        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (sessao.getStatus() == SessaoStatus.AGUARDANDO_CONFIRMACAO) {
            throw new ConflictException(
                "Reserva ainda não foi confirmada no estacionamento. Escaneie o QR de confirmação primeiro."
            );
        }

        // Aceita EM_USO (fluxo novo) e ATIVA (reservas legado pré-migração)
        if (sessao.getStatus() != SessaoStatus.EM_USO
                && sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        if (cpf != null && !cpf.isBlank()) {
            Cliente cliente = clienteRepository.findByCpf(cpf)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
            if (sessao.getClienteId() == null || !sessao.getClienteId().equals(cliente.getId())) {
                throw new ConflictException("Esta reserva pertence a outro motorista");
            }
        }

        return sessao;
    }

    /**
     * Para o fluxo novo (EM_USO), conta a partir de dataHoraConfirmacao.
     * Para reservas legado (ATIVA) que vieram da versão anterior sem o campo,
     * usa inicioReservaPrevisto ou horaEntrada como fallback.
     */
    private LocalDateTime inicioDeUsoParaCalculo(SessaoEstacionamento sessao) {
        if (sessao.getDataHoraConfirmacao() != null) {
            return sessao.getDataHoraConfirmacao();
        }
        if (sessao.getInicioReservaPrevisto() != null) {
            return sessao.getInicioReservaPrevisto();
        }
        return sessao.getHoraEntrada();
    }

    // ── Compatibilidade: endpoints antigos ───────────────────────────────────

    /**
     * Compat: o app antigo chamava /api/reserva/{id}/finalizar para a operação
     * que hoje virou "confirmar no estacionamento". Mantemos o método como um
     * alias de finalizarUso sem cobrança adicional. Builds antigos do mobile
     * só conseguirão finalizar sem extra (que era o comportamento anterior).
     */
    @Transactional
    public ApiResponse finalizarReserva(Long sessaoId) {
        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        // Se já está EM_USO, finaliza sem cobrança adicional
        if (sessao.getStatus() == SessaoStatus.EM_USO) {
            finalizarUso(sessaoId, null, BigDecimal.ZERO);
            return ApiResponse.ok("Reserva finalizada. Boa permanência!");
        }

        // Compat legado pré-migração: encerra direto
        if (sessao.getStatus() == SessaoStatus.ATIVA
                && Boolean.TRUE.equals(sessao.getReservado())) {
            sessao.setStatus(SessaoStatus.ENCERRADA);
            sessao.setHoraSaida(nowSaoPaulo());
            sessaoRepository.save(sessao);
            return ApiResponse.ok("Reserva finalizada. Boa permanência!");
        }

        throw new ConflictException("Reserva não está em uso");
    }

    @Transactional(readOnly = true)
    public CalculoExtraResponse calcularExtra(Long sessaoId) {

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }

        if (!sessao.getStatus().isAtivoOuAguardando()) {
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        LocalDateTime inicio = inicioDeUsoParaCalculo(sessao);
        long extraMinutos = Duration.between(inicio, nowSaoPaulo()).toMinutes() - 60;

        if (extraMinutos <= 15) {
            return new CalculoExtraResponse(false, 0L, BigDecimal.ZERO);
        }

        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
        BigDecimal valorExtra = calcularValorExtra(extraMinutos, est.getPrecoHora());

        return new CalculoExtraResponse(true, extraMinutos, valorExtra);
    }

    // ── Helpers de cálculo de tempo / valor ──────────────────────────────────

    private long arredondarMinutosCobrados(long minutos) {
        if (minutos <= 60) return 60;
        long blocos = (minutos + 29) / 30; // ceil(minutos / 30)
        return blocos * 30;
    }

    private BigDecimal calcularValorPorMinutos(BigDecimal precoHora, long minutosCobrados) {
        if (precoHora == null) return BigDecimal.ZERO;
        return precoHora
                .multiply(BigDecimal.valueOf(minutosCobrados))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private void validarInicioReserva(LocalDateTime inicioReservaPrevisto, Estacionamento est) {
        if (inicioReservaPrevisto == null) {
            throw new RequisicaoInvalidaException("Horário inicial da reserva é obrigatório");
        }

        if (inicioReservaPrevisto.isBefore(nowSaoPaulo())) {
            throw new RequisicaoInvalidaException("Horário da reserva não pode estar no passado");
        }

        LocalTime abertura = est.getHorarioAbertura();
        LocalTime fechamento = est.getHorarioFechamento();
        if (abertura == null || fechamento == null) {
            throw new RequisicaoInvalidaException("Horário de funcionamento não cadastrado para este estacionamento");
        }

        LocalTime horario = inicioReservaPrevisto.toLocalTime();
        boolean dentroFuncionamento = !abertura.isAfter(fechamento)
                ? !horario.isBefore(abertura) && horario.isBefore(fechamento)
                : !horario.isBefore(abertura) || horario.isBefore(fechamento);

        if (!dentroFuncionamento) {
            throw new RequisicaoInvalidaException("Horário da reserva fora do funcionamento do estacionamento");
        }
    }

    private LocalDateTime nowSaoPaulo() {
        return LocalDateTime.now(ZONE_SAO_PAULO);
    }

    private long toEpochMillisSaoPaulo(LocalDateTime dataHora) {
        return dataHora.atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli();
    }

    private BigDecimal calcularValorExtra(long extraMinutos, BigDecimal precoHora) {
        long horasExtras = (long) Math.ceil(extraMinutos / 60.0);
        return precoHora.multiply(BigDecimal.valueOf(horasExtras))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
