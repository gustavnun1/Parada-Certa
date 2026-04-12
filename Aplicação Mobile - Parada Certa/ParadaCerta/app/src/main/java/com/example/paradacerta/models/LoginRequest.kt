package com.example.paradacerta.models

import com.google.gson.annotations.SerializedName

// Aceita login por e-mail OU por CPF — somente um dos dois deve ser preenchido
data class LoginRequest(
    @SerializedName("email") val email: String? = null,
    @SerializedName("cpf")   val cpf: String? = null,
    @SerializedName("senha") val senha: String
)
