package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntradaResponse {
    private boolean sucesso;
    private String mensagem;
    private String sessaoId;
    private LocalDateTime horaEntrada;
    /**
     * Chave Pix do estacionamento, necessária ao motorista para o pagamento
     * via Home após entrar. Pode ser nula quando o estacionamento não tiver
     * Pix configurado.
     */
    private String pixKey;
}
