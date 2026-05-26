package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Pagamento simulado de plano (POST /api/estacionamentos/{id}/plano/pagamento).
 * O número completo do cartão e o CVV NÃO são persistidos.
 */
@Data
public class PagamentoPlanoRequest {

    @NotNull(message = "Tipo do plano é obrigatório")
    private PlanoTipo tipo;

    @NotNull(message = "Cobrança é obrigatória")
    private PlanoCobranca cobranca;

    @NotBlank(message = "Nome no cartão é obrigatório")
    @Size(max = 100)
    private String nomeCartao;

    @NotBlank(message = "Número do cartão é obrigatório")
    private String numeroCartao;

    @NotBlank(message = "Validade é obrigatória")
    @Pattern(regexp = "^\\d{2}/\\d{2}$", message = "Validade deve estar no formato MM/AA")
    private String validade;

    @NotBlank(message = "CVV é obrigatório")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV deve ter 3 ou 4 dígitos")
    private String cvv;

    @Size(max = 14)
    private String cpfCnpjTitular;
}
