package com.paradacerta.api.exception;

/**
 * Lançada quando o usuário/estacionamento não tem permissão para acessar um
 * recurso (ex.: plano não cobre o recurso, trial expirado, recurso Premium etc.).
 *
 * Mapeada como HTTP 403 Forbidden pelo {@link GlobalExceptionHandler}.
 */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
