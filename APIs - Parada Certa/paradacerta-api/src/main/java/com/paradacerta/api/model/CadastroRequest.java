package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

// DTO recebido pelo app Android com todos os dados de cadastro
@Data
public class CadastroRequest {

    // --- Dados Pessoais ---
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String cpf;

    @NotBlank(message = "Data de nascimento é obrigatória")
    private String dataNascimento; // formato: dd/MM/yyyy

    private String numeroCelular;

    // --- Dados do Veículo ---
    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    private String modeloVeiculo;

    @NotBlank(message = "Cor do veículo é obrigatória")
    private String corVeiculo;

    // --- Endereço ---
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
