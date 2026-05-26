package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Resposta do cálculo de cobrança da estadia (sessão não-reservada).
 * Regras: mínimo de 1 hora; acima disso, arredonda para o próximo bloco de 30 minutos.
 */
@Data
@AllArgsConstructor
public class CobrancaEstadiaResponse {

    /** Preço por hora cobrado pelo estacionamento. */
    private BigDecimal precoHora;

    /** Minutos reais de permanência desde a entrada. */
    private long minutosPermanencia;

    /** Minutos efetivamente cobrados após aplicar mínimo e arredondamento. */
    private long minutosCobrados;

    /** Valor total a pagar pela estadia. */
    private BigDecimal valorTotal;
}
