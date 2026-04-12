package com.example.paradacerta.models

data class FormaPagamento(
    val id: Int,
    val clienteCPF: String,
    val tipoPagamento: String,      // "PIX" | "CARTAO_CREDITO"
    val numeroCartao: String? = null,
    val nomeCartao: String? = null,
    val validade: String? = null,   // formato MM/AA
    val bandeira: String? = null    // Visa, Mastercard, Elo, etc.
)

data class FormaPagamentoRequest(
    val clienteCPF: String,
    val tipoPagamento: String,
    val numeroCartao: String? = null,
    val nomeCartao: String? = null,
    val validade: String? = null,
    val bandeira: String? = null
)
