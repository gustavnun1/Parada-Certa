package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Visão administrativa de uma sessão (operação/reservas do painel web).
 * Inclui dados do cliente para exibir no painel do operador.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessaoAdminResponse {
    private Long id;
    private Long clienteId;
    private String clienteNome;
    private String clienteCpf;
    private Integer estacionamentoId;
    private String placa;
    private String modeloVeiculo;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSaida;
    private LocalDateTime horaPagamento;
    private BigDecimal valorPago;
    private String status;     // ATIVA / ENCERRADA / CANCELADA
    private Boolean reservado;
    private String qrCode;
}
