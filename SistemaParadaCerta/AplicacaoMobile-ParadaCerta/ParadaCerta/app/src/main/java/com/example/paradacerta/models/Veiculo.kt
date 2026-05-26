package com.example.paradacerta.models

data class Veiculo(
    val nome: String,
    val placa: String,
    val cor: String,
    val clienteId: Long = 0
)