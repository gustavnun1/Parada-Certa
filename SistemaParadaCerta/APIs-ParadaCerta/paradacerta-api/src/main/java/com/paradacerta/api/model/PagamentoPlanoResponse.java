package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta do pagamento de plano. Devolve o plano vigente após a ativação
 * e os dados não sensíveis do recibo (últimos 4 + bandeira).
 */
@Data
@AllArgsConstructor
public class PagamentoPlanoResponse {

    private Long recibo;
    private String status;
    private BigDecimal valor;
    private String ultimos4;
    private String bandeira;
    private LocalDateTime dataPagamento;
    private PlanoResponse plano;
}
