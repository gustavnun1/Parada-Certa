package com.paradacerta.api.service;

public final class PlacaValidator {

    public static final String MSG_FORMATO_INVALIDO =
            "Placa invalida. Use o formato ABC1234 ou ABC1D23.";

    private PlacaValidator() {
    }

    public static String normalizar(String placa) {
        if (placa == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(7);
        for (char c : placa.toUpperCase().toCharArray()) {
            if (Character.isLetterOrDigit(c) && sb.length() < 7) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isValida(String placa) {
        String normalizada = normalizar(placa);
        return normalizada.matches("^[A-Z]{3}[0-9]{4}$")
                || normalizada.matches("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");
    }
}
