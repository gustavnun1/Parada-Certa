package com.example.paradacerta.models

data class AddressUiState(
    val logradouro: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val estado: String = "",
    val error: String? = null
)