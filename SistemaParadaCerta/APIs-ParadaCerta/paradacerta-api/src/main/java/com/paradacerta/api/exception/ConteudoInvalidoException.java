package com.paradacerta.api.exception;

/**
 * Lançada quando um conteúdo (imagem, comentário etc.) é recusado pelas regras
 * de validação/moderação. Mapeada como HTTP 422 Unprocessable Entity pelo
 * {@link GlobalExceptionHandler}.
 *
 * Use esta exceção quando o pedido é sintaticamente válido (campos preenchidos,
 * formato aceito) mas o CONTEÚDO é recusado pela política da plataforma:
 *  - Imagem rejeitada por Google Vision SafeSearch (adult/violence/racy).
 *  - Imagem com magic bytes inconsistentes com o tipo declarado.
 *  - Limite de fotos do plano atingido.
 */
public class ConteudoInvalidoException extends RuntimeException {
    public ConteudoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
