package com.example.paradacerta.models

/**
 * Representa o conteúdo de um QR Code do Parada Certa.
 *
 * QR de entrada legado:
 *   {"tipo":"ENTRADA","id":1,"nome":"Centro Park","precoHora":5.00}
 *
 * QR de entrada emitido pelo kiosk:
 *   {"v":1,"app":"paradacerta","type":"entrada","estacionamentoId":1,"token":"..."}
 *
 * QR de pagamento:
 *   {"tipo":"PAGAMENTO","sessaoId":"abc123","valor":25.50,"nome":"Centro Park","pixKey":"11999999999"}
 */
data class QrCodePayload(
    val tipo: String? = null,   // "ENTRADA" | "PAGAMENTO"
    val type: String? = null,
    val id: Int? = null,
    val estacionamentoId: Int? = null,
    val token: String? = null,
    val nome: String? = null,
    val precoHora: Double? = null,
    val sessaoId: String? = null,
    val valor: Double? = null,
    val pixKey: String? = null
)
