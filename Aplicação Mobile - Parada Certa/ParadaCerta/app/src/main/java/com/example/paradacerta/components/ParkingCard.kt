package com.example.paradacerta.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paradacerta.models.Parking
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.ui.theme.VerdePrincipal
import com.example.paradacerta.ui.theme.Vermelho

/**
 * Card reutilizável para exibir informações de um estacionamento
 * @param parking Dados do estacionamento
 * @param onClick Ação ao clicar no card
 * @param onFavoriteClick Ação ao clicar no botão de favorito
 */
@Composable
fun ParkingCard(
    parking: Parking,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha superior: Nome e Favorito
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = parking.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (parking.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (parking.isFavorite) Vermelho else CinzaMedio
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge de promoção (se houver)
            if (parking.hasPromo) {
                PromoBadge(text = parking.promoText)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Distância
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${parking.distanceKm} km de distância",
                    fontSize = 13.sp,
                    color = CinzaMedio
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linha inferior: Preço, Vagas e Avaliação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preço
                Column {
                    Text(
                        text = "R$ ${String.format("%.2f", parking.pricePerHour)}/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Vagas disponíveis
                Surface(
                    color = if (parking.availableSpots > 20) VerdePrincipal else Vermelho,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${parking.availableSpots} vagas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avaliação
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RatingStars(rating = parking.rating)
                Text(
                    text = "${parking.rating}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CinzaMedio
                )
            }
        }
    }
}