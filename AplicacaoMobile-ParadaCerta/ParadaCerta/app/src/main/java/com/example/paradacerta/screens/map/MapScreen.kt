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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.components.SeloQualidade
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Ícones customizados de estacionamento ----------
// Separado em duas funções:
// 1. criarBitmapEstacionamento → só usa Android Canvas, seguro chamar em qualquer momento
// 2. BitmapDescriptorFactory.fromBitmap() → requer Maps SDK inicializado, chamado dentro de MapEffect

private fun criarBitmapEstacionamento(context: Context, isQualidade: Boolean = false): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val corFundo = if (isQualidade)
        android.graphics.Color.parseColor("#FF8F00")
    else
        android.graphics.Color.parseColor("#1565C0")

    val paintFundo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = corFundo
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

    if (isQualidade) {
        // Badge de verificação no canto superior direito
        val badgeR = sizePx * 0.21f
        val badgeCx = sizePx * 0.76f
        val badgeCy = sizePx * 0.24f

        val paintBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(badgeCx, badgeCy, badgeR, paintBadge)

        val paintBadgeBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FF8F00")
            style = Paint.Style.STROKE
            strokeWidth = badgeR * 0.22f
        }
        canvas.drawCircle(badgeCx, badgeCy, badgeR - badgeR * 0.11f, paintBadgeBorda)

        // Desenha um check (✓) via Path para garantir renderização consistente
        val paintCheck = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FF8F00")
            style = Paint.Style.STROKE
            strokeWidth = badgeR * 0.30f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = badgeR * 0.52f
        val checkPath = android.graphics.Path().apply {
            moveTo(badgeCx - s * 0.55f, badgeCy)
            lineTo(badgeCx - s * 0.05f, badgeCy + s * 0.50f)
            lineTo(badgeCx + s * 0.65f, badgeCy - s * 0.45f)
        }
        canvas.drawPath(checkPath, paintCheck)
    }

    return bitmap
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
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pesquisa de locais
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var isSearchActive by remember { mutableStateOf(false) }

    val placesClient = remember { Places.createClient(context) }
    val saoPauloBounds = remember {
        RectangularBounds.newInstance(
            LatLng(-23.86, -46.83),
            LatLng(-23.37, -46.38)
        )
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        val request = FindAutocompletePredictionsRequest.builder()
            .setLocationRestriction(saoPauloBounds)
            .setQuery(searchQuery)
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                suggestions = response.autocompletePredictions
            }
            .addOnFailureListener {
                suggestions = emptyList()
            }
    }

    // Bitmaps criados fora do contexto do Maps (seguro) — BitmapDescriptors criados dentro de MapEffect
    val parkingBitmap = remember { criarBitmapEstacionamento(context) }
    val parkingBitmapQualidade = remember { criarBitmapEstacionamento(context, isQualidade = true) }
    var iconeEstacionamento by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var iconeEstacionamentoQualidade by remember { mutableStateOf<BitmapDescriptor?>(null) }

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
                    iconeEstacionamentoQualidade = BitmapDescriptorFactory.fromBitmap(parkingBitmapQualidade)
                }

                // Marcadores dos estacionamentos com ícone customizado
                mapState.estacionamentos.forEach { estacionamento ->
                    val icone = if (estacionamento.avaliacaoMedia > 4.5)
                        iconeEstacionamentoQualidade
                    else
                        iconeEstacionamento
                    Marker(
                        state = MarkerState(
                            position = LatLng(estacionamento.latitude, estacionamento.longitude)
                        ),
                        title = estacionamento.nome,
                        snippet = "R$ ${String.format("%.2f", estacionamento.precoHora)}/hora · ${estacionamento.qtdVagasDisponiveis} vagas",
                        icon = icone,
                        onClick = {
                            selectedEstacionamento = estacionamento
                            false
                        }
                    )
                }
            }

            // ── Barra de pesquisa ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; isSearchActive = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .wrapContentHeight(Alignment.CenterVertically),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    isSearchActive = false
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar local em São Paulo...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    suggestions = emptyList()
                                    isSearchActive = false
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar busca",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sugestões de autocomplete
                if (isSearchActive && suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            suggestions.take(5).forEachIndexed { index, prediction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val fetchReq = FetchPlaceRequest.newInstance(
                                                prediction.placeId,
                                                listOf(Place.Field.LAT_LNG)
                                            )
                                            placesClient.fetchPlace(fetchReq)
                                                .addOnSuccessListener { resp ->
                                                    resp.place.latLng?.let { latLng ->
                                                        scope.launch {
                                                            cameraPositionState.animate(
                                                                update = CameraUpdateFactory.newCameraPosition(
                                                                    CameraPosition.fromLatLngZoom(latLng, 16f)
                                                                ),
                                                                durationMs = 700
                                                            )
                                                        }
                                                    }
                                                    searchQuery = prediction.getPrimaryText(null).toString()
                                                    suggestions = emptyList()
                                                    isSearchActive = false
                                                    keyboardController?.hide()
                                                }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prediction.getPrimaryText(null).toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = prediction.getSecondaryText(null).toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (index < suggestions.take(5).lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }

                // Mensagem de erro
                mapState.errorMessage?.let { erro ->
                    Spacer(Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
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
            }

            // ── Botão de centralizar no usuário ───────────────────────────
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
                        .padding(top = 72.dp, end = 16.dp)
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
        val corPrincipal = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface
        val corSecundaria = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        else
            MaterialTheme.colorScheme.onSurfaceVariant
        val corPreco = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.primary

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
                    color = corPrincipal,
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
                        fontWeight = FontWeight.SemiBold,
                        color = corPrincipal
                    )
                }
            }

            if (estacionamento.avaliacaoMedia > 4.5) {
                Spacer(modifier = Modifier.height(6.dp))
                SeloQualidade()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = estacionamento.endereco,
                style = MaterialTheme.typography.bodySmall,
                color = corSecundaria,
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
                        color = corSecundaria
                    )
                    Text(
                        text = "R$ ${String.format("%.2f", estacionamento.precoHora)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = corPreco
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vagas",
                        style = MaterialTheme.typography.bodySmall,
                        color = corSecundaria
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
                        color = corSecundaria
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
