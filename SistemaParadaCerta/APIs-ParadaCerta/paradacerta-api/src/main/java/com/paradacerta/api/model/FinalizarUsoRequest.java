package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Body do POST /api/reserva/{id}/finalizar-uso
 * - cpf: motorista autenticado (validação de propriedade da reserva)
 * - valorPagoAdicional: quando há cobrança extra (uso ultrapassou a 1ª hora paga),
 *   o mobile cobra via Pix e envia o valor aqui para efetivar o encerramento.
 *   Pode ser zero/null quando não houver excedente.
 */
@Data
public class FinalizarUsoRequest {

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    private BigDecimal valorPagoAdicional;
}
