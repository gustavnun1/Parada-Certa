package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta enviada após login do admin web.
 * Contém os dados essenciais para a sessão (sem token JWT — TCC).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private Integer id;
    private String usuario;
    private String nomeCompleto;
    private String email;
    private String telefone;
    private Integer estacionamentoId;
    private String estacionamentoNome;
}
