package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VeiculoInput {

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String cpf;

    @NotBlank(message = "Placa é obrigatória")
    @Size(min = 1, max = 7, message = "Placa inválida")
    private String placa;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    private String modeloVeiculo;

    @NotBlank(message = "Cor do veículo é obrigatória")
    private String corVeiculo;
}
