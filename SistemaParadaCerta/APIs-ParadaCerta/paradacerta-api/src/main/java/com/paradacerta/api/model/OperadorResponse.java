package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Operador do kiosk em listagens. Sem dados sensíveis completos.
 *
 * LGPD:
 *  - Senha NUNCA é exposta (BCrypt fica apenas no servidor).
 *  - CPF mascarado como "***.***.***-XX" (mostra só os 2 últimos dígitos).
 *  - Para obter o CPF completo (tela de detalhe/edição do próprio operador),
 *    use {@link OperadorDetailResponse} via GET /api/operador/{id}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperadorResponse {
    private Integer id;
    private Integer estacionamentoId;
    private String nome;
    private String usuario;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    /** CPF mascarado para listagens. Formato fixo: "***.***.***-XX". */
    private String cpfMascarado;

    private String email;
    private String telefone;

    /** Cidade e UF — mostradas em listagem (não é dado sensível). */
    private String cidade;
    private String uf;
}
