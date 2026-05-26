package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Cards do dashboard operacional / aba Reservas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperacaoResumoResponse {
    private long ativas;
    private long reservasAtivas;
    private long encerradasHoje;
    private long canceladasHoje;
    private BigDecimal receitaPrevista;
    private BigDecimal receitaRecebidaHoje;

    /** Espelhamento das vagas do estacionamento (para os cards Vagas / Ocupadas / Reservadas / Total). */
    private Integer vagasTotais;
    private Integer vagasDisponiveis;
    private Integer vagasReservadas;
    private Integer vagasReservaveis;
}
