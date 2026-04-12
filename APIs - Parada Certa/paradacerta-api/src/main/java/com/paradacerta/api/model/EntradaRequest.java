package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntradaRequest {

    @NotBlank(message = "QR Code é obrigatório")
    private String qrCode;

    @NotBlank(message = "CPF do usuário é obrigatório")
    private String cpfUsuario;

    @NotNull(message = "ID do estacionamento é obrigatório")
    private Integer estacionamentoId;
}
