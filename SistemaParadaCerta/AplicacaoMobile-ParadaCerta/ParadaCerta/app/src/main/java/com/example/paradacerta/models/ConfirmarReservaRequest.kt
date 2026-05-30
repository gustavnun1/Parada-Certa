package com.example.paradacerta.models

/** Body de POST /api/reserva/confirmar */
data class ConfirmarReservaRequest(
    val qrCode: String,
    val cpf: String
)
