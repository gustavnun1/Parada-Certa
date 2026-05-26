package com.example.paradacerta.models

data class FormaPagamento(
    val id: Int,
    val clienteId: Long = 0,
    val tipoPagamento: String,      // "PIX" | "CARTAO_CREDITO"
    val numeroCartao: String? = null,
    val nomeCartao: String? = null,
    val validade: String? = null,   // formato MM/AA
    val bandeira: String? = null    // Visa, Mastercard, Elo, etc.
)

data class FormaPagamentoRequest(
    val clienteCpf: String,
    val tipoPagamento: String,
    val numeroCartao: String? = null,  // dígitos brutos — backend armazena só os últimos 4
    val nomeCartao: String? = null,
    val validade: String? = null,
    val bandeira: String? = null
    // CVV não é incluído — nunca deve ser transmitido ou armazenado
)

data class PixKeyResponse(val pixKey: String)
