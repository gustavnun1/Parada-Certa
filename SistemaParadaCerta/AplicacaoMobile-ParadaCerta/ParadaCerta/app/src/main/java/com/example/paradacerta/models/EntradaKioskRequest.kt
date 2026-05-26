package com.example.paradacerta.models

data class EntradaKioskRequest(
    val cpfUsuario: String,
    val token: String,
    val placa: String
)
