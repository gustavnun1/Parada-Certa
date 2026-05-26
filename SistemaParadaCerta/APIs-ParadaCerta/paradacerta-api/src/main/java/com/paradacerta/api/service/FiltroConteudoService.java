package com.paradacerta.api.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filtro local de conteúdo ofensivo em português.
 * Funciona sem nenhuma API externa — é a camada base de moderação.
 *
 * Normaliza o texto (remove acentos, lowercase) antes de verificar,
 * evitando burlar o filtro com letras especiais (ex: "p0rra", "meréda").
 */
@Service
public class FiltroConteudoService {

    // ── Lista de termos bloqueados (português BR) ─────────────────────────────

    private static final Set<String> TERMOS_BLOQUEADOS = new HashSet<>();

    static {
        // Palavrões comuns
        TERMOS_BLOQUEADOS.add("porra");
        TERMOS_BLOQUEADOS.add("merda");
        TERMOS_BLOQUEADOS.add("caralho");
        TERMOS_BLOQUEADOS.add("buceta");
        TERMOS_BLOQUEADOS.add("puta");
        TERMOS_BLOQUEADOS.add("arrombado");
        TERMOS_BLOQUEADOS.add("fodase");
        TERMOS_BLOQUEADOS.add("foda");
        TERMOS_BLOQUEADOS.add("fdp");
        TERMOS_BLOQUEADOS.add("vsf");
        TERMOS_BLOQUEADOS.add("vtnc");
        TERMOS_BLOQUEADOS.add("vai tomar");
        TERMOS_BLOQUEADOS.add("cuzao");
        TERMOS_BLOQUEADOS.add("cuz");
        TERMOS_BLOQUEADOS.add("punheta");
        TERMOS_BLOQUEADOS.add("piroca");
        TERMOS_BLOQUEADOS.add("rola");
        TERMOS_BLOQUEADOS.add("xoxota");
        TERMOS_BLOQUEADOS.add("boceta");
        TERMOS_BLOQUEADOS.add("cuzinho");
        TERMOS_BLOQUEADOS.add("corno");
        TERMOS_BLOQUEADOS.add("safado");
        TERMOS_BLOQUEADOS.add("safada");
        TERMOS_BLOQUEADOS.add("vagabundo");
        TERMOS_BLOQUEADOS.add("vagabunda");
        TERMOS_BLOQUEADOS.add("piranha");
        TERMOS_BLOQUEADOS.add("vadia");
        TERMOS_BLOQUEADOS.add("puto");
        TERMOS_BLOQUEADOS.add("babaca");
        TERMOS_BLOQUEADOS.add("idiota");
        TERMOS_BLOQUEADOS.add("imbecil");
        TERMOS_BLOQUEADOS.add("retardado");
        TERMOS_BLOQUEADOS.add("otario");
        TERMOS_BLOQUEADOS.add("otaria");
        TERMOS_BLOQUEADOS.add("filha da puta");
        TERMOS_BLOQUEADOS.add("filho da puta");
        TERMOS_BLOQUEADOS.add("sua mae");
        TERMOS_BLOQUEADOS.add("sua mae");
        TERMOS_BLOQUEADOS.add("desgraca");
        TERMOS_BLOQUEADOS.add("desgracado");
        TERMOS_BLOQUEADOS.add("desgracada");

        // Variações com substituição de letras comuns
        TERMOS_BLOQUEADOS.add("p0rra");
        TERMOS_BLOQUEADOS.add("m3rda");
        TERMOS_BLOQUEADOS.add("car4lho");
        TERMOS_BLOQUEADOS.add("fud3");
        TERMOS_BLOQUEADOS.add("put4");

        // Slurs e discriminação
        TERMOS_BLOQUEADOS.add("viado");
        TERMOS_BLOQUEADOS.add("bicha");
        TERMOS_BLOQUEADOS.add("sapatao");
        TERMOS_BLOQUEADOS.add("traveco");
        TERMOS_BLOQUEADOS.add("macaco");
        TERMOS_BLOQUEADOS.add("macaca");
        TERMOS_BLOQUEADOS.add("crioulo");
        TERMOS_BLOQUEADOS.add("crioula");

        // Ameaças e violência
        TERMOS_BLOQUEADOS.add("vou te matar");
        TERMOS_BLOQUEADOS.add("te matar");
        TERMOS_BLOQUEADOS.add("matar voce");
        TERMOS_BLOQUEADOS.add("vou te bater");
        TERMOS_BLOQUEADOS.add("tomar porrada");
        TERMOS_BLOQUEADOS.add("quebrar a cara");

        // Conteúdo sexual explícito
        TERMOS_BLOQUEADOS.add("sexo");
        TERMOS_BLOQUEADOS.add("transar");
        TERMOS_BLOQUEADOS.add("gozar");
        TERMOS_BLOQUEADOS.add("chupeta");
        TERMOS_BLOQUEADOS.add("chupar");
        TERMOS_BLOQUEADOS.add("ejacular");
        TERMOS_BLOQUEADOS.add("masturbacao");
        TERMOS_BLOQUEADOS.add("pornografia");
        TERMOS_BLOQUEADOS.add("porno");
    }

    // Padrão para extrair palavras (ignora pontuação)
    private static final Pattern PALAVRAS = Pattern.compile("[\\p{L}\\d]+");

    /**
     * Verifica se o texto contém conteúdo bloqueado.
     *
     * @return true  → conteúdo limpo
     *         false → conteúdo ofensivo detectado
     */
    public boolean isConteudoApropriado(String texto) {
        if (texto == null || texto.isBlank()) return true;

        String normalizado = normalizar(texto);

        // Verifica palavras individuais
        var matcher = PALAVRAS.matcher(normalizado);
        while (matcher.find()) {
            if (TERMOS_BLOQUEADOS.contains(matcher.group())) {
                return false;
            }
        }

        // Verifica frases compostas (ex: "filho da puta", "vai tomar")
        for (String termo : TERMOS_BLOQUEADOS) {
            if (termo.contains(" ") && normalizado.contains(termo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Normaliza o texto para comparação:
     * - Converte para lowercase
     * - Remove acentos (ex: "véio" → "veio")
     * - Remove pontuação extra
     */
    private String normalizar(String texto) {
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return semAcentos.toLowerCase();
    }
}
