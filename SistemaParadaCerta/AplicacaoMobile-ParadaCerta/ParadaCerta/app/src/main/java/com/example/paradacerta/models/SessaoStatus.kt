package com.example.paradacerta.models

/**
 * Status da sessão/reserva — espelha o enum SessaoStatus do backend.
 * Builds antigos do app podem receber valores ATIVA/ENCERRADA/CANCELADA;
 * builds novos também recebem AGUARDANDO_CONFIRMACAO e EM_USO.
 */
enum class SessaoStatus {
    AGUARDANDO_CONFIRMACAO,
    EM_USO,
    ATIVA,
    ENCERRADA,
    CANCELADA;

    companion object {
        fun fromString(raw: String?): SessaoStatus {
            if (raw.isNullOrBlank()) return ATIVA
            return runCatching { valueOf(raw.uppercase()) }.getOrDefault(ATIVA)
        }
    }
}
