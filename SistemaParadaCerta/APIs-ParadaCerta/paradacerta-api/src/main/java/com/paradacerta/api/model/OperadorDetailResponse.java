package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Detalhe completo do operador para a tela de edição (GET /api/operador/{id}).
 *
 * LGPD: contém CPF e endereço completos. Use apenas nesta rota individual —
 * em listagens, use {@link OperadorResponse}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperadorDetailResponse {

    private Integer id;
    private Integer estacionamentoId;
    private String nome;
    private String usuario;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    // Dados pessoais
    private String cpf;
    private String email;
    private String telefone;

    // Endereço estruturado
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
}
