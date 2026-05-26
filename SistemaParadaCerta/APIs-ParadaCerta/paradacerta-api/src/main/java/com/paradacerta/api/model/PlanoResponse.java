package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plano vigente de um estacionamento + recursos derivados.
 * Resposta de GET /api/estacionamentos/{id}/plano.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanoResponse {

    private PlanoTipo plano;
    private PlanoCobranca cobranca;
    private LocalDateTime inicio;
    private LocalDateTime fim;

    /** Dias restantes até o fim do ciclo. Null quando {@code fim == null} (sem expiração). */
    private Long diasRestantes;

    /** true se trial BASIC expirou (não bloqueia STANDARD/PREMIUM). */
    private Boolean expirado;

    /** true se o estacionamento ainda tem direito às funcionalidades operacionais. */
    private Boolean ativo;

    // ── Recursos derivados (para o front montar o gating sem refazer a lógica) ─
    /** Limite de fotos do plano vigente (0 se trial expirado). */
    private Integer limiteFotos;

    private Boolean permiteAvaliacoes;
    private Boolean permiteDashboardCompleto;
    private Boolean permiteRelatorioRegional;
    private Boolean permiteDestaqueMapa;

    /** Valor cobrado no ciclo (apenas referência para o front). */
    private BigDecimal valorCiclo;
}
