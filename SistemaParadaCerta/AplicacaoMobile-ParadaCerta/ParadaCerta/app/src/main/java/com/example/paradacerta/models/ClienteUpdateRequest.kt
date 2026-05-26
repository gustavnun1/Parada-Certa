package com.example.paradacerta.models

data class ClienteUpdateRequest(
    val cpf: String,
    val nome: String,
    val email: String,
    val senha: String,
    val dataNascimento: String,
    val numeroCelular: String = "",
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String = "",
    val bairro: String,
    val cidade: String,
    val estado: String
)
