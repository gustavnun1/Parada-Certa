package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Cadastro de operador do kiosk (POST /api/operador).
 *
 * LGPD: todos os dados pessoais são obrigatórios para identificar o operador
 * responsável pelas ações no balcão; senha é gravada em BCrypt; CPF validado
 * com algoritmo dos dois dígitos verificadores no service.
 */
@Data
public class OperadorRequest {

    @NotNull(message = "estacionamentoId é obrigatório")
    private Integer estacionamentoId;

    // ── Identificação ────────────────────────────────────────────────────────
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "Usuário é obrigatório")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Usuário aceita apenas letras, números, ponto, hífen e underline")
    private String usuario;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter ao menos 6 caracteres")
    private String senha;

    // ── Dados sensíveis (LGPD) ───────────────────────────────────────────────
    /** Apenas 11 dígitos numéricos. Os dois últimos verificadores são validados no service. */
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter 11 dígitos numéricos (sem pontos ou traços)")
    private String cpf;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "E-mail inválido"
    )
    @Size(max = 200)
    private String email;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(min = 8, max = 20)
    private String telefone;

    // ── Endereço (LGPD) ──────────────────────────────────────────────────────
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 dígitos numéricos")
    private String cep;

    @NotBlank(message = "Logradouro é obrigatório")
    @Size(max = 200)
    private String logradouro;

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 10)
    private String numero;

    @Size(max = 100)
    private String complemento;

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100)
    private String bairro;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100)
    private String cidade;

    @NotBlank(message = "UF é obrigatória")
    @Size(min = 2, max = 2, message = "UF deve ter 2 letras")
    private String uf;
}
