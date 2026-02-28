package com.example.paradacerta.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradacerta.components.ParkingCard
import com.example.paradacerta.models.MockParkingData
import com.example.paradacerta.ui.theme.CinzaMedio

/**
 * Tela de favoritos do usuário
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onParkingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var parkingList by remember { mutableStateOf(MockParkingData.parkingList) }
    val favoriteParkings = parkingList.filter { it.isFavorite }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favoritos",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (favoriteParkings.isEmpty()) {
            // Estado vazio
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = CinzaMedio
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Nenhum favorito ainda",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Adicione estacionamentos aos favoritos para acesso rápido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CinzaMedio,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Lista de favoritos
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${favoriteParkings.size} ${if (favoriteParkings.size == 1) "favorito" else "favoritos"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(favoriteParkings) { parking ->
                    ParkingCard(
                        parking = parking,
                        onClick = { onParkingClick(parking.id) },
                        onFavoriteClick = {
                            parkingList = parkingList.map {
                                if (it.id == parking.id) {
                                    it.copy(isFavorite = false)
                                } else {
                                    it
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}