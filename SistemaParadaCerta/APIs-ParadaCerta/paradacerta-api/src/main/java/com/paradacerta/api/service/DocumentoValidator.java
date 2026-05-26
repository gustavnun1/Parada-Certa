package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;

/** Valida documentos brasileiros usados pelo painel admin. */
final class DocumentoValidator {

    private DocumentoValidator() {
    }

    static String somenteDigitos(String valor) {
        if (valor == null) return null;
        String d = valor.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    static void validarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            throw new RequisicaoInvalidaException("CPF invalido: deve ter 11 digitos");
        }
        if (!cpf.chars().allMatch(Character::isDigit) || todosIguais(cpf)) {
            throw new RequisicaoInvalidaException("CPF invalido");
        }

        int soma = 0;
        for (int i = 0; i < 9; i++) soma += Character.digit(cpf.charAt(i), 10) * (10 - i);
        int dig1 = (soma % 11) < 2 ? 0 : 11 - (soma % 11);
        if (dig1 != Character.digit(cpf.charAt(9), 10)) {
            throw new RequisicaoInvalidaException("CPF invalido");
        }

        soma = 0;
        for (int i = 0; i < 10; i++) soma += Character.digit(cpf.charAt(i), 10) * (11 - i);
        int dig2 = (soma % 11) < 2 ? 0 : 11 - (soma % 11);
        if (dig2 != Character.digit(cpf.charAt(10), 10)) {
            throw new RequisicaoInvalidaException("CPF invalido");
        }
    }

    static void validarCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            throw new RequisicaoInvalidaException("CNPJ invalido: deve ter 14 digitos");
        }
        if (!cnpj.chars().allMatch(Character::isDigit) || todosIguais(cnpj)) {
            throw new RequisicaoInvalidaException("CNPJ invalido");
        }

        int dig1 = calcularDigitoCnpj(cnpj, 12);
        if (dig1 != Character.digit(cnpj.charAt(12), 10)) {
            throw new RequisicaoInvalidaException("CNPJ invalido");
        }

        int dig2 = calcularDigitoCnpj(cnpj, 13);
        if (dig2 != Character.digit(cnpj.charAt(13), 10)) {
            throw new RequisicaoInvalidaException("CNPJ invalido");
        }
    }

    private static int calcularDigitoCnpj(String cnpj, int tamanho) {
        int[] pesos = tamanho == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.digit(cnpj.charAt(i), 10) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String valor) {
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != valor.charAt(0)) return false;
        }
        return true;
    }
}
