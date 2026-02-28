package com.example.paradacerta.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paradacerta.components.ParkingCard
import com.example.paradacerta.models.MockParkingData

/**
 * Tela inicial com lista de estacionamentos próximos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onParkingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado local para gerenciar favoritos
    var parkingList by remember { mutableStateOf(MockParkingData.parkingList) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parada Certa",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Ação de busca */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Estacionamentos próximos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(parkingList) { parking ->
                ParkingCard(
                    parking = parking,
                    onClick = { onParkingClick(parking.id) },
                    onFavoriteClick = {
                        // Atualiza o estado de favorito
                        parkingList = parkingList.map {
                            if (it.id == parking.id) {
                                it.copy(isFavorite = !it.isFavorite)
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