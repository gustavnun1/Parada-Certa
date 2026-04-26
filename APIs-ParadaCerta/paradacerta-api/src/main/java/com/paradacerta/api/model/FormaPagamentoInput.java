package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para receber dados de forma de pagamento via API.
 * Aceita o CPF do cliente e resolve para clienteId internamente.
 *
 * O número do cartão é recebido como dígitos brutos — o controller
 * aplica Luhn, valida a validade e armazena APENAS os últimos 4 dígitos.
 * CVV nunca é recebido nem armazenado.
 */
@Data
public class FormaPagamentoInput {

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String clienteCpf;

    @NotBlank(message = "Tipo de pagamento é obrigatório")
    @Pattern(regexp = "CARTAO_CREDITO|DEBITO|PIX", message = "Tipo de pagamento inválido")
    private String tipoPagamento;

    /** Dígitos brutos (13–19 chars). Null para PIX. */
    @Pattern(regexp = "\\d{13,19}", message = "Número do cartão inválido")
    private String numeroCartao;

    @Size(max = 100, message = "Nome no cartão muito longo")
    private String nomeCartao;

    /** Formato MM/AA */
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Validade deve estar no formato MM/AA")
    private String validade;

    @Size(max = 30, message = "Bandeira inválida")
    private String bandeira;
}
