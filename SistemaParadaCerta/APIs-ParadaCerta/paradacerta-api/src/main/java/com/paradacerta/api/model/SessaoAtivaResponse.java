package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SessaoAtivaResponse {
    private String sessaoId;
    private Integer estacionamentoId;
    private String estacionamentoNome;
    private String pixKey;
    private long horaEntrada;       // Unix timestamp em ms
    private Long inicioReservaPrevisto;  // Unix timestamp em ms, apenas reservas
    private Long dataHoraConfirmacao;    // Unix timestamp em ms, quando o motorista confirmou
    private BigDecimal precoHora;
    private String placa;
    private String modeloVeiculo;
    private boolean reservado;
    private SessaoStatus status;         // AGUARDANDO_CONFIRMACAO | EM_USO | ATIVA
    private BigDecimal valorPagoAntecipado; // valor já pago na criação da reserva
}
