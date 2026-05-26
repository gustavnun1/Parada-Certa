package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SolicitarRecuperacaoRequest {

    @NotBlank(message = "Login e obrigatorio")
    private String login; // e-mail ou CPF (somente digitos)

    private boolean cpf = false; // true = login e CPF, false = login e e-mail

    private String perfil; // ADMIN no painel web; vazio/null mantem fluxo do app
}
