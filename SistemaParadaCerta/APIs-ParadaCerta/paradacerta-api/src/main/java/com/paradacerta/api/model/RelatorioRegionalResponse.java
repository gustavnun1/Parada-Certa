package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Relatório regional disponível apenas no plano PREMIUM.
 *
 * Dados são agregados/anonimizados: nenhuma linha identifica um motorista
 * individual. Mostra estatísticas por bairro/cidade dos estacionamentos
 * concorrentes do mesmo município.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelatorioRegionalResponse {

    private String cidade;
    private String bairro;
    private Integer totalEstacionamentosNaRegiao;
    private BigDecimal precoMedioHora;
    private BigDecimal avaliacaoMedia;
    private Long totalSessoesPeriodo;
    private BigDecimal receitaAgregadaPeriodo;

    private List<BairroResumo> distribuicaoPorBairro;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BairroResumo {
        private String bairro;
        private Integer quantidadeEstacionamentos;
        private BigDecimal precoMedio;
        private Long sessoesPeriodo;
    }
}
