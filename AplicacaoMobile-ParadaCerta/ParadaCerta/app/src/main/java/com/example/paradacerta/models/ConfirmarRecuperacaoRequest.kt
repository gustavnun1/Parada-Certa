package com.example.paradacerta.models

data class ConfirmarRecuperacaoRequest(
    val login: String,
    val cpf: Boolean = false,
    val codigo: String,
    val novaSenha: String
)
