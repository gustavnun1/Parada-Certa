package com.example.paradacerta.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paradacerta.models.EstacionamentoStatus

/**
 * Badge visual do status do estacionamento (Aberto / Fechado / Sem horário).
 * Usada tanto na lista de estacionamentos (MapScreen) quanto na tela de detalhes.
 *
 * @param isOnPrimary quando o badge é renderizado sobre fundo da cor primária
 *  (ex.: card selecionado no MapScreen), inverte as cores para manter contraste.
 */
@Composable
fun StatusFuncionamentoBadge(
    status: EstacionamentoStatus,
    modifier: Modifier = Modifier,
    isOnPrimary: Boolean = false
) {
    val (texto, fundo, conteudo) = when (status) {
        EstacionamentoStatus.Aberto -> Triple(
            "Aberto",
            if (isOnPrimary) Color.White else Color(0xFFD1FADF),
            if (isOnPrimary) Color(0xFF15803D) else Color(0xFF15803D)
        )
        EstacionamentoStatus.Fechado -> Triple(
            "Fechado",
            if (isOnPrimary) Color.White else Color(0xFFFEE2E2),
            if (isOnPrimary) Color(0xFFB91C1C) else Color(0xFFB91C1C)
        )
        EstacionamentoStatus.SemHorario -> Triple(
            "Horário não informado",
            if (isOnPrimary) Color.White else Color(0xFFE5E7EB),
            if (isOnPrimary) Color(0xFF4B5563) else Color(0xFF4B5563)
        )
    }

    Box(
        modifier = modifier
            .background(color = fundo, shape = RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = conteudo,
            fontWeight = FontWeight.SemiBold
        )
    }
}
