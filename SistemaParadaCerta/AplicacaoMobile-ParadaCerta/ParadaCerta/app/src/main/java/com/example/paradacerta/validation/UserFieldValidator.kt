package com.example.paradacerta.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object UserFieldValidator {
    private val nomeRegex = Regex("^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$")
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    fun somenteDigitos(valor: String): String = valor.filter { it.isDigit() }

    fun validarNome(nome: String): String? {
        val limpo = nome.trim().replace(Regex("\\s+"), " ")
        return when {
            limpo.isBlank() -> "Informe o nome."
            limpo.length < 3 -> "O nome deve possuir no mínimo 3 caracteres."
            limpo.length > 80 -> "O nome deve possuir no máximo 80 caracteres."
            limpo.any { it.isDigit() } -> "O nome não pode conter números."
            !nomeRegex.matches(limpo) -> "Nome inválido."
            else -> null
        }
    }

    fun validarEmail(email: String): String? {
        val limpo = email.trim()
        return when {
            limpo.isBlank() -> "Informe um email válido."
            limpo.length > 120 -> "O email deve possuir no máximo 120 caracteres."
            !emailRegex.matches(limpo) -> "Email inválido."
            else -> null
        }
    }

    fun validarSenha(senha: String, obrigatoria: Boolean = true): String? {
        if (!obrigatoria && senha.isBlank()) return null
        return when {
            senha.isBlank() -> "Informe a senha."
            senha.length < 8 -> "A senha deve possuir no mínimo 8 caracteres."
            senha.length > 100 -> "A senha deve possuir no máximo 100 caracteres."
            !senha.any { it.isUpperCase() } ||
                !senha.any { it.isLowerCase() } ||
                !senha.any { it.isDigit() } ||
                !senha.any { !it.isLetterOrDigit() } ->
                "A senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais."
            else -> null
        }
    }

    fun validarCpf(cpf: String): String? {
        val digits = somenteDigitos(cpf)
        return when {
            digits.length != 11 -> "CPF deve ter 11 dígitos."
            digits.toSet().size == 1 -> "CPF inválido."
            !cpfDigitoValido(digits) -> "CPF inválido."
            else -> null
        }
    }

    fun validarTelefone(telefone: String): String? {
        val digits = somenteDigitos(telefone)
        return when {
            telefone.any { it.isLetter() } -> "O telefone deve conter apenas números."
            digits.length !in 10..11 -> "O telefone deve possuir DDD e número válidos."
            digits.take(2).toIntOrNull() == null || digits.take(2) == "00" -> "Telefone inválido."
            digits.length == 11 && digits[2] != '9' -> "Telefone inválido."
            else -> null
        }
    }

    fun validarDataNascimento(data: String): String? {
        if (!Regex("\\d{2}/\\d{2}/\\d{4}").matches(data)) {
            return "Informe uma data válida."
        }
        val nascimento = try {
            LocalDate.parse(data, dateFormatter)
        } catch (e: DateTimeParseException) {
            return "Data de nascimento inválida."
        }
        val hoje = LocalDate.now()
        val idadeMinima = hoje.minusYears(18)
        return when {
            !nascimento.isAfter(hoje) && nascimento.isAfter(idadeMinima) -> "Data de nascimento invalida. O usuario deve ter pelo menos 18 anos."
            nascimento.isAfter(hoje) -> "Data de nascimento inválida."
            nascimento.isBefore(hoje.minusYears(120)) -> "Data de nascimento inválida."
            else -> null
        }
    }

    fun validarCep(cep: String): String? {
        val digits = somenteDigitos(cep)
        return when {
            digits.length != 8 -> "CEP inválido."
            else -> null
        }
    }

    fun validarNumeroEndereco(numero: String): String? {
        val limpo = numero.trim()
        return when {
            limpo.isBlank() -> "Informe o número."
            limpo.length > 10 -> "O número deve possuir no máximo 10 caracteres."
            !Regex("^[0-9A-Za-z/-]+$").matches(limpo) -> "Número inválido."
            else -> null
        }
    }

    private fun cpfDigitoValido(cpf: String): Boolean {
        fun digito(pesoInicial: Int): Int {
            val soma = (0 until pesoInicial - 1).sumOf { i ->
                cpf[i].digitToInt() * (pesoInicial - i)
            }
            val resto = soma % 11
            return if (resto < 2) 0 else 11 - resto
        }
        return digito(10) == cpf[9].digitToInt() && digito(11) == cpf[10].digitToInt()
    }
}
