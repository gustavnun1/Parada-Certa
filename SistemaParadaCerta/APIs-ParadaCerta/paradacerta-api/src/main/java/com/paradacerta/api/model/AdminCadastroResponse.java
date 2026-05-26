package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta do cadastro do admin web (estabelecimento + responsável).
 * Já vem com o admin "logado": o front pode pular o login após cadastro.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCadastroResponse {
    private Integer adminId;
    private String usuario;
    private String email;
    private String nomeCompleto;
    private Integer estacionamentoId;
    private String estacionamentoNome;
}
