package com.example.paradacerta.models

import com.google.gson.annotations.SerializedName

// Body enviado no POST
data class ClientRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val cpf: String,
    val dataNascimento: String,
    val numeroCelular: String = "",

    val placa: String,
    val modeloVeiculo: String,
    val corVeiculo: String,

    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String = "",
    val bairro: String,
    val cidade: String,
    val estado: String
)

// Resposta padrão de todos os endpoints
data class ApiResponse(
    @SerializedName("sucesso")   val sucesso: Boolean,
    @SerializedName("mensagem")  val mensagem: String
)