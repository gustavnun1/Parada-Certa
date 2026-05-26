package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Cadastro de operador/administrador adicional dentro de um estacionamento existente. */
@Data
public class OperadorCadastroRequest {

    @NotNull(message = "estacionamentoId é obrigatório")
    private Integer estacionamentoId;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    private String nomeCompleto;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 200)
    private String email;

    @Size(max = 20)
    private String telefone;

    @NotBlank(message = "CPF e obrigatorio")
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter 11 digitos numericos")
    private String cpf;

    /** Se vazio, derivado do email. */
    @Size(max = 50)
    private String usuario;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter ao menos 6 caracteres")
    private String senha;
}
