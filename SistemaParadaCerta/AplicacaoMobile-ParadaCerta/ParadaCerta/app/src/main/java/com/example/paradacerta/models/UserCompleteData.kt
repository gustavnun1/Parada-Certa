package com.example.paradacerta.models

data class UserCompleteData(
    val cliente: Cliente,
    val veiculos: List<Veiculo>,
    val endereco: Endereco?
)