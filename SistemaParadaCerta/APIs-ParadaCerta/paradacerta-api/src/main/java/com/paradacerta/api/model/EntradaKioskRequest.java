package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EntradaKioskRequest {
    @NotBlank(message = "CPF do usuario e obrigatorio")
    private String cpfUsuario;

    @NotBlank(message = "Token do QR Code e obrigatorio")
    private String token;

    @NotBlank(message = "Placa do veiculo e obrigatoria")
    private String placa;
}
