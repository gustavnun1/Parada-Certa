package com.example.paradacerta.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paradacerta.ui.theme.Amarelo
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Componente reutilizável para exibir avaliação em estrelas
 * @param rating Avaliação de 0.0 a 5.0
 * @param modifier Modificador para customização
 */
@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier
) {
    val fullStars = floor(rating).toInt()
    val hasHalfStar = rating - fullStars >= 0.5
    val emptyStars = if (hasHalfStar) 5 - fullStars - 1 else 5 - fullStars

    Row(modifier = modifier) {
        // Estrelas cheias
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Amarelo,
                modifier = Modifier.size(16.dp)
            )
        }

        // Meia estrela (representada como estrela cheia com opacidade reduzida)
        if (hasHalfStar) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Amarelo.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Estrelas vazias
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = Amarelo,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}