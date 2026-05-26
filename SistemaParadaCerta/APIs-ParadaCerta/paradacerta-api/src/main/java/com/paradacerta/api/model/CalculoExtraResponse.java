package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CalculoExtraResponse {

    /** Indica se há cobrança extra além da 1 hora coberta pela reserva. */
    private boolean temCobrancaExtra;

    /** Minutos excedentes após a 1ª hora (0 se não houver extra). */
    private long minutosExtra;

    /** Valor adicional a pagar (0.00 se não houver extra). */
    private BigDecimal valorExtra;
}
