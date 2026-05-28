package com.paradacerta.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradacerta.api.exception.ConteudoInvalidoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Moderação de imagens via Google Vision SafeSearch Detection.
 *
 * <h2>Modo "TCC sem chave"</h2>
 * Se a propriedade {@code paradacerta.gcp.vision.api-key} estiver vazia, este service
 * <b>aprova todas as imagens</b> com um WARN no log — adequado para desenvolvimento
 * local sem custo na Google Cloud.
 *
 * <h2>Modo produção</h2>
 * Com a chave preenchida, envia a imagem em base64 para
 * {@code https://vision.googleapis.com/v1/images:annotate?key=...} e bloqueia o
 * upload se {@code adult}, {@code violence} ou {@code racy} vierem como
 * {@code LIKELY} ou {@code VERY_LIKELY}. {@code medical} e {@code spoof} são
 * logados mas não bloqueiam.
 *
 * <h2>Fail-closed</h2>
 * Em caso de erro de rede, timeout, key inválida ou resposta inesperada, a imagem
 * é REJEITADA (lança {@link ConteudoInvalidoException}). Isto evita uploads sem
 * verificação quando o serviço está indisponível.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeracaoImagemService {

    private static final String ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";

    /** Categorias que BLOQUEIAM o upload quando vêm LIKELY/VERY_LIKELY. */
    private static final Set<String> CATEGORIAS_BLOQUEANTES =
            Set.of("adult", "violence", "racy");

    /** Categorias apenas logadas (sem bloquear). */
    private static final Set<String> CATEGORIAS_INFORMATIVAS =
            Set.of("medical", "spoof");

    /** Valores que indicam alta probabilidade de conteúdo sensível. */
    private static final Set<String> NIVEIS_BLOQUEANTES =
            Set.of("LIKELY", "VERY_LIKELY");

    @Value("${paradacerta.gcp.vision.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Modera a imagem. Lança {@link ConteudoInvalidoException} se rejeitada.
     * Retorna silenciosamente se aprovada.
     *
     * <p><b>Mensagens distintas para erro real vs. fail-closed</b>: quando a
     * Vision API responde dizendo que a imagem é sensível ({@code adult/violence/racy}
     * em {@code LIKELY/VERY_LIKELY}), a mensagem identifica claramente conteúdo
     * sensível. Quando a chamada falha (timeout, quota, key inválida, resposta
     * malformada), a mensagem é diferente — assim o usuário entende que pode
     * tentar de novo e não confunde com um bloqueio definitivo da foto.
     */
    @SuppressWarnings("unchecked")
    public void moderar(byte[] imagemBytes) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("MODERACAO_IMAGEM_DESATIVADA: paradacerta.gcp.vision.api-key vazio. " +
                    "Imagem aceita sem verificacao SafeSearch (modo TCC/dev).");
            return;
        }

        final String mensagemSensivel =
                "Não foi possível enviar esta imagem, pois ela pode conter conteúdo sensível, " +
                "inadequado ou informações pessoais. Selecione outra foto do estabelecimento.";
        final String mensagemFalhaServico =
                "Não foi possível verificar a imagem no momento. Tente novamente em instantes.";

        // Monta corpo: { requests: [{ image: { content: <base64> }, features: [{ type: SAFE_SEARCH_DETECTION }] }] }
        Map<String, Object> request = new LinkedHashMap<>();
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("content", Base64.getEncoder().encodeToString(imagemBytes));
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "SAFE_SEARCH_DETECTION");
        request.put("image", image);
        request.put("features", List.of(feature));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requests", List.of(request));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> resposta;
        try {
            resposta = restTemplate.postForObject(
                    ENDPOINT + "?key=" + apiKey, entity, Map.class
            );
        } catch (RestClientException ex) {
            log.error("Falha ao chamar Google Vision (fail-closed; rejeitando imagem): {}", ex.getMessage());
            throw new ConteudoInvalidoException(mensagemFalhaServico);
        }

        if (resposta == null) {
            log.error("Google Vision retornou null (fail-closed; rejeitando imagem).");
            throw new ConteudoInvalidoException(mensagemFalhaServico);
        }

        List<Map<String, Object>> respostas = (List<Map<String, Object>>) resposta.get("responses");
        if (respostas == null || respostas.isEmpty()) {
            log.error("Google Vision sem campo 'responses' (fail-closed).");
            throw new ConteudoInvalidoException(mensagemFalhaServico);
        }

        Map<String, Object> primeira = respostas.get(0);
        Map<String, Object> erro = (Map<String, Object>) primeira.get("error");
        if (erro != null) {
            log.error("Google Vision retornou erro (fail-closed): {}", erro);
            throw new ConteudoInvalidoException(mensagemFalhaServico);
        }

        Map<String, Object> safe = (Map<String, Object>) primeira.get("safeSearchAnnotation");
        if (safe == null) {
            log.error("Google Vision sem safeSearchAnnotation (fail-closed).");
            throw new ConteudoInvalidoException(mensagemFalhaServico);
        }

        boolean rejeitada = false;
        StringBuilder motivos = new StringBuilder();
        for (Map.Entry<String, Object> e : safe.entrySet()) {
            String categoria = e.getKey().toLowerCase(Locale.ROOT);
            String valor = String.valueOf(e.getValue());
            if (CATEGORIAS_BLOQUEANTES.contains(categoria) && NIVEIS_BLOQUEANTES.contains(valor)) {
                rejeitada = true;
                if (motivos.length() > 0) motivos.append(", ");
                motivos.append(categoria).append("=").append(valor);
            } else if (CATEGORIAS_INFORMATIVAS.contains(categoria) && NIVEIS_BLOQUEANTES.contains(valor)) {
                log.info("SafeSearch sinalizou categoria informativa: {}={}", categoria, valor);
            }
        }

        if (rejeitada) {
            log.warn("Imagem rejeitada pelo SafeSearch: {}", motivos);
            throw new ConteudoInvalidoException(mensagemSensivel);
        }

        log.debug("Imagem aprovada pelo SafeSearch.");
    }
}
