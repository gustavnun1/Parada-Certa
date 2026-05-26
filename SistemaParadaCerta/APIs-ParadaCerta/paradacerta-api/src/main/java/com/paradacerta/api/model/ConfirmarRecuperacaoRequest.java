package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarRecuperacaoRequest {

    @NotBlank(message = "Login e obrigatorio")
    private String login; // e-mail ou CPF (somente digitos)

    private boolean cpf = false;

    private String perfil;

    @NotBlank(message = "Codigo e obrigatorio")
    @Size(min = 6, max = 6, message = "Codigo deve ter 6 digitos")
    private String codigo;

    @NotBlank(message = "Nova senha e obrigatoria")
    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String novaSenha;
}
