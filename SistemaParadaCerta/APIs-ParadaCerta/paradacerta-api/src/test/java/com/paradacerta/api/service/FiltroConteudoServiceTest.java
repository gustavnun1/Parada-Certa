package com.paradacerta.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroConteudoServiceTest {

    private final FiltroConteudoService service = new FiltroConteudoService();

    @Test
    void aceitaTextoNuloVazioOuAvaliacaoLegitima() {
        assertThat(service.isConteudoApropriado(null)).isTrue();
        assertThat(service.isConteudoApropriado("   ")).isTrue();
        assertThat(service.isConteudoApropriado("Atendimento rapido, vaga coberta e preco justo.")).isTrue();
    }

    @Test
    void bloqueiaTermosComAcentosPontuacaoOuSubstituicaoDeLetras() {
        assertThat(service.isConteudoApropriado("Que m3rda de atendimento")).isFalse();
        assertThat(service.isConteudoApropriado("Servico porra!!!")).isFalse();
        assertThat(service.isConteudoApropriado("vou te matar quando voltar")).isFalse();
    }

    @Test
    void naoBloqueiaTermoQuandoApareceApenasComoParteDeOutraPalavra() {
        assertThat(service.isConteudoApropriado("A passagem pela rota foi tranquila.")).isTrue();
    }
}
