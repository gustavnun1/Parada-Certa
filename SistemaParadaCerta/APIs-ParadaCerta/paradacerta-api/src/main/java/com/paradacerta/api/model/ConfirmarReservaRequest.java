package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body do POST /api/reserva/confirmar
 * - qrCode: conteúdo escaneado pelo mobile (UUID gerado na criação da reserva)
 * - cpf: motorista autenticado no app, usado para validar propriedade da reserva
 */
@Data
public class ConfirmarReservaRequest {

    @NotBlank(message = "QR Code é obrigatório")
    private String qrCode;

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;
}
