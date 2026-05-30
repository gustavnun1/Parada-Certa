package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Resposta da confirmação de reserva via QR Code.
 * Retornada pelo POST /api/reserva/confirmar quando o motorista escaneia
 * com sucesso o QR de confirmação no estacionamento.
 */
@Data
@AllArgsConstructor
public class ConfirmacaoReservaResponse {
    private String sessaoId;
    private Integer estacionamentoId;
    private String estacionamentoNome;
    private String pixKey;
    private long dataHoraConfirmacao; // Unix timestamp em ms
    private BigDecimal precoHora;
}
