package com.example.paradacerta.validation

object PlacaValidator {
    private val placaAntiga = Regex("^[A-Z]{3}[0-9]{4}$")
    private val placaMercosul = Regex("^[A-Z]{3}[0-9][A-Z][0-9]{2}$")

    const val MENSAGEM_FORMATO_INVALIDO =
        "Placa invalida. Use o formato ABC1234 ou ABC1D23."

    fun normalizar(placa: String): String {
        return placa.uppercase()
            .filter { it.isLetterOrDigit() }
            .take(7)
    }

    fun isValida(placa: String): Boolean {
        val normalizada = normalizar(placa)
        return placaAntiga.matches(normalizada) || placaMercosul.matches(normalizada)
    }
}
