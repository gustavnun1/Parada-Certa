package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntradaAppRequest {

    @NotBlank(message = "CPF do usuário é obrigatório")
    private String cpfUsuario;

    @NotNull(message = "ID do estacionamento é obrigatório")
    private Integer estacionamentoId;

    @NotBlank(message = "Placa do veículo é obrigatória")
    private String placa;
}
