package com.example.paradacerta.models

data class SessaoAtivaResponse(
    val sessaoId: String,
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val pixKey: String?,
    val horaEntrada: Long,
    val inicioReservaPrevisto: Long? = null,
    val precoHora: Double,
    val placa: String?,
    val modeloVeiculo: String?,
    val reservado: Boolean? = null
)
