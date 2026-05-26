package com.paradacerta.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "AdmEstacionamento")
public class AdmEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    /** Hash BCrypt da senha. NUNCA serializado na resposta. */
    @JsonIgnore
    @Column(name = "senhaHash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "nomeCompleto", nullable = false, length = 100)
    private String nomeCompleto;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /** Coluna adicionada via 02-ALTER. Login web é por email. */
    @Column(name = "email", length = 200)
    private String email;

    /** Coluna adicionada via 02-ALTER. Cadastrado pelo responsável. */
    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "dataNascimento")
    private LocalDate dataNascimento;
}
