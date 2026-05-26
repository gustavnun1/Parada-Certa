package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Operador do kiosk. Tabela dedicada criada pelo script 05-CREATE-OperadorEstacionamento.sql.
 * Substitui o uso de AdmEstacionamento para o login do balcão.
 */
@Entity
@Table(name = "OperadorEstacionamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "senhaHash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "criadoEm", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

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
