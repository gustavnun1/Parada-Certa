package com.example.paradacerta.models

data class SolicitarRecuperacaoRequest(
    val login: String,
    val cpf: Boolean = false
)
