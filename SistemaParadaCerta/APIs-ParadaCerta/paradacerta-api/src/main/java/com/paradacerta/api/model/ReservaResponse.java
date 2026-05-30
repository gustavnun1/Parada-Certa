package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReservaResponse {
    private String sessaoId;
    private Integer estacionamentoId;
    private String estacionamentoNome;
    private String pixKey;
    private long horaEntrada;       // Unix timestamp em ms
    private long inicioReservaPrevisto; // Unix timestamp em ms
    private BigDecimal precoHora;
    private String placa;
    private String modeloVeiculo;
}
