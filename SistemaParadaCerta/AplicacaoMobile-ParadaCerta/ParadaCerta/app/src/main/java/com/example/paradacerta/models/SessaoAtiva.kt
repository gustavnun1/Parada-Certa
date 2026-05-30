package com.example.paradacerta.models

data class SessaoAtiva(
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val modeloVeiculo: String,
    val placa: String,
    val precoHora: Double,
    val horaEntrada: Long = System.currentTimeMillis(),
    val inicioReservaPrevisto: Long? = null,
    val sessaoId: String = "",
    val pixKey: String = "",
    val reservado: Boolean = false,
    val horarioReserva: String? = null
)
