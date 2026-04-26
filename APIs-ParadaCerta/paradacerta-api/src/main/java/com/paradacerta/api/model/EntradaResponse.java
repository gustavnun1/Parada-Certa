package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EntradaResponse {
    private boolean sucesso;
    private String mensagem;
    private String sessaoId;
    private LocalDateTime horaEntrada;
}
