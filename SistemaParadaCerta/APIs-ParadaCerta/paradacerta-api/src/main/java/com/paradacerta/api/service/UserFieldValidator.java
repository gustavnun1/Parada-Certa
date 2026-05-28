package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UserFieldValidator {

    private static final Pattern NOME_VALIDO = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$");
    private static final Pattern EMAIL_VALIDO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NUMERO_VALIDO = Pattern.compile("^[0-9A-Za-z/-]+$");
    private static final DateTimeFormatter DATA_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private UserFieldValidator() {
    }

    public static String normalizarNome(String nome) {
        if (nome == null) throw new RequisicaoInvalidaException("Informe o nome.");
        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3) throw new RequisicaoInvalidaException("O nome deve possuir no mínimo 3 caracteres.");
        if (limpo.length() > 80) throw new RequisicaoInvalidaException("O nome deve possuir no máximo 80 caracteres.");
        if (limpo.chars().anyMatch(Character::isDigit)) {
            throw new RequisicaoInvalidaException("O nome não pode conter números.");
        }
        if (!NOME_VALIDO.matcher(limpo).matches()) throw new RequisicaoInvalidaException("Nome inválido.");
        return limpo;
    }

    public static String normalizarEmail(String email) {
        if (email == null) throw new RequisicaoInvalidaException("Informe um email válido.");
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        if (limpo.length() > 120 || !EMAIL_VALIDO.matcher(limpo).matches()) {
            throw new RequisicaoInvalidaException("Email inválido.");
        }
        return limpo;
    }

    static void validarSenha(String senha, boolean obrigatoria) {
        if (!obrigatoria && (senha == null || senha.isBlank())) return;
        if (senha == null || senha.isBlank()) throw new RequisicaoInvalidaException("Informe a senha.");
        if (senha.length() < 8) throw new RequisicaoInvalidaException("A senha deve possuir no mínimo 8 caracteres.");
        if (senha.length() > 100) throw new RequisicaoInvalidaException("A senha deve possuir no máximo 100 caracteres.");
        boolean upper = senha.chars().anyMatch(Character::isUpperCase);
        boolean lower = senha.chars().anyMatch(Character::isLowerCase);
        boolean digit = senha.chars().anyMatch(Character::isDigit);
        boolean special = senha.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!(upper && lower && digit && special)) {
            throw new RequisicaoInvalidaException(
                    "A senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais."
            );
        }
    }

    static String normalizarTelefone(String telefone) {
        String digits = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new RequisicaoInvalidaException("O telefone deve possuir DDD e número válidos.");
        }
        if (digits.startsWith("00") || (digits.length() == 11 && digits.charAt(2) != '9')) {
            throw new RequisicaoInvalidaException("Telefone inválido.");
        }
        return digits;
    }

    static LocalDate parseDataNascimento(String dataNascimento) {
        if (dataNascimento == null || !dataNascimento.matches("\\d{2}/\\d{2}/\\d{4}")) {
            throw new RequisicaoInvalidaException("Informe uma data válida.");
        }
        LocalDate data;
        try {
            data = LocalDate.parse(dataNascimento, DATA_FORMAT);
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException("Data de nascimento inválida.");
        }
        LocalDate hoje = LocalDate.now();
        if (data.isAfter(hoje) || data.isBefore(hoje.minusYears(120))) {
            throw new RequisicaoInvalidaException("Data de nascimento inválida.");
        }
        return data;
    }

    static String normalizarCep(String cep) {
        String digits = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digits.length() != 8) throw new RequisicaoInvalidaException("CEP inválido.");
        return digits;
    }

    static String validarTextoObrigatorio(String valor, String mensagem, int max) {
        if (valor == null || valor.trim().isBlank()) throw new RequisicaoInvalidaException(mensagem);
        String limpo = valor.trim().replaceAll("\\s+", " ");
        if (limpo.length() > max) throw new RequisicaoInvalidaException(mensagem);
        return limpo;
    }

    static String normalizarNumeroEndereco(String numero) {
        if (numero == null || numero.trim().isBlank()) throw new RequisicaoInvalidaException("Informe o número.");
        String limpo = numero.trim();
        if (limpo.length() > 10 || !NUMERO_VALIDO.matcher(limpo).matches()) {
            throw new RequisicaoInvalidaException("Número inválido.");
        }
        return limpo;
    }

    static String normalizarUf(String estado) {
        if (estado == null) throw new RequisicaoInvalidaException("Estado é obrigatório.");
        String uf = estado.trim().toUpperCase(Locale.ROOT);
        if (!uf.matches("[A-Z]{2}")) throw new RequisicaoInvalidaException("Estado deve ter 2 caracteres (UF).");
        return uf;
    }
}
