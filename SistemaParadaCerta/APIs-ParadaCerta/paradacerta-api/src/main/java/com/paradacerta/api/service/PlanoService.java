package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.AssinaturaPlanoPagamentoRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Regras de plano de assinatura dos estacionamentos.
 *
 * Catálogo (preços em BRL — referência apenas; TCC não cobra de fato):
 *  - BASIC    : trial 30 dias  — R$ 0,00.
 *  - STANDARD : R$ 149,90/mês  ou R$ 1.798,80/ano.
 *  - PREMIUM  : R$ 399,90/mês  ou R$ 4.798,80/ano.
 *
 * Limites/recursos:
 *  - Fotos: BASIC=3, STANDARD=3, PREMIUM=5.
 *  - Basic em trial expirado: sem acesso a recursos.
 *  - Relatórios regionais: apenas PREMIUM ativo.
 *  - Destaque no mapa: apenas PREMIUM ativo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoService {

    public static final int DIAS_TRIAL_BASIC = 30;
    public static final int DIAS_MENSAL = 30;
    public static final int DIAS_ANUAL  = 365;

    public static final int LIMITE_FOTOS_BASIC = 3;
    public static final int LIMITE_FOTOS_STANDARD = 3;
    public static final int LIMITE_FOTOS_PREMIUM = 5;

    private static final BigDecimal PRECO_STANDARD_MENSAL = new BigDecimal("149.90");
    private static final BigDecimal PRECO_STANDARD_ANUAL  = new BigDecimal("1798.80");
    private static final BigDecimal PRECO_PREMIUM_MENSAL  = new BigDecimal("399.90");
    private static final BigDecimal PRECO_PREMIUM_ANUAL   = new BigDecimal("4798.80");

    private final EstacionamentoRepository estacionamentoRepository;
    private final AssinaturaPlanoPagamentoRepository assinaturaPagamentoRepository;

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * Resposta consolidada para o front, com todos os flags de gating.
     */
    public PlanoResponse construirPlanoResponse(Estacionamento est) {
        PlanoTipo plano = est.getPlano() == null ? PlanoTipo.BASIC : est.getPlano();
        PlanoCobranca cobranca = est.getPlanoCobranca() == null ? PlanoCobranca.TRIAL : est.getPlanoCobranca();

        boolean expirado = isExpirado(est);
        boolean ativo = !expirado;
        Long diasRestantes = calcularDiasRestantes(est);

        return new PlanoResponse(
                plano,
                cobranca,
                est.getPlanoInicio(),
                est.getPlanoFim(),
                diasRestantes,
                expirado,
                ativo,
                getLimiteFotos(est),
                permiteAvaliacoes(est),
                permiteDashboardCompleto(est),
                temAcessoRelatorioRegional(est),
                temDestaqueMapa(est),
                getValorCiclo(plano, cobranca)
        );
    }

    @Transactional(readOnly = true)
    public PlanoResponse buscarPlano(Integer estacionamentoId) {
        Estacionamento est = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
        return construirPlanoResponse(est);
    }

    // ── Mudança de plano ─────────────────────────────────────────────────────

    @Transactional
    public PlanoResponse mudarPlano(Integer estacionamentoId, PlanoUpdateRequest req) {
        Estacionamento est = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        validarCombinacao(req.getTipo(), req.getCobranca());

        LocalDateTime agora = LocalDateTime.now();
        int diasDoCiclo;
        switch (req.getCobranca()) {
            case TRIAL:  diasDoCiclo = DIAS_TRIAL_BASIC; break;
            case MENSAL: diasDoCiclo = DIAS_MENSAL;      break;
            case ANUAL:  diasDoCiclo = DIAS_ANUAL;       break;
            default: throw new RequisicaoInvalidaException("Cobrança inválida");
        }

        // Renovação: se for o mesmo plano e ainda não expirou, soma a partir do planoFim atual.
        LocalDateTime base;
        boolean renovacao = req.getTipo() == est.getPlano()
                && est.getPlanoFim() != null
                && est.getPlanoFim().isAfter(agora);
        if (renovacao) {
            base = est.getPlanoFim();
            log.info("Plano renovado: estId={}, tipo={}, cobranca={}", est.getId(), req.getTipo(), req.getCobranca());
        } else {
            base = agora;
            est.setPlanoInicio(agora);
            log.info("Plano alterado: estId={}, de={}/{} para={}/{}",
                    est.getId(), est.getPlano(), est.getPlanoCobranca(), req.getTipo(), req.getCobranca());
        }

        est.setPlano(req.getTipo());
        est.setPlanoCobranca(req.getCobranca());
        est.setPlanoFim(base.plusDays(diasDoCiclo));

        estacionamentoRepository.save(est);
        return construirPlanoResponse(est);
    }

    private void validarCombinacao(PlanoTipo tipo, PlanoCobranca cobranca) {
        if (tipo == PlanoTipo.BASIC && cobranca != PlanoCobranca.TRIAL) {
            throw new RequisicaoInvalidaException("Basic só aceita cobrança TRIAL");
        }
        if (tipo != PlanoTipo.BASIC && cobranca == PlanoCobranca.TRIAL) {
            throw new RequisicaoInvalidaException("TRIAL só é válido para o plano Basic");
        }
    }

    // ── Helpers de gating ────────────────────────────────────────────────────

    public boolean isBasicExpired(Estacionamento est) {
        return est != null
                && est.getPlano() == PlanoTipo.BASIC
                && est.getPlanoFim() != null
                && est.getPlanoFim().isBefore(LocalDateTime.now());
    }

    /** Verdadeiro se trial expirado (BASIC) — para Standard/Premium o ciclo expira mas o plano permanece ativo até pagamento. */
    public boolean isExpirado(Estacionamento est) {
        if (est == null) return true;
        if (est.getPlano() == PlanoTipo.BASIC) {
            return est.getPlanoFim() != null && est.getPlanoFim().isBefore(LocalDateTime.now());
        }
        // Para STANDARD/PREMIUM no TCC: se passou planoFim, consideramos vencido (acesso bloqueado).
        return est.getPlanoFim() != null && est.getPlanoFim().isBefore(LocalDateTime.now());
    }

    /** Limite de fotos do plano vigente. Retorna 0 quando o plano está expirado. */
    public int getLimiteFotos(Estacionamento est) {
        if (est == null) return 0;
        if (isExpirado(est)) return 0;
        PlanoTipo p = est.getPlano() == null ? PlanoTipo.BASIC : est.getPlano();
        switch (p) {
            case BASIC:    return LIMITE_FOTOS_BASIC;
            case STANDARD: return LIMITE_FOTOS_STANDARD;
            case PREMIUM:  return LIMITE_FOTOS_PREMIUM;
            default:       return 0;
        }
    }

    public boolean permiteAvaliacoes(Estacionamento est) {
        return !isExpirado(est);
    }

    public boolean permiteDashboardCompleto(Estacionamento est) {
        return !isExpirado(est);
    }

    public boolean temAcessoRelatorioRegional(Estacionamento est) {
        return est != null && est.getPlano() == PlanoTipo.PREMIUM && !isExpirado(est);
    }

    public boolean temDestaqueMapa(Estacionamento est) {
        return est != null && est.getPlano() == PlanoTipo.PREMIUM && !isExpirado(est);
    }

    private Long calcularDiasRestantes(Estacionamento est) {
        if (est == null || est.getPlanoFim() == null) return null;
        long minutos = Duration.between(LocalDateTime.now(), est.getPlanoFim()).toMinutes();
        if (minutos <= 0) return 0L;
        return (long) Math.ceil(minutos / (60.0 * 24.0));
    }

    // ── Pagamento simulado + ativação do plano ───────────────────────────────

    /**
     * Valida o cartão (Luhn + validade + CVV), registra o recibo (somente últimos 4 + bandeira)
     * e ativa o plano. Plano BASIC/TRIAL não exige cartão e é tratado fora deste fluxo.
     */
    @Transactional
    public PagamentoPlanoResponse processarPagamentoEAtualizarPlano(Integer estacionamentoId,
                                                                    PagamentoPlanoRequest req) {
        Estacionamento est = estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        validarCombinacao(req.getTipo(), req.getCobranca());
        if (req.getCobranca() == PlanoCobranca.TRIAL) {
            throw new RequisicaoInvalidaException("Trial não exige pagamento.");
        }

        String numero = req.getNumeroCartao() == null ? "" : req.getNumeroCartao().replaceAll("\\D", "");
        if (!luhnValido(numero)) {
            throw new RequisicaoInvalidaException("Número do cartão inválido.");
        }
        if (!validadeFutura(req.getValidade())) {
            throw new RequisicaoInvalidaException("Cartão vencido ou validade inválida.");
        }
        if (req.getCvv() == null || !req.getCvv().matches("\\d{3,4}")) {
            throw new RequisicaoInvalidaException("CVV inválido.");
        }
        if (req.getNomeCartao() == null || req.getNomeCartao().trim().isEmpty()) {
            throw new RequisicaoInvalidaException("Nome no cartão é obrigatório.");
        }

        BigDecimal valor = getValorCiclo(req.getTipo(), req.getCobranca());
        String ultimos4 = numero.substring(Math.max(0, numero.length() - 4));
        String bandeira = identificarBandeira(numero);

        AssinaturaPlanoPagamento recibo = new AssinaturaPlanoPagamento();
        recibo.setEstacionamentoId(est.getId());
        recibo.setPlano(req.getTipo());
        recibo.setCobranca(req.getCobranca());
        recibo.setValor(valor);
        recibo.setStatus("APROVADO");
        recibo.setDataPagamento(LocalDateTime.now());
        recibo.setUltimos4(ultimos4);
        recibo.setBandeira(bandeira);
        recibo.setNomeCartao(req.getNomeCartao().trim());
        recibo = assinaturaPagamentoRepository.save(recibo);

        // Ativa/renova o plano de fato (mesma lógica de mudarPlano).
        PlanoUpdateRequest update = new PlanoUpdateRequest();
        update.setTipo(req.getTipo());
        update.setCobranca(req.getCobranca());
        PlanoResponse planoAtivado = mudarPlano(est.getId(), update);

        log.info("Pagamento de plano aprovado: estId={}, plano={}, cobranca={}, valor={}",
                est.getId(), req.getTipo(), req.getCobranca(), valor);

        return new PagamentoPlanoResponse(
                recibo.getId(),
                recibo.getStatus(),
                recibo.getValor(),
                recibo.getUltimos4(),
                recibo.getBandeira(),
                recibo.getDataPagamento(),
                planoAtivado
        );
    }

    /** Algoritmo de Luhn. Aceita números com 13 a 19 dígitos. */
    private static boolean luhnValido(String digits) {
        if (digits == null || digits.length() < 13 || digits.length() > 19) return false;
        int soma = 0;
        boolean dobrar = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = Character.digit(digits.charAt(i), 10);
            if (d < 0) return false;
            if (dobrar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    /** Valida validade no formato MM/AA — deve ser igual ou posterior ao mês atual. */
    private static boolean validadeFutura(String validade) {
        try {
            if (validade == null || !validade.matches("\\d{2}/\\d{2}")) return false;
            String[] partes = validade.split("/");
            int mes = Integer.parseInt(partes[0]);
            int ano = Integer.parseInt(partes[1]);
            if (mes < 1 || mes > 12) return false;
            YearMonth cartao = YearMonth.of(2000 + ano, mes);
            return !cartao.isBefore(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    /** Heurística simples para identificar a bandeira a partir dos primeiros dígitos. */
    private static String identificarBandeira(String digits) {
        if (digits == null || digits.isEmpty()) return "OUTRO";
        if (digits.startsWith("4")) return "VISA";
        if (digits.matches("^5[1-5].*") || digits.matches("^2(2[2-9]|[3-6]\\d|7[01]|720).*")) return "MASTERCARD";
        if (digits.matches("^3[47].*")) return "AMEX";
        if (digits.matches("^(36|38|30[0-5]).*")) return "DINERS";
        if (digits.startsWith("6011") || digits.startsWith("65")) return "DISCOVER";
        if (digits.startsWith("35")) return "JCB";
        if (digits.startsWith("4011") || digits.startsWith("438935") || digits.startsWith("431274")
                || digits.startsWith("451416") || digits.startsWith("5067") || digits.startsWith("509")
                || digits.startsWith("6277") || digits.startsWith("6362") || digits.startsWith("6516")
                || digits.startsWith("650") || digits.startsWith("6550")) return "ELO";
        if (digits.startsWith("606282") || digits.startsWith("3841")) return "HIPERCARD";
        return "OUTRO";
    }

    private BigDecimal getValorCiclo(PlanoTipo plano, PlanoCobranca cobranca) {
        if (plano == null) return BigDecimal.ZERO;
        switch (plano) {
            case BASIC: return BigDecimal.ZERO;
            case STANDARD: return cobranca == PlanoCobranca.ANUAL ? PRECO_STANDARD_ANUAL : PRECO_STANDARD_MENSAL;
            case PREMIUM:  return cobranca == PlanoCobranca.ANUAL ? PRECO_PREMIUM_ANUAL  : PRECO_PREMIUM_MENSAL;
            default: return BigDecimal.ZERO;
        }
    }
}
