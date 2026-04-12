package com.paradacerta.api.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String mensagem) {
        super(mensagem);
    }
}
