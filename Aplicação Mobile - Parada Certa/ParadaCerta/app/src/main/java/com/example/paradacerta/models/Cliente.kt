package com.example.paradacerta.models

import java.util.Date

data class Cliente(
    val nome: String,
    val cpf: String,
    val email: String,
    val senha: String,
    val dataNascimento: Date,
    val numeroCelular: String = "",
    val placa: String
)