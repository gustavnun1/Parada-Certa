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

    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$",
            message = "Nome inválido. O nome não pode conter números nem caracteres especiais.")
    private String nomeCompleto;

    @Email(message = "Informe um email válido.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Informe um email válido.")
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
