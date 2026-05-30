package com.example.paradacerta.models

/** Body de POST /api/reserva/{sessaoId}/finalizar-uso */
data class FinalizarUsoRequest(
    val cpf: String,
    val valorPagoAdicional: Double? = null
)
