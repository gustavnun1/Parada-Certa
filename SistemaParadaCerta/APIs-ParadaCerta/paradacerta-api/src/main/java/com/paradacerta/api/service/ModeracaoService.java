package com.paradacerta.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Segunda camada de moderação: chama a OpenAI Moderation API.
 * Opera em fail-open — se a API falhar, o filtro local ainda protege.
 *
 * Configure: moderation.openai.api-key em application.properties
 */
@Service
public class ModeracaoService {

    private static final Logger log = LoggerFactory.getLogger(ModeracaoService.class);
    private static final String OPENAI_URL = "https://api.openai.com/v1/moderations";

    @Value("${moderation.openai.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    /**
     * @return true  → conteúdo adequado ou API indisponível (filtro local já rodou antes)
     *         false → OpenAI sinalizou conteúdo inapropriado
     */
    public boolean isConteudoApropriado(String texto) {
        if (texto == null || texto.isBlank()) return true;

        if (apiKey.isBlank()) {
            log.debug("[Moderação API] Chave não configurada — usando apenas filtro local.");
            return true;
        }

        try {
            ModeracaoResponse response = restClient.post()
                    .uri(OPENAI_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("input", texto))
                    .retrieve()
                    .body(ModeracaoResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("[Moderação API] Resposta vazia — filtro local já aplicado.");
                return true;
            }

            ResultItem resultado = response.getResults().get(0);

            if (resultado.isFlagged()) {
                List<String> categorias = resultado.getCategories().entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .toList();
                log.warn("[Moderação API] Conteúdo rejeitado pela OpenAI — categorias: {}", categorias);
                return false;
            }

            log.debug("[Moderação API] Conteúdo aprovado pela OpenAI.");
            return true;

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("[Moderação API] Chave inválida ou revogada (401). " +
                      "Gere uma nova chave em platform.openai.com/api-keys e atualize application.properties.");
            return true; // filtro local já aplicado antes desta chamada

        } catch (Exception e) {
            log.warn("[Moderação API] API indisponível ({}). Filtro local ainda ativo.", e.getMessage());
            return true;
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ModeracaoResponse {
        private List<ResultItem> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ResultItem {
        private boolean flagged;
        private Map<String, Boolean> categories;

        @JsonProperty("category_scores")
        private Map<String, Double> categoryScores;
    }
}
