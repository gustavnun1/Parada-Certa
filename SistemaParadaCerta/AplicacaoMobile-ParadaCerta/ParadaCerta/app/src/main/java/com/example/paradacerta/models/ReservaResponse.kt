package com.example.paradacerta.models

data class ReservaResponse(
    val sessaoId: String,
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val pixKey: String?,
    val horaEntrada: Long,
    val inicioReservaPrevisto: Long?,
    val precoHora: Double,
    val placa: String?,
    val modeloVeiculo: String?,
    val status: String? = null
)
