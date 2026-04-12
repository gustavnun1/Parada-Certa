package com.paradacerta.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Cliente")
public class Cliente {

    @Id
    @Column(name = "cpf", length = 11, nullable = false)
    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String cpf;

    @Column(name = "nome", nullable = false)
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @Column(name = "senha", nullable = false)
    @NotBlank(message = "Senha é obrigatória")
    private String senha;

    @Column(name = "dataNascimento", nullable = false)
    @NotNull(message = "Data de nascimento é obrigatória")
    private LocalDate dataNascimento;

    @Column(name = "numeroCelular")
    private String numeroCelular;

    @Column(name = "placa")
    private String placa;

    @Column(name = "veiculo")
    private String veiculo;

    @Column(name = "premium", nullable = false)
    private boolean premium = false;
}
