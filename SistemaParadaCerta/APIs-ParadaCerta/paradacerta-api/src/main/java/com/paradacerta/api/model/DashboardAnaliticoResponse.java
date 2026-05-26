package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resposta de GET /api/operacao/{estacionamentoId}/analitico?periodo=...
 * Indicadores agregados do dashboard administrativo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAnaliticoResponse {

    private String  periodo;            // "hoje" | "semana" | "mes" | "ano" | "todos"
    private long    totalSessoes;       // sessões criadas no período
    private long    totalEncerradas;    // sessões encerradas no período
    private long    totalReservas;      // sessões com reservado=true criadas no período
    private long    sessoesCanceladas;  // status=CANCELADA no período

    /** Percentual médio de ocupação no período. null quando vagas totais = 0. */
    private BigDecimal ocupacaoMediaPct;

    /** Ticket médio = soma(valorPago) / count(ENCERRADAS pagas) no período. Pode ser null. */
    private BigDecimal ticketMedio;

    /** Série diária de receita confirmada (ENCERRADA com valorPago > 0). */
    private List<ReceitaDia> receitaPorDia;

    /** Distribuição das entradas por hora do dia (0..23) no período. */
    private List<OcupacaoHora> ocupacaoPorHora;

    /** Top 5 horários com mais entradas (do período). */
    private List<OcupacaoHora> topHorarios;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReceitaDia {
        private LocalDate data;
        private BigDecimal receita;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OcupacaoHora {
        private int hora;        // 0..23
        private long qtdSessoes;
    }
}
