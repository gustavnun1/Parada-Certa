package com.paradacerta.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Operador de balcão (kiosk).
 *
 * Separado de {@link AdmEstacionamento}, que controla o login do painel WEB.
 * Cada estacionamento tem N operadores; o admin web do estacionamento cria/edita/desativa.
 *
 * Scripts SQL:
 *  - 05-CREATE-OperadorEstacionamento.sql   (criação inicial)
 *  - 06-ALTER-OperadorEstacionamento-DadosPessoais.sql  (LGPD: cpf, email, telefone, endereço)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "OperadorEstacionamento")
public class OperadorEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    /** Hash BCrypt. Nunca trafega em JSON. */
    @JsonIgnore
    @Column(name = "senhaHash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "criadoEm", nullable = false)
    private LocalDateTime criadoEm;

    // ── Dados pessoais (LGPD) — Script 06 ────────────────────────────────────
    /** CPF de 11 dígitos. NUNCA serializado em JSON: o front recebe somente o campo
     *  mascarado em {@code OperadorResponse.cpfMascarado}, ou o CPF completo em
     *  {@code OperadorDetailResponse} (rota de detalhe individual). */
    @JsonIgnore
    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    // ── Endereço estruturado — Script 06 ─────────────────────────────────────
    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "logradouro", length = 200)
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;
}
