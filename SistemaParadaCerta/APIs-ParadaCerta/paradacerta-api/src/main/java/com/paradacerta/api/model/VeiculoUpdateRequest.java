package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VeiculoUpdateRequest {

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    private String modeloVeiculo;

    @NotBlank(message = "Cor do veículo é obrigatória")
    private String corVeiculo;
}
