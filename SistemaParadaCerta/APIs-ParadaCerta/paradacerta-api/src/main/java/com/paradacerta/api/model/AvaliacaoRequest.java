package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AvaliacaoRequest {

    @NotNull(message = "ID do estacionamento é obrigatório")
    private Integer estacionamentoId;

    @NotBlank(message = "CPF do cliente é obrigatório")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String clienteCpf;

    @NotNull(message = "Nota é obrigatória")
    @Min(value = 1, message = "Nota mínima é 1")
    @Max(value = 5, message = "Nota máxima é 5")
    private Integer nota;

    private String comentario; // opcional
}
