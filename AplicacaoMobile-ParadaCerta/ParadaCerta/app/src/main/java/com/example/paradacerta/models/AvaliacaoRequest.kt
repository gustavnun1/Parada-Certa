package com.example.paradacerta.models

data class AvaliacaoRequest(
    val estacionamentoId: Int,
    val clienteCpf: String,
    val nota: Int,
    val comentario: String? = null
)
