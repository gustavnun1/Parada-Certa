package com.example.paradacerta.screens.map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

// ---------- Ícone customizado de estacionamento ----------
// Separado em duas funções:
// 1. criarBitmapEstacionamento → só usa Android Canvas, seguro chamar em qualquer momento
// 2. BitmapDescriptorFactory.fromBitmap() → requer Maps SDK inicializado, chamado dentro de MapEffect

private fun criarBitmapEstacionamento(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paintFundo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }
    val paintBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.07f
    }
    val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = sizePx * 0.52f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val raio = cx - paintBorda.strokeWidth

    canvas.drawCircle(cx, cy, raio, paintFundo)
    canvas.drawCircle(cx, cy, raio, paintBorda)

    val textY = cy - (paintTexto.descent() + paintTexto.ascent()) / 2f
    canvas.drawText("P", cx, textY, paintTexto)

    return bitmap   // retorna Bitmap puro, sem dependência do Maps SDK
}

// ---------- Tela do Mapa ----------

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

    // Bitmap criado fora do contexto do Maps (seguro) — BitmapDescriptor criado dentro de MapEffect
    val parkingBitmap = remember { criarBitmapEstacionamento(context) }
    var iconeEstacionamento by remember { mutableStateOf<BitmapDescriptor?>(null) }

    // Permissões de localização
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val permissaoConcedida = locationPermissions.permissions.any { it.status.isGranted }

    // Estado da câmera — começa no centro do Brasil até obter localização real
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-15.7801, -47.9292), 4f)
    }

    // Solicita permissão e, ao conceder, centraliza no usuário
    LaunchedEffect(permissaoConcedida) {
        if (permissaoConcedida) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                // getCurrentLocation solicita leitura FRESCA do GPS/rede,
                // evitando a posição em cache desatualizada de lastLocation
                val cts = CancellationTokenSource()
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        val lat = location?.latitude ?: -23.550520
                        val lng = location?.longitude ?: -46.633308

                        viewModel.carregarEstacionamentosProximos(lat, lng, raioKm = 5.0)

                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                                ),
                                durationMs = 900
                            )
                        }
                    }
            } catch (_: SecurityException) {
                viewModel.carregarEstacionamentosProximos(-23.550520, -46.633308)
            }
        } else {
            locationPermissions.launchMultiplePermissionRequest()
            viewModel.carregarTodos()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa", fontWeight = FontWeight.Bold) },
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
            // ── Mapa ─────────────────────────────────────────────────────
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    // Habilita a camada nativa de localização do usuário (ponto azul)
                    isMyLocationEnabled = permissaoConcedida
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    // Botão "minha localização" nativo do Google Maps
                    myLocationButtonEnabled = permissaoConcedida
                )
            ) {
                // BitmapDescriptorFactory só pode ser chamado após o Maps SDK estar pronto.
                // MapEffect garante que executamos depois que o mapa foi inicializado.
                MapEffect(Unit) { _ ->
                    iconeEstacionamento = BitmapDescriptorFactory.fromBitmap(parkingBitmap)
                }

                // Marcadores dos estacionamentos com ícone customizado
                mapState.estacionamentos.forEach { estacionamento ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(estacionamento.latitude, estacionamento.longitude)
                        ),
                        title = estacionamento.nome,
                        snippet = "R$ ${String.format("%.2f", estacionamento.precoHora)}/hora · ${estacionamento.qtdVagasDisponiveis} vagas",
                        icon = iconeEstacionamento
                            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = {
                            selectedEstacionamento = estacionamento
                            false // false = abre a InfoWindow padrão do Google Maps
                        }
                    )
                }
            }

            // ── Botão de centralizar no usuário (extra, quando permissão está ativa) ──
            if (permissaoConcedida) {
                FloatingActionButton(
                    onClick = {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        try {
                            val cts = CancellationTokenSource()
                            fusedLocationClient
                                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        scope.launch {
                                            cameraPositionState.animate(
                                                update = CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.fromLatLngZoom(
                                                        LatLng(location.latitude, location.longitude), 15f
                                                    )
                                                ),
                                                durationMs = 600
                                            )
                                        }
                                    }
                                }
                        } catch (_: SecurityException) {}
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Minha localização",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Loading ───────────────────────────────────────────────────
            if (mapState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // ── Erro ──────────────────────────────────────────────────────
            mapState.errorMessage?.let { erro ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Painel inferior com cards dos estacionamentos ─────────────
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
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
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    update = CameraUpdateFactory.newCameraPosition(
                                                        CameraPosition.fromLatLngZoom(
                                                            LatLng(estacionamento.latitude, estacionamento.longitude),
                                                            16f
                                                        )
                                                    ),
                                                    durationMs = 500
                                                )
                                            }
                                        },
                                        onDetailsClick = { onParkingClick(estacionamento.id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ---------- Card do estacionamento ----------

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

            Text(
                text = estacionamento.endereco,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            estacionamento.horarioAbertura?.let { abertura ->
                estacionamento.horarioFechamento?.let { fechamento ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⏰ $abertura – $fechamento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDetailsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Detalhes")
            }
        }
    }
}
