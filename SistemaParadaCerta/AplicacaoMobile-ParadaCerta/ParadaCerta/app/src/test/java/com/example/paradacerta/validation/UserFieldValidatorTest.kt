package com.example.paradacerta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserFieldValidatorTest {

    @Test
    fun nomeNaoAceitaNumeros() {
        assertEquals("O nome não pode conter números.", UserFieldValidator.validarNome("Maria 123"))
        assertNull(UserFieldValidator.validarNome("Maria Silva"))
    }

    @Test
    fun senhaPrecisaSerForte() {
        assertEquals(
            "A senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais.",
            UserFieldValidator.validarSenha("Senha123")
        )
        assertNull(UserFieldValidator.validarSenha("Senha@123"))
    }

    @Test
    fun telefonePrecisaTerDddENumeroValido() {
        assertEquals(
            "O telefone deve possuir DDD e número válidos.",
            UserFieldValidator.validarTelefone("11999")
        )
        assertNull(UserFieldValidator.validarTelefone("(11) 99999-9999"))
    }

    @Test
    fun dataInvalidaEhBloqueada() {
        assertEquals("Data de nascimento inválida.", UserFieldValidator.validarDataNascimento("31/02/2000"))
        assertNull(UserFieldValidator.validarDataNascimento("20/05/2000"))
    }

    @Test
    fun cepPrecisaTerOitoDigitos() {
        assertEquals("CEP inválido.", UserFieldValidator.validarCep("01001"))
        assertNull(UserFieldValidator.validarCep("01001000"))
    }
}
