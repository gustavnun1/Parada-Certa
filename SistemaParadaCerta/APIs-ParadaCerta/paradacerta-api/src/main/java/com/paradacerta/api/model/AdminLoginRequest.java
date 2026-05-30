package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {

    @NotBlank(message = "E-mail ou CPF e obrigatorio")
    private String email;

    @NotBlank(message = "Senha e obrigatoria")
    private String senha;
}
