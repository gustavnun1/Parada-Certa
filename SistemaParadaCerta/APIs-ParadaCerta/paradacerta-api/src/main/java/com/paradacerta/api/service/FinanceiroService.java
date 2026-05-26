package com.paradacerta.api.service;

import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cálculos financeiros do painel administrativo, baseados em SessaoEstacionamento.
 * Taxa da plataforma é configurada via paradacerta.taxa-plataforma (padrão 0.05).
 */
@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final SessaoRepository           sessaoRepository;
    private final EstacionamentoRepository   estacionamentoRepository;
    private final ClienteRepository          clienteRepository;

    @Value("${paradacerta.taxa-plataforma:0.05}")
    private BigDecimal taxaPlataforma;

    // ── Listagem de pagamentos ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FinanceiroPagamentoResponse> listarPagamentos(Integer estacionamentoId, String periodo) {
        if (!estacionamentoRepository.existsById(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        List<SessaoEstacionamento> sessoes;
        if (periodo == null || "todos".equalsIgnoreCase(periodo)) {
            sessoes = sessaoRepository.findPagamentosPagosTodos(estacionamentoId);
        } else {
            LocalDateTime[] intervalo = intervaloDe(periodo);
            sessoes = sessaoRepository.findPagamentosPagosPeriodo(estacionamentoId, intervalo[0], intervalo[1]);
        }

        if (sessoes.isEmpty()) return List.of();

        List<Long> clienteIds = sessoes.stream()
                .map(SessaoEstacionamento::getClienteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Cliente> clientesPorId = clienteRepository.findAllById(clienteIds).stream()
                .collect(Collectors.toMap(Cliente::getId, c -> c));

        return sessoes.stream().map(s -> {
            BigDecimal bruto    = s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago();
            BigDecimal taxa     = bruto.multiply(taxaPlataforma).setScale(2, RoundingMode.HALF_UP);
            BigDecimal liquido  = bruto.subtract(taxa).setScale(2, RoundingMode.HALF_UP);
            Cliente c = clientesPorId.get(s.getClienteId());
            // Queries só trazem sessões ENCERRADAS; canceladas não geram pagamento.
            String statusPagamento = "PAGO";

            return new FinanceiroPagamentoResponse(
                    s.getId(),
                    s.getClienteId(),
                    c != null ? c.getNome() : (s.getClienteId() == null ? "Cliente kiosk" : "Cliente #" + s.getClienteId()),
                    s.getPlaca(),
                    s.getHoraPagamento(),
                    s.getHoraEntrada(),
                    s.getHoraSaida(),
                    bruto,
                    taxa,
                    liquido,
                    statusPagamento,
                    s.getReservado(),
                    "QR Code"
            );
        }).toList();
    }

    // ── Resumo financeiro ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinanceiroResumoResponse resumo(Integer estacionamentoId, String periodo) {
        if (!estacionamentoRepository.existsById(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        String periodoEfetivo = (periodo == null || periodo.isBlank()) ? "todos" : periodo;
        List<SessaoEstacionamento> pagos;
        if ("todos".equalsIgnoreCase(periodoEfetivo)) {
            pagos = sessaoRepository.findPagamentosPagosTodos(estacionamentoId);
        } else {
            LocalDateTime[] intervalo = intervaloDe(periodoEfetivo);
            pagos = sessaoRepository.findPagamentosPagosPeriodo(estacionamentoId, intervalo[0], intervalo[1]);
        }

        BigDecimal totalRecebido = pagos.stream()
                .map(s -> s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalTaxas = totalRecebido.multiply(taxaPlataforma).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLiquido = totalRecebido.subtract(totalTaxas).setScale(2, RoundingMode.HALF_UP);

        BigDecimal ticketMedio = pagos.isEmpty()
                ? BigDecimal.ZERO
                : totalRecebido.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);

        BigDecimal maiorPagamento = pagos.stream()
                .map(s -> s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // Pagamentos hoje (sempre, independente do filtro acima)
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje    = LocalDate.now().atTime(LocalTime.MAX);
        long pagamentosHoje = sessaoRepository.countByEstacionamentoStatusAndPagamentoPeriodo(
                estacionamentoId, SessaoStatus.ENCERRADA, inicioHoje, fimHoje);

        long pendentes = sessaoRepository.countByEstacionamentoIdAndStatus(estacionamentoId, SessaoStatus.ATIVA);

        return new FinanceiroResumoResponse(
                totalRecebido,
                totalTaxas,
                totalLiquido,
                pagamentosHoje,
                ticketMedio,
                maiorPagamento,
                pendentes,
                taxaPlataforma,
                periodoEfetivo.toLowerCase()
        );
    }

    // ── Período → intervalo de datas ────────────────────────────────────────

    private LocalDateTime[] intervaloDe(String periodo) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio;
        LocalDateTime fim = hoje.atTime(LocalTime.MAX);

        switch (periodo.toLowerCase()) {
            case "hoje" -> inicio = hoje.atStartOfDay();
            case "semana" -> inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
            case "mes" -> inicio = hoje.withDayOfMonth(1).atStartOfDay();
            case "ano" -> inicio = hoje.withDayOfYear(1).atStartOfDay();
            default -> inicio = LocalDate.of(2000, 1, 1).atStartOfDay();
        }
        return new LocalDateTime[]{inicio, fim};
    }
}
