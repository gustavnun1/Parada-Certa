package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserFieldValidatorTest {

    private static final DateTimeFormatter DATA_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    @Test
    void nomeNaoAceitaNumeros() {
        RequisicaoInvalidaException ex = assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.normalizarNome("Joao 123")
        );
        assertEquals("O nome não pode conter números.", ex.getMessage());
        assertEquals("Joao Silva", UserFieldValidator.normalizarNome("  Joao   Silva  "));
    }

    @Test
    void senhaPrecisaSerForte() {
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.validarSenha("Senha123", true)
        );
        assertDoesNotThrow(() -> UserFieldValidator.validarSenha("Senha@123", true));
    }

    @Test
    void telefoneEDataSaoValidados() {
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.normalizarTelefone("11999")
        );
        assertEquals("11999999999", UserFieldValidator.normalizarTelefone("(11) 99999-9999"));

        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.parseDataNascimento("31/02/2000")
        );
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.parseDataNascimento("01/01/2030")
        );
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.parseDataNascimento(LocalDate.now().minusYears(18).plusDays(1).format(DATA_FORMAT))
        );
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.parseDataNascimento(LocalDate.now().minusYears(120).minusDays(1).format(DATA_FORMAT))
        );
        assertDoesNotThrow(() -> UserFieldValidator.parseDataNascimento("20/05/2000"));
    }

    @Test
    void cepPrecisaTerOitoDigitos() {
        assertThrows(
                RequisicaoInvalidaException.class,
                () -> UserFieldValidator.normalizarCep("01001")
        );
        assertEquals("01001000", UserFieldValidator.normalizarCep("01001-000"));
    }
}
