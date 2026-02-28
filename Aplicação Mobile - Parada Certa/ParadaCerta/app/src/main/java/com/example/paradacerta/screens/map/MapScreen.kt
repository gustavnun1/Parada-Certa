package com.example.paradacerta.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradacerta.components.ParkingCard
import com.example.paradacerta.models.MockParkingData
import com.example.paradacerta.ui.theme.CinzaClaro
import com.example.paradacerta.ui.theme.CinzaMedio

/**
 * Tela de mapa com simulação visual e cards de estacionamentos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onParkingClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var parkingList by remember { mutableStateOf(MockParkingData.parkingList) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mapa",
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Simulação de mapa (placeholder visual)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinzaClaro),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = CinzaMedio
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Mapa Ilustrativo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CinzaMedio
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Integração futura com API de mapas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CinzaMedio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "📍 Localização: São Paulo, SP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Cards flutuantes de estacionamentos na parte inferior
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Estacionamentos próximos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(parkingList.take(3)) { parking ->
                                Box(modifier = Modifier.width(300.dp)) {
                                    ParkingCard(
                                        parking = parking,
                                        onClick = { onParkingClick(parking.id) },
                                        onFavoriteClick = {
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
                }
            }
        }
    }
}