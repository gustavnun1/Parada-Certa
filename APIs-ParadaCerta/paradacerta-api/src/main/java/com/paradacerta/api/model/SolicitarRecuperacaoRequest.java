package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolicitarRecuperacaoRequest {

    @NotBlank(message = "Login é obrigatório")
    private String login; // e-mail ou CPF (somente dígitos)

    private boolean cpf = false; // true = login é CPF, false = login é e-mail
}
