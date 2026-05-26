package com.example.paradacerta.models

import com.google.gson.annotations.SerializedName

data class Estacionamento(
    val id: Int,
    val nome: String,
    val qtdVagasTotais: Int,
    val qtdVagasDisponiveis: Int,
    val avaliacaoMedia: Double,
    val latitude: Double,
    val longitude: Double,
    val endereco: String,
    val precoHora: Double,
    val horarioAbertura: String?,
    val horarioFechamento: String?,
    val fotoPrincipal: String?,
    val descricao: String?,
    val ativo: Boolean = true,
    val permiteReserva: Boolean = false,
    val qtdVagasReservaveis: Int = 0,
    val pixKey: String? = null,
    @SerializedName("isPremium")
    val isPremium: Boolean = false,
    @SerializedName("premium")
    val premium: Boolean = false,
    val plano: String? = null,
    val planoFim: String? = null
)
