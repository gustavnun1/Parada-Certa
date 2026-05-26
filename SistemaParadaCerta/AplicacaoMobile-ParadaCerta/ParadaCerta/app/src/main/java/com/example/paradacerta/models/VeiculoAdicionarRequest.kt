package com.example.paradacerta.models

data class VeiculoAdicionarRequest(
    val cpf: String,
    val placa: String,
    val modeloVeiculo: String,
    val corVeiculo: String
)
