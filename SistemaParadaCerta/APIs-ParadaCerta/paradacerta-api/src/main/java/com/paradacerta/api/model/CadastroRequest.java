package com.paradacerta.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO recebido pelo app Android com todos os dados de cadastro.
@Data
public class CadastroRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 80, message = "O nome deve possuir entre 3 e 80 caracteres.")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$", message = "Nome inválido.")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "Email inválido.")
    @Size(max = 120, message = "O email deve possuir no máximo 120 caracteres.")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve possuir entre 8 e 100 caracteres.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "A senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais."
    )
    private String senha;

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotBlank(message = "Data de nascimento é obrigatória")
    private String dataNascimento;

    @NotBlank(message = "Telefone é obrigatório")
    private String numeroCelular;

    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    private String modeloVeiculo;

    @NotBlank(message = "Cor do veículo é obrigatória")
    private String corVeiculo;

    @NotBlank(message = "CEP é obrigatório")
    private String cep;

    @NotBlank(message = "Logradouro é obrigatório")
    private String logradouro;

    @NotBlank(message = "Número é obrigatório")
    private String numero;

    private String complemento;

    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;

    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres (UF)")
    private String estado;
}
