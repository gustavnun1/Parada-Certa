package com.example.paradacerta.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * Status visível de um estacionamento para o motorista no app mobile.
 *
 * O cálculo usa o relógio local do aparelho (LocalTime.now()) e o intervalo
 * de funcionamento cadastrado (horarioAbertura/horarioFechamento). Trata
 * corretamente o caso de intervalos que cruzam a meia-noite (ex.: 22:00 às 06:00).
 */
sealed class EstacionamentoStatus {
    object Aberto : EstacionamentoStatus()
    object Fechado : EstacionamentoStatus()
    object SemHorario : EstacionamentoStatus()

    val ehAberto: Boolean get() = this is Aberto
    val ehFechado: Boolean get() = this is Fechado

    companion object {
        /**
         * Calcula o status do estacionamento dado o horário corrente.
         *
         * - Se um dos horários for nulo/vazio → [SemHorario].
         * - Se abertura == fechamento → [Aberto] (interpretado como 24h).
         * - Se fechamento > abertura → [Aberto] se `agora` ∈ [abertura, fechamento).
         * - Se fechamento < abertura → cruza meia-noite; [Aberto] se `agora` ≥ abertura
         *   OU `agora` < fechamento.
         */
        fun calcular(
            horarioAbertura: String?,
            horarioFechamento: String?,
            agora: LocalTime = LocalTime.now()
        ): EstacionamentoStatus {
            val abertura = parse(horarioAbertura) ?: return SemHorario
            val fechamento = parse(horarioFechamento) ?: return SemHorario

            if (abertura == fechamento) return Aberto // 24h

            val cruzaMeiaNoite = fechamento.isBefore(abertura)
            val aberto = if (cruzaMeiaNoite) {
                !agora.isBefore(abertura) || agora.isBefore(fechamento)
            } else {
                !agora.isBefore(abertura) && agora.isBefore(fechamento)
            }
            return if (aberto) Aberto else Fechado
        }

        /** Aceita "HH:mm", "HH:mm:ss" ou outros formatos curtos do backend. */
        private fun parse(valor: String?): LocalTime? {
            if (valor.isNullOrBlank()) return null
            val texto = valor.trim()
            return try {
                LocalTime.parse(texto)
            } catch (_: Exception) {
                // tenta com apenas HH:mm
                try {
                    val partes = texto.split(":")
                    val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
                    val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
                    if (h !in 0..23 || m !in 0..59) null
                    else LocalTime.of(h, m)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Recalcula o status do estacionamento periodicamente (a cada 30s), garantindo
 * que a tela reflita a transição abre→fecha (ou vice-versa) sem precisar
 * recarregar manualmente.
 */
@Composable
fun rememberEstacionamentoStatus(
    horarioAbertura: String?,
    horarioFechamento: String?
): State<EstacionamentoStatus> {
    val chave = remember(horarioAbertura, horarioFechamento) {
        (horarioAbertura ?: "") + "|" + (horarioFechamento ?: "")
    }
    val status = remember(chave) {
        mutableStateOf(EstacionamentoStatus.calcular(horarioAbertura, horarioFechamento))
    }

    LaunchedEffect(chave) {
        status.value = EstacionamentoStatus.calcular(horarioAbertura, horarioFechamento)
        while (true) {
            delay(30_000L)
            status.value = EstacionamentoStatus.calcular(horarioAbertura, horarioFechamento)
        }
    }

    return status
}
