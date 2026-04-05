package com.example.paradacerta.screens.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onParkingClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel()
) {
    val mapState by viewModel.mapState.collectAsState()
    var selectedEstacionamento by remember { mutableStateOf<Estacionamento?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permissões de localização
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Solicita permissões ao abrir a tela
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Obtém localização atual
    fun obterLocalizacaoAtual() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.carregarEstacionamentosProximos(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        raioKm = 5.0
                    )
                } else {
                    // Usa localização padrão (São Paulo)
                    viewModel.carregarEstacionamentosProximos(
                        latitude = -23.550520,
                        longitude = -46.633308,
                        raioKm = 5.0
                    )
                }
            }
        } catch (e: SecurityException) {
            // Sem permissão, usa localização padrão
            viewModel.carregarEstacionamentosProximos(
                latitude = -23.550520,
                longitude = -46.633308,
                raioKm = 5.0
            )
        }
    }

    // Carrega ao abrir
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            obterLocalizacaoAtual()
        } else {
            viewModel.carregarEstacionamentosProximos(
                latitude = -23.550520,
                longitude = -46.633308,
                raioKm = 5.0
            )
        }
    }

    // Posição inicial do mapa (São Paulo)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(mapState.userLatitude, mapState.userLongitude),
            14f
        )
    }

    // Carrega estacionamentos ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.carregarEstacionamentosProximos(
            latitude = -23.550520,  // São Paulo
            longitude = -46.633308,
            raioKm = 5.0
        )
    }

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
            // Mapa do Google
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Marcadores dos estacionamentos
                mapState.estacionamentos.forEach { estacionamento ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(estacionamento.latitude, estacionamento.longitude)
                        ),
                        title = estacionamento.nome,
                        snippet = "R$ ${String.format("%.2f", estacionamento.precoHora)}/hora - ${estacionamento.qtdVagasDisponiveis} vagas",
                        onClick = {
                            selectedEstacionamento = estacionamento
                            false
                        }
                    )
                }
            }

            // Loading indicator
            if (mapState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Erro
            mapState.errorMessage?.let { erro ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Cards flutuantes na parte inferior
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estacionamentos próximos (${mapState.estacionamentos.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(onClick = { viewModel.carregarTodos() }) {
                                Text("Ver todos")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (mapState.estacionamentos.isEmpty() && !mapState.isLoading) {
                            Text(
                                text = "Nenhum estacionamento encontrado",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(mapState.estacionamentos) { estacionamento ->
                                    EstacionamentoMapCard(
                                        estacionamento = estacionamento,
                                        isSelected = selectedEstacionamento?.id == estacionamento.id,
                                        onClick = {
                                            selectedEstacionamento = estacionamento
                                            // Move câmera para o estacionamento
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                LatLng(estacionamento.latitude, estacionamento.longitude),
                                                16f
                                            )
                                        },
                                        onDetailsClick = { onParkingClick(estacionamento.id) }
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

@Composable
fun EstacionamentoMapCard(
    estacionamento: Estacionamento,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Nome e avaliação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = estacionamento.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", estacionamento.avaliacaoMedia),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Endereço
            Text(
                text = estacionamento.endereco,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preço e vagas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Preço/hora",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", estacionamento.precoHora)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vagas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${estacionamento.qtdVagasDisponiveis}/${estacionamento.qtdVagasTotais}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (estacionamento.qtdVagasDisponiveis > 0)
                            Color(0xFF4CAF50)
                        else
                            Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horário
            estacionamento.horarioAbertura?.let { abertura ->
                estacionamento.horarioFechamento?.let { fechamento ->
                    Text(
                        text = "⏰ $abertura - $fechamento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Botão ver detalhes
            Button(
                onClick = onDetailsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Detalhes")
            }
        }
    }
}