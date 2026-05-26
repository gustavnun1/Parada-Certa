package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PagamentoResponse {
    private boolean sucesso;
    private String mensagem;
    private Double valorPago;
    private LocalDateTime horaSaida;
}
