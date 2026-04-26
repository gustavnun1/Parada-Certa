package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    private String email;

    private String cpf;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

}
