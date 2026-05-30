package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Item da listagem de reservas aguardando confirmação no painel admin.
 * Inclui os dados que o admin precisa para localizar o motorista e o conteúdo
 * do QR Code para imprimir/exibir na entrada do estacionamento.
 */
@Data
@AllArgsConstructor
public class ReservaAguardandoResponse {
    private String sessaoId;
    private String qrCode;                       // conteúdo do QR — será desenhado client-side
    private String qrCodePayload;                // JSON estruturado pronto para o mobile escanear
    private String motoristaNome;
    private String motoristaCpfMascarado;
    private String placa;
    private String modeloVeiculo;
    private Long inicioReservaPrevisto;          // Unix ms
    private Long criadaEm;                       // Unix ms (horaEntrada)
    private BigDecimal valorPagoAntecipado;
}
