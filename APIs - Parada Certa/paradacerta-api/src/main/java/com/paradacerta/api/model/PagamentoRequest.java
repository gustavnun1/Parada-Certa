package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagamentoRequest {

    @NotBlank(message = "QR Code é obrigatório")
    private String qrCode;

    @NotNull(message = "Valor pago é obrigatório")
    private Double valorPago;
}
