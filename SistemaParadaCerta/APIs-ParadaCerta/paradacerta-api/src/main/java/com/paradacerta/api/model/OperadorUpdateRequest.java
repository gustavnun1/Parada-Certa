package com.paradacerta.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/operador/{id} — atualização parcial. Campos null permanecem como estão.
 *
 * Observação LGPD: CPF NÃO pode ser alterado por esta rota (campo imutável após
 * o cadastro inicial). Se precisar corrigir, exclua o operador e crie um novo.
 */
@Data
public class OperadorUpdateRequest {

    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$",
            message = "Nome inválido. O nome não pode conter números nem caracteres especiais.")
    private String nome;

    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Usuário aceita apenas letras, números, ponto, hífen e underline")
    private String usuario;

    private Boolean ativo;

    // ── Dados sensíveis editáveis ────────────────────────────────────────────
    @Email(message = "E-mail inválido")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "E-mail inválido"
    )
    @Size(max = 200)
    private String email;

    @Size(min = 8, max = 20)
    private String telefone;

    // ── Endereço editável ────────────────────────────────────────────────────
    @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 dígitos numéricos")
    private String cep;

    @Size(max = 200)
    private String logradouro;

    @Size(max = 10)
    private String numero;

    @Size(max = 100)
    private String complemento;

    @Size(max = 100)
    private String bairro;

    @Size(max = 100)
    private String cidade;

    @Size(min = 2, max = 2, message = "UF deve ter 2 letras")
    private String uf;
}
