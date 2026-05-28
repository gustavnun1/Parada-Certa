package com.example.paradacerta.models

data class AvaliacaoItem(
    val id: Int,
    val estacionamentoId: Int? = null,
    val clienteId: Long? = null,
    val clienteNome: String? = null,
    val nota: Int,
    val comentario: String?,
    val dataAvaliacao: String
)
