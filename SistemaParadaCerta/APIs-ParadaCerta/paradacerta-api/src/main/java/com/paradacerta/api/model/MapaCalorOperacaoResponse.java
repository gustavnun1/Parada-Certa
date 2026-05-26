package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados agregados para o mapa de calor Premium.
 *
 * LGPD: nao carrega cliente, CPF, placa, sessao individual ou nome de
 * estacionamento. Coordenadas sao aproximadas por regiao.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapaCalorOperacaoResponse {

    private String cidade;
    private String uf;
    private String periodo;
    private Long totalEntradasPeriodo;
    private Integer totalRegioes;
    private List<PontoCalor> pontos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PontoCalor {
        private String bairro;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer quantidadeEstacionamentos;
        private Long entradasPeriodo;
        private BigDecimal intensidade;
    }
}
