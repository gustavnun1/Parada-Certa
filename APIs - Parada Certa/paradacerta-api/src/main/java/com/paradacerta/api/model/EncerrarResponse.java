package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EncerrarResponse {
    private boolean sucesso;
    private String mensagem;
    private Double valorPago;
    private String duracao;
}
