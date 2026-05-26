package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConteudoInvalidoException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoValidatorServiceTest {

    private final FotoValidatorService service = new FotoValidatorService();

    @Test
    void aceitaPngComExtensaoMimeEMagicBytesValidos() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile file = new MockMultipartFile("foto", "vaga.png", "image/png", png);

        FotoValidatorService.ResultadoValidacao result = service.validar(file);

        assertThat(result.mime).isEqualTo("image/png");
        assertThat(result.extensao).isEqualTo("png");
        assertThat(result.bytes).containsExactly(png);
    }

    @Test
    void rejeitaArquivoVazioOuSemNomeComoRequisicaoInvalida() {
        assertThatThrownBy(() -> service.validar(new MockMultipartFile("foto", "vaga.png", "image/png", new byte[0])))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Envie um arquivo de imagem.");

        assertThatThrownBy(() -> service.validar(new MockMultipartFile("foto", "", "image/png", new byte[] {1, 2, 3})))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Arquivo enviado sem nome.");
    }

    @Test
    void rejeitaExtensaoMimeOuConteudoIncompativel() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };

        assertThatThrownBy(() -> service.validar(new MockMultipartFile("foto", "vaga.gif", "image/gif", png)))
                .isInstanceOf(ConteudoInvalidoException.class)
                .hasMessageContaining("Use JPG, PNG ou WEBP.");

        assertThatThrownBy(() -> service.validar(new MockMultipartFile("foto", "vaga.jpg", "image/jpeg", png)))
                .isInstanceOf(ConteudoInvalidoException.class)
                .hasMessageContaining("extens")
                .hasMessageContaining("conte");
    }
}
