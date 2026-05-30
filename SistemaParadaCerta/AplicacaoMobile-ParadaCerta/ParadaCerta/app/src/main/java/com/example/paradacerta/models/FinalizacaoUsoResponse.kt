package com.example.paradacerta.models

/**
 * Resposta do cálculo / efetivação de finalização de uso de vaga reservada.
 * Quando `exigeCobrancaAdicional` é true, o mobile precisa cobrar
 * `valorRestante` via Pix antes de efetivar a finalização.
 */
data class FinalizacaoUsoResponse(
    val sessaoId: String,
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val pixKey: String?,
    val minutosUso: Long,
    val minutosCobrados: Long,
    val precoHora: Double,
    val valorPagoAntecipado: Double,
    val valorFinalCalculado: Double,
    val valorRestante: Double,
    val exigeCobrancaAdicional: Boolean
)
