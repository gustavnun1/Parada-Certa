package com.paradacerta.api.service;

import com.paradacerta.api.exception.AcessoNegadoException;
import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.AdmEstacionamentoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lógica do painel operacional do administrador (aba Reservas / Operação).
 * Trabalha sempre por estacionamentoId (admin só vê o próprio).
 */
@Service
@RequiredArgsConstructor
public class OperacaoService {
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final SessaoRepository              sessaoRepository;
    private final EstacionamentoRepository      estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final ClienteRepository             clienteRepository;
    private final VeiculoRepository             veiculoRepository;
    private final AdmEstacionamentoRepository   admRepository;
    private final PlanoService                  planoService;
    private final EmailService                  emailService;

    @Value("${paradacerta.taxa-plataforma:0.05}")
    private BigDecimal taxaPlataforma;

    // ── Listagem de sessões ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SessaoAdminResponse> listarSessoes(
            Integer estacionamentoId,
            String statusFiltro,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (!estacionamentoRepository.existsById(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        List<SessaoEstacionamento> sessoes;

        boolean temPeriodo = dataInicio != null && dataFim != null;
        boolean temStatus  = statusFiltro != null && !statusFiltro.isBlank() && !"todos".equalsIgnoreCase(statusFiltro);

        if (temPeriodo && temStatus) {
            SessaoStatus status = parseStatus(statusFiltro);
            sessoes = sessaoRepository.findByEstacionamentoStatusPeriodo(
                    estacionamentoId, status,
                    dataInicio.atStartOfDay(), dataFim.atTime(LocalTime.MAX));
        } else if (temPeriodo) {
            sessoes = sessaoRepository.findByEstacionamentoIdAndPeriodo(
                    estacionamentoId,
                    dataInicio.atStartOfDay(), dataFim.atTime(LocalTime.MAX));
        } else if (temStatus) {
            sessoes = sessaoRepository.findByEstacionamentoIdAndStatusOrderByHoraEntradaDesc(
                    estacionamentoId, parseStatus(statusFiltro));
        } else {
            sessoes = sessaoRepository.findByEstacionamentoIdOrderByHoraEntradaDesc(estacionamentoId);
        }

        return hidratar(sessoes);
    }

    // ── Resumo do dashboard operacional ──────────────────────────────────────

    @Transactional(readOnly = true)
    public OperacaoResumoResponse resumo(Integer estacionamentoId) {
        if (!estacionamentoRepository.existsById(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        LocalDate hoje = hojeSaoPaulo();
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje    = hoje.atTime(LocalTime.MAX);

        // "Ativas" no painel = sessões em curso fisicamente no estacionamento.
        // Inclui entrada comum (ATIVA) e reserva confirmada presencialmente (EM_USO).
        // AGUARDANDO_CONFIRMACAO entra em "reservasAtivasCount" abaixo.
        long ativas       =
                sessaoRepository.countByEstacionamentoIdAndStatus(estacionamentoId, SessaoStatus.ATIVA)
              + sessaoRepository.countByEstacionamentoIdAndStatus(estacionamentoId, SessaoStatus.EM_USO);
        long encerradas   = sessaoRepository.countByEstacionamentoStatusAndPagamentoPeriodo(
                estacionamentoId, SessaoStatus.ENCERRADA, inicioHoje, fimHoje);
        long canceladas   = countCanceladasHoje(estacionamentoId, inicioHoje, fimHoje);

        // Reservas ativas (subconjunto das ativas)
        List<SessaoEstacionamento> reservasAtivas = sessaoRepository.findReservasAtivas(estacionamentoId);
        long reservasAtivasCount = reservasAtivas.size();
        BigDecimal receitaPrevista = reservasAtivas.stream()
                .map(s -> s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Receita recebida hoje
        List<SessaoEstacionamento> pagosHoje = sessaoRepository.findPagamentosPagosPeriodo(
                estacionamentoId, inicioHoje, fimHoje);
        BigDecimal receitaRecebidaHoje = pagosHoje.stream()
                .map(s -> s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Optional<VagasEstacionamento> vagasOpt = vagasRepository.findByEstacionamentoId(estacionamentoId);
        Integer total       = vagasOpt.map(VagasEstacionamento::getQtdVagasTotais).orElse(0);
        Integer disponiveis = vagasOpt.map(VagasEstacionamento::getQtdVagasDisponiveis).orElse(0);
        Integer reservadas  = vagasOpt.map(VagasEstacionamento::getQtdVagasReservadas).orElse(0);
        Integer reservaveis = vagasOpt.map(VagasEstacionamento::getQtdVagasReservaveis).orElse(0);

        return new OperacaoResumoResponse(
                ativas,
                reservasAtivasCount,
                encerradas,
                canceladas,
                receitaPrevista,
                receitaRecebidaHoje,
                total, disponiveis, reservadas, reservaveis
        );
    }

    private long countCanceladasHoje(Integer estId, LocalDateTime inicio, LocalDateTime fim) {
        // canceladas usam horaSaida como momento do cancelamento (ReservaService.cancelarReserva seta horaSaida)
        return sessaoRepository.findByEstacionamentoStatusPeriodo(estId, SessaoStatus.CANCELADA, inicio, fim).size();
    }

    // ── Dashboard analítico ──────────────────────────────────────────────────

    /**
     * Indicadores agregados para o dashboard administrativo.
     * Suporta os períodos: hoje, semana (semana atual seg-dom), mês (mês corrente),
     * ano (ano corrente) ou todos (sem filtro).
     */
    @Transactional(readOnly = true)
    public DashboardAnaliticoResponse analitico(Integer estacionamentoId, String periodo) {
        if (!estacionamentoRepository.existsById(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Estacionamento não encontrado");
        }

        String periodoNorm = periodo == null ? "hoje" : periodo.trim().toLowerCase();
        Periodo p = resolverPeriodo(periodoNorm);

        long totalSessoes      = sessaoRepository.contarEntradasPeriodo(estacionamentoId, p.inicio, p.fim);
        long totalEncerradas   = sessaoRepository.contarPorStatusPeriodo(estacionamentoId, SessaoStatus.ENCERRADA, p.inicio, p.fim);
        long totalReservas     = sessaoRepository.contarReservasPeriodo(estacionamentoId, p.inicio, p.fim);
        long sessoesCanceladas = sessaoRepository.contarPorStatusPeriodo(estacionamentoId, SessaoStatus.CANCELADA, p.inicio, p.fim);

        // Pagamentos (para receita/ticket médio)
        List<SessaoEstacionamento> pagamentos = sessaoRepository.pagamentosNoPeriodo(estacionamentoId, p.inicio, p.fim);
        BigDecimal totalPago = pagamentos.stream()
                .map(s -> s.getValorPago() == null ? BigDecimal.ZERO : s.getValorPago())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticketMedio = pagamentos.isEmpty()
                ? null
                : totalPago.divide(BigDecimal.valueOf(pagamentos.size()), 2, RoundingMode.HALF_UP);

        // Receita por dia (agrupamento em Java para evitar dependência do dialeto SQL)
        Map<LocalDate, BigDecimal> receitaPorDiaMap = new LinkedHashMap<>();
        for (SessaoEstacionamento s : pagamentos) {
            LocalDate dia = s.getHoraPagamento().toLocalDate();
            BigDecimal atual = receitaPorDiaMap.getOrDefault(dia, BigDecimal.ZERO);
            receitaPorDiaMap.put(dia, atual.add(s.getValorPago()));
        }
        List<DashboardAnaliticoResponse.ReceitaDia> receitaPorDia = receitaPorDiaMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DashboardAnaliticoResponse.ReceitaDia(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());

        // Ocupação por hora (24 buckets fixos)
        List<SessaoEstacionamento> entradas = sessaoRepository.entradasNoPeriodo(estacionamentoId, p.inicio, p.fim);
        long[] horasContagem = new long[24];
        for (SessaoEstacionamento s : entradas) {
            int h = s.getHoraEntrada().getHour();
            if (h >= 0 && h < 24) horasContagem[h]++;
        }
        List<DashboardAnaliticoResponse.OcupacaoHora> ocupacaoPorHora = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            ocupacaoPorHora.add(new DashboardAnaliticoResponse.OcupacaoHora(h, horasContagem[h]));
        }
        // Top 5 horários
        List<DashboardAnaliticoResponse.OcupacaoHora> topHorarios = ocupacaoPorHora.stream()
                .filter(oh -> oh.getQtdSessoes() > 0)
                .sorted(Comparator.comparingLong(DashboardAnaliticoResponse.OcupacaoHora::getQtdSessoes).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Ocupação média no período: total entradas / vagas totais.
        // Métrica simples para o dashboard (não confunde com taxa instantânea).
        BigDecimal ocupacaoMediaPct = null;
        Optional<VagasEstacionamento> vagasOpt = vagasRepository.findByEstacionamentoId(estacionamentoId);
        Integer vagasTotais = vagasOpt.map(VagasEstacionamento::getQtdVagasTotais).orElse(0);
        if (vagasTotais != null && vagasTotais > 0) {
            // Mede ocupação como "sessões totais por vaga × 100" limitado a uma escala razoável.
            BigDecimal denom = BigDecimal.valueOf(vagasTotais);
            ocupacaoMediaPct = BigDecimal.valueOf(totalSessoes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(denom, 2, RoundingMode.HALF_UP);
        }

        return new DashboardAnaliticoResponse(
                periodoNorm,
                totalSessoes,
                totalEncerradas,
                totalReservas,
                sessoesCanceladas,
                ocupacaoMediaPct,
                ticketMedio,
                receitaPorDia,
                ocupacaoPorHora,
                topHorarios
        );
    }

    private Periodo resolverPeriodo(String p) {
        LocalDate hoje = hojeSaoPaulo();
        switch (p) {
            case "hoje":
                return new Periodo(hoje.atStartOfDay(), hoje.atTime(LocalTime.MAX));
            case "semana": {
                LocalDate inicio = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate fim    = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                return new Periodo(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
            }
            case "mes":
            case "mês":
                return new Periodo(
                        hoje.withDayOfMonth(1).atStartOfDay(),
                        hoje.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX));
            case "ano":
                return new Periodo(
                        hoje.withDayOfYear(1).atStartOfDay(),
                        hoje.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX));
            case "todos":
                return new Periodo(null, null);
            default:
                throw new RequisicaoInvalidaException("Período inválido. Use: hoje, semana, mes, ano ou todos.");
        }
    }

    private static class Periodo {
        final LocalDateTime inicio;
        final LocalDateTime fim;
        Periodo(LocalDateTime inicio, LocalDateTime fim) { this.inicio = inicio; this.fim = fim; }
    }

    private LocalDate hojeSaoPaulo() {
        return LocalDate.now(ZONE_SAO_PAULO);
    }

    private LocalDateTime nowSaoPaulo() {
        return LocalDateTime.now(ZONE_SAO_PAULO);
    }

    // ── Reservas aguardando confirmação (painel admin) ───────────────────────

    /**
     * Lista reservas em AGUARDANDO_CONFIRMACAO de um estacionamento, montando o
     * payload do QR de confirmação que o admin web deve exibir/imprimir. O
     * mobile escaneia esse QR e chama POST /api/reserva/confirmar.
     */
    @Transactional(readOnly = true)
    public List<ReservaAguardandoResponse> listarReservasAguardandoConfirmacao(Integer estacionamentoId) {
        List<SessaoEstacionamento> sessoes =
                sessaoRepository.findReservasAguardandoConfirmacao(estacionamentoId);

        return sessoes.stream().map(sessao -> {
            String motoristaNome = null;
            String cpfMascarado = null;
            if (sessao.getClienteId() != null) {
                Cliente cliente = clienteRepository.findById(sessao.getClienteId()).orElse(null);
                if (cliente != null) {
                    motoristaNome = cliente.getNome();
                    cpfMascarado = mascararCpf(cliente.getCpf());
                }
            }

            String modeloVeiculo = null;
            if (sessao.getPlaca() != null) {
                modeloVeiculo = veiculoRepository.findById(sessao.getPlaca())
                        .map(Veiculo::getNome)
                        .orElse(null);
            }

            // Payload estruturado que o mobile escaneia em modo "confirmação de reserva".
            String qr = sessao.getQrCode();
            String payload = String.format(
                    "{\"v\":1,\"app\":\"paradacerta\",\"tipo\":\"CONFIRMACAO_RESERVA\",\"sessaoId\":\"%s\",\"qrCode\":\"%s\"}",
                    sessao.getId(), qr
            );

            Long inicioMs = sessao.getInicioReservaPrevisto() != null
                    ? sessao.getInicioReservaPrevisto().atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli()
                    : null;
            Long criadaMs = sessao.getHoraEntrada() != null
                    ? sessao.getHoraEntrada().atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli()
                    : null;

            return new ReservaAguardandoResponse(
                    String.valueOf(sessao.getId()),
                    qr,
                    payload,
                    motoristaNome,
                    cpfMascarado,
                    sessao.getPlaca(),
                    modeloVeiculo,
                    inicioMs,
                    criadaMs,
                    sessao.getValorPago()
            );
        }).collect(Collectors.toList());
    }

    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return cpf;
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(cpf.length() - 2);
    }

    // ── Relatório regional (PREMIUM) ─────────────────────────────────────────

    /**
     * Relatório regional do estacionamento — disponível APENAS para PREMIUM ativo.
     * Lança {@link AcessoNegadoException} (403) caso contrário.
     *
     * Dados são agregados/anonimizados: nenhum motorista é identificado individualmente.
     */
    @Transactional(readOnly = true)
    public RelatorioRegionalResponse relatorioRegional(Integer estacionamentoId, String periodo) {
        Estacionamento meu = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!planoService.temAcessoRelatorioRegional(meu)) {
            throw new AcessoNegadoException(
                "Recurso disponível apenas no plano Premium. Faça upgrade para acessar relatórios regionais."
            );
        }

        if (meu.getCidade() == null || meu.getCidade().isBlank()) {
            // sem cidade não há como agregar; devolve resposta vazia (não inventa dado).
            return new RelatorioRegionalResponse(
                    "—", "—", 0,
                    null, null, 0L, BigDecimal.ZERO, List.of()
            );
        }

        String periodoNorm = periodo == null ? "mes" : periodo.trim().toLowerCase();
        Periodo p = resolverPeriodo(periodoNorm);

        List<Estacionamento> regiao = estacionamentoRepository.findAtivosPorCidade(meu.getCidade());
        int totalRegiao = regiao.size();

        BigDecimal precoMedio = regiao.stream()
                .map(Estacionamento::getPrecoHora)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long contagemPreco = regiao.stream()
                .map(Estacionamento::getPrecoHora)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .count();
        precoMedio = contagemPreco == 0 ? null
                : precoMedio.divide(BigDecimal.valueOf(contagemPreco), 2, RoundingMode.HALF_UP);

        BigDecimal avaliacaoMediaRegiao = regiao.stream()
                .map(Estacionamento::getAvaliacaoMedia)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long contagemAval = regiao.stream()
                .map(Estacionamento::getAvaliacaoMedia)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal avaliacaoMediaFinal = contagemAval == 0 ? null
                : avaliacaoMediaRegiao.divide(BigDecimal.valueOf(contagemAval), 2, RoundingMode.HALF_UP);

        long totalSessoes = 0L;
        BigDecimal receitaAgregada = BigDecimal.ZERO;
        Map<String, long[]> sessoesPorBairro = new LinkedHashMap<>(); // bairro -> [qtdEst, sessoes]
        Map<String, BigDecimal[]> precoPorBairro = new LinkedHashMap<>(); // bairro -> [soma, count]

        for (Estacionamento e : regiao) {
            long sessoesEst = sessaoRepository.contarEntradasPeriodo(e.getId(), p.inicio, p.fim);
            totalSessoes += sessoesEst;
            List<SessaoEstacionamento> pagos = sessaoRepository.pagamentosNoPeriodo(e.getId(), p.inicio, p.fim);
            for (SessaoEstacionamento s : pagos) {
                if (s.getValorPago() != null) receitaAgregada = receitaAgregada.add(s.getValorPago());
            }

            String bairro = (e.getBairro() == null || e.getBairro().isBlank()) ? "—" : e.getBairro();
            long[] estatBairro = sessoesPorBairro.getOrDefault(bairro, new long[]{0L, 0L});
            estatBairro[0] += 1;
            estatBairro[1] += sessoesEst;
            sessoesPorBairro.put(bairro, estatBairro);

            if (e.getPrecoHora() != null && e.getPrecoHora().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal[] estatPreco = precoPorBairro.getOrDefault(bairro, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                estatPreco[0] = estatPreco[0].add(e.getPrecoHora());
                estatPreco[1] = estatPreco[1].add(BigDecimal.ONE);
                precoPorBairro.put(bairro, estatPreco);
            }
        }

        List<RelatorioRegionalResponse.BairroResumo> distribuicao = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : sessoesPorBairro.entrySet()) {
            String bairro = entry.getKey();
            long[] estat = entry.getValue();
            BigDecimal[] precoEstat = precoPorBairro.get(bairro);
            BigDecimal precoMedioBairro = (precoEstat == null || precoEstat[1].compareTo(BigDecimal.ZERO) == 0)
                    ? null
                    : precoEstat[0].divide(precoEstat[1], 2, RoundingMode.HALF_UP);
            distribuicao.add(new RelatorioRegionalResponse.BairroResumo(
                    bairro, (int) estat[0], precoMedioBairro, estat[1]
            ));
        }
        distribuicao.sort(Comparator.comparingLong(RelatorioRegionalResponse.BairroResumo::getSessoesPeriodo).reversed());

        return new RelatorioRegionalResponse(
                meu.getCidade(),
                meu.getBairro() == null ? "—" : meu.getBairro(),
                totalRegiao,
                precoMedio,
                avaliacaoMediaFinal,
                totalSessoes,
                receitaAgregada.setScale(2, RoundingMode.HALF_UP),
                distribuicao
        );
    }

    // ── Encerrar / cancelar (chamado do painel web) ──────────────────────────

    @Transactional(readOnly = true)
    public MapaCalorOperacaoResponse mapaCalor(Integer estacionamentoId, String periodo) {
        Estacionamento meu = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!planoService.temAcessoRelatorioRegional(meu)) {
            throw new AcessoNegadoException(
                    "Recurso disponível apenas no plano Premium. Faça upgrade para acessar o mapa de calor."
            );
        }

        String periodoNorm = periodo == null ? "mes" : periodo.trim().toLowerCase();
        Periodo p = resolverPeriodo(periodoNorm);

        if (meu.getCidade() == null || meu.getCidade().isBlank()) {
            return new MapaCalorOperacaoResponse("São Paulo", "SP", periodoNorm, 0L, 0, List.of());
        }

        List<Estacionamento> regiao = estacionamentoRepository.findAtivosPorCidade(meu.getCidade()).stream()
                .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                .filter(e -> e.getLatitude().compareTo(BigDecimal.ZERO) != 0 || e.getLongitude().compareTo(BigDecimal.ZERO) != 0)
                .toList();

        if (regiao.isEmpty()) {
            return new MapaCalorOperacaoResponse(meu.getCidade(), meu.getUf(), periodoNorm, 0L, 0, List.of());
        }

        List<Integer> ids = regiao.stream().map(Estacionamento::getId).toList();
        Map<Integer, Long> entradasPorEstacionamento = new HashMap<>();
        for (Object[] row : sessaoRepository.contarEntradasPorEstacionamentoPeriodo(ids, p.inicio, p.fim)) {
            Integer id = ((Number) row[0]).intValue();
            Long total = ((Number) row[1]).longValue();
            entradasPorEstacionamento.put(id, total);
        }

        Map<String, PontoCalorAgg> agregados = new LinkedHashMap<>();
        for (Estacionamento e : regiao) {
            long entradas = entradasPorEstacionamento.getOrDefault(e.getId(), 0L);
            if (entradas <= 0) continue;

            BigDecimal lat = aproximarCoordenada(e.getLatitude());
            BigDecimal lng = aproximarCoordenada(e.getLongitude());
            String bairro = (e.getBairro() == null || e.getBairro().isBlank()) ? "Região não informada" : e.getBairro().trim();
            String chave = bairro.toLowerCase() + "|" + lat + "|" + lng;

            PontoCalorAgg agg = agregados.getOrDefault(chave, new PontoCalorAgg(bairro, lat, lng));
            agg.quantidadeEstacionamentos++;
            agg.entradasPeriodo += entradas;
            agregados.put(chave, agg);
        }

        long maxEntradas = agregados.values().stream()
                .mapToLong(a -> a.entradasPeriodo)
                .max()
                .orElse(0L);

        List<MapaCalorOperacaoResponse.PontoCalor> pontos = agregados.values().stream()
                .sorted(Comparator.comparingLong((PontoCalorAgg a) -> a.entradasPeriodo).reversed())
                .map(a -> {
                    BigDecimal intensidade = maxEntradas <= 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(a.entradasPeriodo)
                                    .divide(BigDecimal.valueOf(maxEntradas), 2, RoundingMode.HALF_UP);
                    return new MapaCalorOperacaoResponse.PontoCalor(
                            a.bairro,
                            a.latitude,
                            a.longitude,
                            a.quantidadeEstacionamentos,
                            a.entradasPeriodo,
                            intensidade
                    );
                })
                .toList();

        long totalEntradas = pontos.stream().mapToLong(MapaCalorOperacaoResponse.PontoCalor::getEntradasPeriodo).sum();
        return new MapaCalorOperacaoResponse(meu.getCidade(), meu.getUf(), periodoNorm, totalEntradas, pontos.size(), pontos);
    }

    private static BigDecimal aproximarCoordenada(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static class PontoCalorAgg {
        final String bairro;
        final BigDecimal latitude;
        final BigDecimal longitude;
        int quantidadeEstacionamentos;
        long entradasPeriodo;

        PontoCalorAgg(String bairro, BigDecimal latitude, BigDecimal longitude) {
            this.bairro = bairro;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    @Transactional
    public ApiResponse encerrarSessao(Long sessaoId, BigDecimal valorPago) {
        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão não encontrada"));

        // Admin pode encerrar entrada comum (ATIVA) ou reserva confirmada (EM_USO).
        // Reservas AGUARDANDO_CONFIRMACAO devem usar /cancelar.
        if (sessao.getStatus() != SessaoStatus.ATIVA
                && sessao.getStatus() != SessaoStatus.EM_USO) {
            throw new ConflictException("Sessão já encerrada, cancelada ou aguardando confirmação");
        }

        LocalDateTime agora = nowSaoPaulo();
        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(agora);
        sessao.setHoraPagamento(agora);

        if (valorPago != null && valorPago.compareTo(BigDecimal.ZERO) > 0) {
            if (Boolean.TRUE.equals(sessao.getReservado()) && sessao.getValorPago() != null) {
                sessao.setValorPago(sessao.getValorPago().add(valorPago));
            } else {
                sessao.setValorPago(valorPago);
            }
        }

        sessaoRepository.save(sessao);
        return ApiResponse.ok("Sessão encerrada");
    }

    @Transactional
    public ApiResponse cancelarReserva(Long sessaoId) {
        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Reserva não encontrada"));

        if (!Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException("Esta sessão não é uma reserva");
        }
        // Reserva já confirmada pelo motorista (EM_USO) não pode ser cancelada
        // pelo admin — o motorista está fisicamente usando a vaga. O admin
        // precisa aguardar o encerramento normal ou usar /encerrar.
        if (sessao.getStatus() == SessaoStatus.EM_USO) {
            throw new ConflictException(
                "Esta reserva já foi confirmada pelo motorista e está em uso. "
              + "Não é possível cancelá-la — aguarde a finalização do uso."
            );
        }
        // Cancelamento permitido em AGUARDANDO_CONFIRMACAO (fluxo novo) e
        // ATIVA (compat legado pré-migração).
        if (sessao.getStatus() != SessaoStatus.AGUARDANDO_CONFIRMACAO
                && sessao.getStatus() != SessaoStatus.ATIVA) {
            // Idempotência: se já foi cancelada antes, NÃO redispara e-mail.
            throw new ConflictException("Reserva já encerrada ou cancelada");
        }

        // Cancelamento manual pelo admin (painel web) não gera faturamento:
        // valorPago é zerado para não somar nos totais financeiros. Status
        // CANCELADA é preservado para auditoria.
        sessao.setValorPago(BigDecimal.ZERO);
        sessao.setStatus(SessaoStatus.CANCELADA);
        sessao.setHoraSaida(nowSaoPaulo());
        sessaoRepository.save(sessao);

        // Notificação ao motorista. NÃO reverte cancelamento em caso de falha.
        boolean emailEnviado = notificarMotoristaReservaCancelada(sessao);
        return ApiResponse.ok(emailEnviado
                ? "Reserva cancelada com sucesso. O motorista será notificado por e-mail."
                : "Reserva cancelada com sucesso, mas não foi possível enviar o e-mail de notificação ao motorista.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse cancelarSessao(Long sessaoId, CancelarSessaoRequest req) {
        if (req == null || req.getAdminId() == null || req.getSenhaAdmin() == null || req.getSenhaAdmin().isBlank()) {
            throw new RequisicaoInvalidaException("Informe a senha do administrador para cancelar a entrada");
        }

        SessaoEstacionamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão não encontrada"));

        AdmEstacionamento admin = admRepository.findById(req.getAdminId())
                .orElseThrow(() -> new AcessoNegadoException("Administrador não encontrado"));

        if (Boolean.FALSE.equals(admin.getAtivo())) {
            throw new AcessoNegadoException("Administrador desativado");
        }
        if (!Objects.equals(admin.getEstacionamentoId(), sessao.getEstacionamentoId())) {
            throw new AcessoNegadoException("Administrador não pertence a este estacionamento");
        }
        if (!BCrypt.checkpw(req.getSenhaAdmin(), admin.getSenhaHash())) {
            throw new AcessoNegadoException("Senha do administrador inválida");
        }
        // Reserva EM_USO está em poder do motorista — admin não cancela mais.
        if (sessao.getStatus() == SessaoStatus.EM_USO) {
            throw new ConflictException(
                "Esta reserva já foi confirmada pelo motorista e está em uso. "
              + "Não é possível cancelar — aguarde a finalização."
            );
        }
        if (sessao.getStatus() != SessaoStatus.ATIVA
                && sessao.getStatus() != SessaoStatus.AGUARDANDO_CONFIRMACAO) {
            throw new ConflictException("Sessão já encerrada ou cancelada");
        }

        // Cancelamento manual feito pelo administrador (painel web) NÃO gera
        // faturamento — o valor pago é zerado para não somar nos totais
        // financeiros (dashboard, financeiro, ticket médio, gráficos).
        // O status CANCELADA é preservado para auditoria/relatórios.
        boolean eraReserva = Boolean.TRUE.equals(sessao.getReservado());
        sessao.setValorPago(BigDecimal.ZERO);
        sessao.setStatus(SessaoStatus.CANCELADA);
        sessao.setHoraSaida(nowSaoPaulo());
        sessaoRepository.save(sessao);

        // E-mail é exclusivo do fluxo de RESERVA cancelada pelo admin.
        // Para entrada comum (sem reserva), não há notificação por e-mail.
        if (eraReserva) {
            boolean emailEnviado = notificarMotoristaReservaCancelada(sessao);
            return ApiResponse.ok(emailEnviado
                    ? "Reserva cancelada com sucesso. O motorista será notificado por e-mail."
                    : "Reserva cancelada com sucesso, mas não foi possível enviar o e-mail de notificação ao motorista.");
        }
        return ApiResponse.ok("Entrada cancelada");
    }

    /**
     * Dispara o e-mail de "reserva cancelada pela administração" ao motorista.
     *
     * <p>Sempre fail-safe — qualquer falha (cliente sem e-mail, estacionamento
     * removido, SMTP fora) retorna {@code false} sem propagar exceção. O
     * cancelamento da reserva NÃO deve ser revertido por falha de e-mail.
     *
     * <p>Pré-requisito: a sessão já deve ter sido persistida com status
     * CANCELADA antes desta chamada (garantia de idempotência).
     */
    private boolean notificarMotoristaReservaCancelada(SessaoEstacionamento sessao) {
        try {
            if (sessao == null || sessao.getClienteId() == null) {
                return false;
            }
            Cliente cliente = clienteRepository.findById(sessao.getClienteId()).orElse(null);
            if (cliente == null) return false;

            Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId()).orElse(null);
            if (est == null) return false;

            String modelo = null;
            if (sessao.getPlaca() != null && !sessao.getPlaca().isBlank()) {
                modelo = veiculoRepository.findById(sessao.getPlaca())
                        .map(Veiculo::getNome)
                        .orElse(null);
            }

            return emailService.enviarEmailReservaCancelada(cliente, est, sessao, modelo);
        } catch (Exception e) {
            // Última barreira — nunca propaga para a transação de cancelamento.
            return false;
        }
    }

    private SessaoStatus parseStatus(String s) {
        try {
            return SessaoStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("Status inválido: " + s);
        }
    }

    /** Resolve nome do cliente, CPF, modelo do veículo em poucas queries. */
    private List<SessaoAdminResponse> hidratar(List<SessaoEstacionamento> sessoes) {
        if (sessoes.isEmpty()) return List.of();

        List<Long> clienteIds = sessoes.stream()
                .map(SessaoEstacionamento::getClienteId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Cliente> clientesPorId = clienteRepository.findAllById(clienteIds).stream()
                .collect(Collectors.toMap(Cliente::getId, c -> c));

        List<String> placas = sessoes.stream()
                .map(SessaoEstacionamento::getPlaca)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
        Map<String, String> modeloPorPlaca = new HashMap<>();
        if (!placas.isEmpty()) {
            veiculoRepository.findAllById(placas).forEach(v -> modeloPorPlaca.put(v.getPlaca(), v.getNome()));
        }

        return sessoes.stream().map(s -> {
            Cliente c = clientesPorId.get(s.getClienteId());
            String nome  = c != null ? c.getNome() : (s.getClienteId() == null ? "Cliente kiosk" : "Cliente #" + s.getClienteId());
            String cpf   = c != null ? c.getCpf()  : null;
            String modelo = s.getPlaca() != null ? modeloPorPlaca.get(s.getPlaca()) : null;

            return new SessaoAdminResponse(
                    s.getId(),
                    s.getClienteId(),
                    nome,
                    cpf,
                    s.getEstacionamentoId(),
                    s.getPlaca(),
                    modelo,
                    s.getHoraEntrada(),
                    s.getInicioReservaPrevisto(),
                    s.getHoraSaida(),
                    s.getHoraPagamento(),
                    s.getValorPago(),
                    s.getStatus() != null ? s.getStatus().name() : null,
                    s.getReservado(),
                    s.getQrCode()
            );
        }).toList();
    }
}
