package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoValidatorTest {

    @Test
    void somenteDigitosRemoveMascaraERetornaNullQuandoNaoHaNumero() {
        assertThat(DocumentoValidator.somenteDigitos("123.456.789-09")).isEqualTo("12345678909");
        assertThat(DocumentoValidator.somenteDigitos("abc")).isNull();
        assertThat(DocumentoValidator.somenteDigitos(null)).isNull();
    }

    @Test
    void validarCpfAceitaCpfComDigitosVerificadoresValidos() {
        assertThatCode(() -> DocumentoValidator.validarCpf("12345678909"))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCpfRejeitaSequenciaRepetidaOuDigitoInvalido() {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf("11111111111"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("CPF invalido");

        assertThatThrownBy(() -> DocumentoValidator.validarCpf("12345678900"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("CPF invalido");
    }

    @Test
    void validarCnpjAceitaCnpjComDigitosVerificadoresValidos() {
        assertThatCode(() -> DocumentoValidator.validarCnpj("11222333000181"))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCnpjRejeitaTamanhoOuDigitoInvalido() {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj("123"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("CNPJ invalido: deve ter 14 digitos");

        assertThatThrownBy(() -> DocumentoValidator.validarCnpj("11222333000180"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("CNPJ invalido");
    }
}
