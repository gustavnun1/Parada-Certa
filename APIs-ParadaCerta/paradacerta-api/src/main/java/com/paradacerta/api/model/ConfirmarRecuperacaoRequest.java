package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarRecuperacaoRequest {

    @NotBlank(message = "Login é obrigatório")
    private String login; // e-mail ou CPF (somente dígitos)

    private boolean cpf = false;

    @NotBlank(message = "Código é obrigatório")
    @Size(min = 6, max = 6, message = "Código deve ter 6 dígitos")
    private String codigo;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String novaSenha;
}
