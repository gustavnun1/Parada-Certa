package com.example.paradacerta.models

data class UserCompleteData(
    val cliente: Cliente,
    val veiculo: Veiculo?,
    val endereco: Endereco?
)