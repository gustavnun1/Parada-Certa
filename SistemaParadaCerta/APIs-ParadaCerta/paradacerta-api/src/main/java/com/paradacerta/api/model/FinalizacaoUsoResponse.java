package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Resposta da finalização de uso de vaga reservada.
 * Usada em duas situações:
 *  - GET de cálculo prévio (antes de confirmar o pagamento adicional, se houver)
 *  - POST de finalização efetiva (após cobrança do restante via Pix, quando aplicável)
 *
 * Quando {@code exigeCobrancaAdicional} é true, o app mobile precisa cobrar
 * {@code valorRestante} via Pix antes de chamar a finalização efetiva.
 */
@Data
@AllArgsConstructor
public class FinalizacaoUsoResponse {
    private String sessaoId;
    private Integer estacionamentoId;
    private String estacionamentoNome;
    private String pixKey;
    private long minutosUso;          // tempo total desde dataHoraConfirmacao até agora
    private long minutosCobrados;     // minutosUso arredondado conforme regra (1h min, blocos 30min)
    private BigDecimal precoHora;
    private BigDecimal valorPagoAntecipado;
    private BigDecimal valorFinalCalculado;
    private BigDecimal valorRestante; // diferença a cobrar (0 quando antecipado já cobre)
    private boolean exigeCobrancaAdicional;
}
