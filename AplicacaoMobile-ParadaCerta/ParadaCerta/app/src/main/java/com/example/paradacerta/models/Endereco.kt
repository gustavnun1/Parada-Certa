package com.example.paradacerta.models

data class Endereco(
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String,
    val bairro: String,
    val cidade: String,
    val estado: String,
    val clienteId: Long = 0
)