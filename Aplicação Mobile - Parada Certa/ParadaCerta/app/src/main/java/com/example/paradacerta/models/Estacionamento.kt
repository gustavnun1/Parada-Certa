package com.example.paradacerta.models

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
    val ativo: Boolean = true
)