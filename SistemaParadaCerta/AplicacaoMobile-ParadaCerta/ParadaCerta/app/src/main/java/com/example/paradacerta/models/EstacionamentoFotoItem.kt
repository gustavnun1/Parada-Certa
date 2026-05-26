package com.example.paradacerta.models

data class EstacionamentoFotoItem(
    val id: Int,
    val estacionamentoId: Int,
    val url: String,
    val ordem: Int = 0,
    val principal: Boolean = false
)
