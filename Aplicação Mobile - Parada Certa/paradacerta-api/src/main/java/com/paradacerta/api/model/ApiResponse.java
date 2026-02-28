package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

// Resposta padrão para todas as chamadas da API
@Data
@AllArgsConstructor
public class ApiResponse {
    private boolean sucesso;
    private String mensagem;

    public static ApiResponse ok(String mensagem) {
        return new ApiResponse(true, mensagem);
    }

    public static ApiResponse erro(String mensagem) {
        return new ApiResponse(false, mensagem);
    }
}
