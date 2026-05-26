package com.paradacerta.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Atualização parcial de um operador/admin.
 * Campos não enviados permanecem inalterados.
 */
@Data
public class AdminUpdateRequest {

    @Size(max = 100)
    private String nomeCompleto;

    @Email(message = "E-mail inválido")
    @Size(max = 200)
    private String email;

    @Size(max = 20)
    private String telefone;

    @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter 11 digitos numericos")
    private String cpf;

    private LocalDate dataNascimento;

    /** Se preenchida, gera novo BCrypt hash. */
    @Size(min = 6, max = 100, message = "Senha deve ter ao menos 6 caracteres")
    private String senha;

    private Boolean ativo;
}
