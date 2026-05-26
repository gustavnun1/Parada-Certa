package com.paradacerta.api.exception;

import com.paradacerta.api.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleNotFound(UsuarioNaoEncontradoException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse handleUnauthorized(CredenciaisInvalidasException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleBadRequest(RequisicaoInvalidaException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse handleConflict(ConflictException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse handleAcessoNegado(AcessoNegadoException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(ConteudoInvalidoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse handleConteudoInvalido(ConteudoInvalidoException ex) {
        return ApiResponse.erro(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ApiResponse.erro("Arquivo excede o tamanho máximo permitido (5 MB).");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos");
        return ApiResponse.erro(mensagem);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGeneric(Exception ex) {
        return ApiResponse.erro("Erro interno do servidor");
    }
}
