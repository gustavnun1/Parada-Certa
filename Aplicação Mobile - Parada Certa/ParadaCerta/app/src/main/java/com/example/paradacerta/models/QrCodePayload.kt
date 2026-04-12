package com.example.paradacerta.models

/**
 * Representa o conteúdo de um QR Code do Parada Certa.
 *
 * QR de entrada:
 *   {"tipo":"ENTRADA","id":1,"nome":"Centro Park","precoHora":5.00}
 *
 * QR de pagamento:
 *   {"tipo":"PAGAMENTO","sessaoId":"abc123","valor":25.50,"nome":"Centro Park","pixKey":"11999999999"}
 */
data class QrCodePayload(
    val tipo: String,           // "ENTRADA" | "PAGAMENTO"
    val id: Int? = null,
    val nome: String? = null,
    val precoHora: Double? = null,
    val sessaoId: String? = null,
    val valor: Double? = null,
    val pixKey: String? = null
)
