package com.example.paradacerta.models

/** Resposta de POST /api/reserva/confirmar */
data class ConfirmacaoReservaResponse(
    val sessaoId: String,
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val pixKey: String?,
    val dataHoraConfirmacao: Long,
    val precoHora: Double
)
