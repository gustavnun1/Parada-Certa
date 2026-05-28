package com.example.paradacerta.models

data class EntradaAppRequest(
    val cpfUsuario: String,
    val estacionamentoId: Int,
    val placa: String,
    val horaEntradaDispositivoMs: Long
)
