package com.example.paradacerta.models

/**
 * Resposta de GET /api/sessao/{id}/calculo-cobranca.
 * Reflete o valor calculado pelo backend (fonte da verdade) aplicando:
 *  - cobrança mínima de 1 hora;
 *  - arredondamento por blocos de 30 minutos acima de 1h.
 */
data class CobrancaEstadia(
    val precoHora: Double,
    val minutosPermanencia: Long,
    val minutosCobrados: Long,
    val valorTotal: Double
)
