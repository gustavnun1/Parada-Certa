package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Projeção pública do operador/admin (sem senhaHash). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponse {
    private Integer id;
    private String usuario;
    private String nomeCompleto;
    private String email;
    private String telefone;
    private String cpfMascarado;
    private Integer estacionamentoId;
    private Boolean ativo;
}
