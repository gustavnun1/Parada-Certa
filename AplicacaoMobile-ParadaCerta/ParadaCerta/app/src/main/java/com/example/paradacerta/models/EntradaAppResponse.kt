package com.example.paradacerta.models

data class EntradaAppResponse(
    val sucesso: Boolean,
    val mensagem: String,
    val sessaoId: String,
    val horaEntrada: String   // LocalDateTime serializado como string
)
