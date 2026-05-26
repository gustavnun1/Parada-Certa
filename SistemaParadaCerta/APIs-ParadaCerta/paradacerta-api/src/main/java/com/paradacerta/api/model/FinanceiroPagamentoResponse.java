package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Linha do histórico financeiro do estacionamento.
 * Calculado a partir das sessões ENCERRADAS com valorPago > 0.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceiroPagamentoResponse {
    private Long sessaoId;
    private Long clienteId;
    private String clienteNome;
    private String placa;
    private LocalDateTime dataPagamento;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSaida;
    private BigDecimal valorBruto;
    private BigDecimal taxaPlataforma;
    private BigDecimal valorLiquido;
    private String status;       // "PAGO" / "PENDENTE" / "CANCELADO"
    private Boolean reservado;
    private String forma;        // "QR Code" (padrão hoje) — base para evolução
}
