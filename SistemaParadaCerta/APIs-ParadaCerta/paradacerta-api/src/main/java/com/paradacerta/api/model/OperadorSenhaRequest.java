package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /api/operador/{id}/senha — troca senha do operador. */
@Data
public class OperadorSenhaRequest {

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter ao menos 6 caracteres")
    private String senha;
}
