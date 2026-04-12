package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncerrarRequest {

    @NotBlank(message = "Método de pagamento é obrigatório")
    private String metodoPagamento;
}
