package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConteudoInvalidoException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Validação local (sem rede) de uma imagem enviada por upload:
 *  1. Extensão (whitelist).
 *  2. Tipo MIME declarado (whitelist).
 *  3. Tamanho máximo (5 MB).
 *  4. Magic bytes — assinatura binária real do arquivo.
 *
 * Lança {@link ConteudoInvalidoException} (HTTP 422) com mensagem amigável.
 * Erros estritamente sintáticos (arquivo vazio, sem nome) viram {@link RequisicaoInvalidaException} (400).
 */
@Slf4j
@Service
public class FotoValidatorService {

    public static final long MAX_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> MIMES_VALIDOS =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> EXTENSOES_VALIDAS =
            Set.of("jpg", "jpeg", "png", "webp");

    /**
     * Resultado da validação local — devolve o MIME canônico detectado pelos magic bytes,
     * que pode diferir do MIME declarado (ex.: {@code .jpg} chega como {@code image/pjpeg}).
     */
    public ResultadoValidacao validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie um arquivo de imagem.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            throw new RequisicaoInvalidaException("Arquivo enviado sem nome.");
        }

        String ext = extrairExtensao(nome);
        if (!EXTENSOES_VALIDAS.contains(ext)) {
            throw new ConteudoInvalidoException(
                "Formato de imagem não suportado. Use JPG, PNG ou WEBP."
            );
        }

        String mimeDeclarado = arquivo.getContentType();
        if (mimeDeclarado == null || !MIMES_VALIDOS.contains(mimeDeclarado.toLowerCase(Locale.ROOT))) {
            throw new ConteudoInvalidoException(
                "Tipo de imagem não suportado. Use JPG, PNG ou WEBP."
            );
        }

        if (arquivo.getSize() <= 0) {
            throw new RequisicaoInvalidaException("Arquivo vazio.");
        }
        if (arquivo.getSize() > MAX_BYTES) {
            throw new ConteudoInvalidoException(
                "A imagem é muito grande. O limite é de 5 MB."
            );
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            log.error("Falha ao ler bytes do upload", e);
            throw new ConteudoInvalidoException("Não foi possível processar a imagem enviada.");
        }

        String mimeReal = detectarMimeReal(bytes);
        if (mimeReal == null) {
            throw new ConteudoInvalidoException(
                "Não foi possível enviar esta imagem, pois ela pode conter conteúdo sensível, " +
                "inadequado ou informações pessoais. Selecione outra foto do estabelecimento."
            );
        }

        // Coerência entre MIME declarado e magic bytes (impede arquivo renomeado)
        if (!compatibiliza(mimeDeclarado, mimeReal)) {
            log.warn("MIME declarado ({}) divergente do detectado ({}) — upload rejeitado.", mimeDeclarado, mimeReal);
            throw new ConteudoInvalidoException(
                "O arquivo enviado não parece ser uma imagem válida (extensão diferente do conteúdo)."
            );
        }

        return new ResultadoValidacao(mimeReal, ext, bytes);
    }

    /** Detecta a assinatura binária. Retorna o MIME canônico ou null se desconhecido. */
    private static String detectarMimeReal(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && (bytes[4] & 0xFF) == 0x0D
                && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0x0A) {
            return "image/png";
        }

        // WEBP: bytes 0-3 = "RIFF", bytes 8-11 = "WEBP"
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }

        return null;
    }

    private static boolean compatibiliza(String declarado, String real) {
        if (declarado == null || real == null) return false;
        String d = declarado.toLowerCase(Locale.ROOT);
        if (d.equals(real)) return true;
        // Aceita variações: image/pjpeg <-> image/jpeg
        if ((d.equals("image/jpeg") || d.equals("image/pjpeg")) && real.equals("image/jpeg")) return true;
        return false;
    }

    private static String extrairExtensao(String nomeArquivo) {
        int p = nomeArquivo.lastIndexOf('.');
        if (p < 0 || p == nomeArquivo.length() - 1) return "";
        return nomeArquivo.substring(p + 1).toLowerCase(Locale.ROOT);
    }

    public static class ResultadoValidacao {
        public final String mime;
        public final String extensao;
        public final byte[] bytes;

        public ResultadoValidacao(String mime, String extensao, byte[] bytes) {
            this.mime = mime;
            this.extensao = extensao;
            this.bytes = bytes;
        }
    }
}
