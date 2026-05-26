package com.example.paradacerta.screens.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.paradacerta.models.DevidaReservaExtra
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.models.QrCodePayload
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.viewmodel.MapViewModel
import com.example.paradacerta.viewmodel.ReservaViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    userViewModel: UserViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel(),
    onEntrada: () -> Unit,
    onPagamento: (sessaoId: String, valor: Double, nome: String, pixKey: String) -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val veiculos by userViewModel.veiculosData.collectAsState()
    val mapState by mapViewModel.mapState.collectAsState()
    val sessaoAtiva by userViewModel.sessaoAtiva.collectAsState()
    val devidaReservaExtra by userViewModel.devidaReservaExtra.collectAsState()
    val reservaViewModel: ReservaViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var loadingEntradaDemo by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Dados pendentes para quando o picker de veículo está sendo mostrado
    var pendingEntradaPayload by remember { mutableStateOf<QrCodePayload?>(null) }
    var pendingDemoEntrada by remember { mutableStateOf(false) }
    var pendingDemoEstacionamento by remember { mutableStateOf<Estacionamento?>(null) }
    var showVeiculoPicker by remember { mutableStateOf(false) }

    // Diálogo: tentativa de entrada com sessão já ativa
    var sessaoAtivaDialog by remember { mutableStateOf(false) }

    // Diálogo: nenhum veículo válido selecionado / cadastrado
    var veiculoInvalidoDialog by remember { mutableStateOf(false) }

    // Estado para o diálogo de confirmação de saída de sessão reservada
    // Triple(estacionamentoId, estacionamentoNome, precoHora)
    var reservaExitDialog by remember { mutableStateOf<Triple<Int, String, Double>?>(null) }

    // AtomicBoolean elevado para o nível do composable para poder resetar a partir do diálogo
    val jaProcessou = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // Carrega estacionamentos para o botão de demo

    // Procura o pixKey do estacionamento na lista já carregada do backend.
    // Útil quando entrarmos por QR físico (cujo payload não traz pixKey) — assim
    // a SessaoAtiva já chega à Home com a chave Pix preenchida.
    fun carregarEstacionamentoDoQr(estId: Int, onLoaded: (Estacionamento?) -> Unit) {
        val cached = mapState.estacionamentos.firstOrNull { it.id == estId }
        if (estId <= 0 || cached != null) {
            onLoaded(cached)
            return
        }
        scope.launch {
            loadingEntradaDemo = true
            val estacionamento = runCatching {
                val response = ParadaCertaClient.service.buscarEstacionamentoPorId(estId)
                if (response.isSuccessful) response.body() else null
            }.getOrNull()
            loadingEntradaDemo = false
            onLoaded(estacionamento)
        }
    }

    fun iniciarEntradaComVeiculo(
        veiculo: Veiculo,
        estId: Int,
        estNome: String,
        precoHora: Double,
        pixKey: String
    ) {
        if (veiculo.placa.isBlank()) {
            veiculoInvalidoDialog = true
            jaProcessou.set(false)
            return
        }
        val sessao = SessaoAtiva(
            estacionamentoId = estId,
            estacionamentoNome = estNome,
            modeloVeiculo = veiculo.nome,
            placa = veiculo.placa,
            precoHora = precoHora,
            pixKey = pixKey
        )
        userViewModel.iniciarSessaoComBackend(sessao, veiculo.placa)
        onEntrada()
    }

    fun iniciarEntradaKioskComVeiculo(
        veiculo: Veiculo,
        estId: Int,
        estNome: String,
        precoHora: Double,
        pixKey: String,
        token: String
    ) {
        if (veiculo.placa.isBlank() || token.isBlank()) {
            veiculoInvalidoDialog = true
            jaProcessou.set(false)
            return
        }
        val sessao = SessaoAtiva(
            estacionamentoId = estId,
            estacionamentoNome = estNome,
            modeloVeiculo = veiculo.nome,
            placa = veiculo.placa,
            precoHora = precoHora,
            pixKey = pixKey
        )
        userViewModel.vincularSessaoKiosk(sessao, veiculo.placa, token)
        onEntrada()
    }

    fun iniciarEntradaComPayload(veiculo: Veiculo, payload: QrCodePayload) {
        val estId = payload.id ?: payload.estacionamentoId ?: 0
        carregarEstacionamentoDoQr(estId) { estacionamento ->
            if (!payload.token.isNullOrBlank()) {
                iniciarEntradaKioskComVeiculo(
                    veiculo = veiculo,
                    estId = estId,
                    estNome = payload.nome ?: estacionamento?.nome ?: "Estacionamento",
                    precoHora = payload.precoHora ?: estacionamento?.precoHora ?: 0.0,
                    pixKey = payload.pixKey ?: estacionamento?.pixKey.orEmpty(),
                    token = payload.token
                )
            } else {
                iniciarEntradaComVeiculo(
                    veiculo = veiculo,
                    estId = estId,
                    estNome = payload.nome ?: estacionamento?.nome ?: "Estacionamento",
                    precoHora = payload.precoHora ?: estacionamento?.precoHora ?: 0.0,
                    pixKey = payload.pixKey ?: estacionamento?.pixKey.orEmpty()
                )
            }
        }
    }

    fun simularEntradaComEstacionamento(est: Estacionamento) {
        when (veiculos.size) {
            0 -> veiculoInvalidoDialog = true
            1 -> {
                val veiculo = veiculos.first()
                iniciarEntradaComVeiculo(
                    veiculo, est.id, est.nome, est.precoHora, est.pixKey.orEmpty()
                )
            }
            else -> {
                pendingDemoEstacionamento = est
                pendingDemoEntrada = true
                showVeiculoPicker = true
            }
        }
    }

    fun simularEntradaAleatoria() {
        if (sessaoAtiva != null) {
            sessaoAtivaDialog = true
            return
        }
        val lista = mapState.estacionamentos
        if (lista.isNotEmpty()) {
            simularEntradaComEstacionamento(lista.random())
            return
        }
        scope.launch {
            loadingEntradaDemo = true
            val estacionamentos = runCatching {
                val response = ParadaCertaClient.service.listarEstacionamentos()
                if (response.isSuccessful) response.body().orEmpty() else emptyList()
            }.getOrDefault(emptyList())
            loadingEntradaDemo = false
            if (estacionamentos.isNotEmpty()) {
                simularEntradaComEstacionamento(estacionamentos.random())
            }
        }
    }

    // Diálogo de seleção de veículo antes de iniciar sessão
    if (showVeiculoPicker) {
        VeiculoPickerDialog(
            veiculos = veiculos,
            onVeiculoSelecionado = { veiculo ->
                showVeiculoPicker = false
                val payload = pendingEntradaPayload
                val lista = mapState.estacionamentos
                when {
                    payload != null -> {
                        pendingEntradaPayload = null
                        iniciarEntradaComPayload(veiculo, payload)
                    }
                    pendingDemoEntrada && pendingDemoEstacionamento != null -> {
                        pendingDemoEntrada = false
                        val est = pendingDemoEstacionamento!!
                        pendingDemoEstacionamento = null
                        iniciarEntradaComVeiculo(
                            veiculo, est.id, est.nome, est.precoHora, est.pixKey.orEmpty()
                        )
                    }
                    pendingDemoEntrada && lista.isNotEmpty() -> {
                        pendingDemoEntrada = false
                        val est = lista.random()
                        iniciarEntradaComVeiculo(
                            veiculo, est.id, est.nome, est.precoHora, est.pixKey.orEmpty()
                        )
                    }
                }
            },
            onDismiss = {
                showVeiculoPicker = false
                pendingEntradaPayload = null
                pendingDemoEntrada = false
                pendingDemoEstacionamento = null
                jaProcessou.set(false)
            }
        )
    }

    // Diálogo: nenhum veículo válido cadastrado/selecionado
    if (veiculoInvalidoDialog) {
        AlertDialog(
            onDismissRequest = {
                veiculoInvalidoDialog = false
                jaProcessou.set(false)
            },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null) },
            title = { Text("Veículo não identificado") },
            text = {
                Text("Não foi possível identificar o veículo selecionado. Selecione novamente e tente outra vez.")
            },
            confirmButton = {
                Button(onClick = {
                    veiculoInvalidoDialog = false
                    jaProcessou.set(false)
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Diálogo: já possui sessão/reserva ativa — bloqueio de nova entrada
    if (sessaoAtivaDialog) {
        val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val mensagem = when {
            sessaoAtiva?.reservado == true && devidaReservaExtra != null ->
                "Você possui uma cobrança pendente de ${moeda.format(devidaReservaExtra!!.valor)} referente à reserva em ${sessaoAtiva!!.estacionamentoNome}. Regularize o pagamento antes de continuar."
            sessaoAtiva?.reservado == true ->
                "Você possui uma reserva ativa em ${sessaoAtiva!!.estacionamentoNome}. Finalize ou cancele antes de escanear uma nova entrada."
            else ->
                "Você já está estacionado em ${sessaoAtiva?.estacionamentoNome ?: "um estacionamento"}. Pague e saia antes de escanear uma nova entrada."
        }
        AlertDialog(
            onDismissRequest = {
                sessaoAtivaDialog = false
                jaProcessou.set(false)
            },
            title = { Text("Vaga já ocupada") },
            text = { Text(mensagem) },
            confirmButton = {
                Button(onClick = {
                    sessaoAtivaDialog = false
                    jaProcessou.set(false)
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Diálogo de confirmação de saída para sessão reservada
    reservaExitDialog?.let { (estId, estNome, _) ->
        val sessao = sessaoAtiva
        val moedaExit = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val extraMsExit = sessao?.let { System.currentTimeMillis() - it.horaEntrada - 3_600_000L } ?: 0L
        val extraMinExit = (extraMsExit / 60_000L).coerceAtLeast(0L)
        val temExtraExit = extraMinExit > 15
        val extraValorExit = if (temExtraExit && sessao != null)
            kotlin.math.ceil(extraMinExit / 60.0) * sessao.precoHora else 0.0
        val horasExit = extraMinExit / 60
        val minsExit = extraMinExit % 60

        AlertDialog(
            onDismissRequest = {
                reservaExitDialog = null
                jaProcessou.set(false)
            },
            icon = {
                Icon(
                    if (temExtraExit) Icons.Default.WarningAmber else Icons.Default.QrCode,
                    contentDescription = null,
                    tint = if (temExtraExit) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface
                )
            },
            title = { Text(if (temExtraExit) "Cobrança adicional" else "Confirmar entrada") },
            text = {
                if (temExtraExit) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sua reserva em $estNome ultrapassou 1 hora.")
                        Text(
                            buildString {
                                append("Tempo excedente: ")
                                if (horasExit > 0) append("${horasExit}h ")
                                append("${minsExit}min")
                            }
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Valor adicional:",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Text(
                                text = moedaExit.format(extraValorExit),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Text("Você possui uma reserva em $estNome. Confirmar entrada no estacionamento? Sua vaga já está paga.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reservaExitDialog = null
                        if (temExtraExit && sessao != null) {
                            userViewModel.setDevidaReservaExtra(
                                DevidaReservaExtra(
                                    sessaoId = sessao.sessaoId,
                                    valor = extraValorExit,
                                    nomeEstacionamento = sessao.estacionamentoNome,
                                    pixKey = sessao.pixKey
                                )
                            )
                            onPagamento(
                                sessao.sessaoId,
                                extraValorExit,
                                sessao.estacionamentoNome,
                                sessao.pixKey
                            )
                        } else {
                            val sessaoId = sessao?.sessaoId ?: ""
                            reservaViewModel.finalizarReserva(sessaoId)
                            userViewModel.encerrarSessao()
                            onEntrada()
                        }
                    },
                    colors = if (temExtraExit)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors()
                ) {
                    Text(if (temExtraExit) "Pagar e confirmar" else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    reservaExitDialog = null
                    jaProcessou.set(false)
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner QR Code", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                cameraPermission.status.isGranted -> {
                    CameraPreviewWithScanner(
                        jaProcessou = jaProcessou,
                        onDemoEntrada = { simularEntradaAleatoria() },
                        isLoadingDemo = loadingEntradaDemo || mapState.isLoading,
                        onQrDetected = { payload ->
                            when ((payload.tipo ?: payload.type ?: "").uppercase()) {
                                "ENTRADA" -> {
                                    when {
                                        sessaoAtiva != null -> sessaoAtivaDialog = true
                                        veiculos.isEmpty() -> {
                                            veiculoInvalidoDialog = true
                                            jaProcessou.set(false)
                                        }
                                        veiculos.size == 1 -> {
                                            iniciarEntradaComPayload(veiculos.first(), payload)
                                        }
                                        else -> {
                                            pendingEntradaPayload = payload
                                            showVeiculoPicker = true
                                        }
                                    }
                                }
                                "PAGAMENTO" -> {
                                    val sessao = sessaoAtiva
                                    val qrSessaoId = payload.sessaoId ?: ""
                                    if (sessao?.reservado == true && qrSessaoId == sessao.sessaoId) {
                                        // QR bate com a reserva ativa: confirmar entrada gratuita
                                        reservaExitDialog = Triple(
                                            sessao.estacionamentoId,
                                            sessao.estacionamentoNome,
                                            sessao.precoHora
                                        )
                                    } else if (sessao?.reservado == true && qrSessaoId != sessao.sessaoId) {
                                        // QR de outro estacionamento: bloqueia
                                        sessaoAtivaDialog = true
                                    } else {
                                        onPagamento(
                                            qrSessaoId,
                                            payload.valor ?: 0.0,
                                            payload.nome ?: "Estacionamento",
                                            payload.pixKey ?: ""
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                cameraPermission.status.shouldShowRationale -> {
                    PermissionRationale(onRequest = { cameraPermission.launchPermissionRequest() })
                }

                else -> {
                    LaunchedEffect(Unit) { cameraPermission.launchPermissionRequest() }
                    PermissionRationale(onRequest = { cameraPermission.launchPermissionRequest() })
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithScanner(
    jaProcessou: java.util.concurrent.atomic.AtomicBoolean,
    onDemoEntrada: () -> Unit,
    isLoadingDemo: Boolean,
    onQrDetected: (QrCodePayload) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gson = remember { Gson() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraProviderRef = remember {
        java.util.concurrent.atomic.AtomicReference<ProcessCameraProvider?>()
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef.get()?.unbindAll()
            barcodeScanner.close()
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview da câmera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.set(cameraProvider)

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                if (!jaProcessou.get()) {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val qrRaw = barcodes
                                                    .firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                    ?.rawValue

                                                if (qrRaw != null && jaProcessou.compareAndSet(false, true)) {
                                                    try {
                                                        val payload = gson.fromJson(qrRaw, QrCodePayload::class.java)
                                                        if (payload?.tipo != null || payload?.type != null) {
                                                            ContextCompat.getMainExecutor(ctx).execute {
                                                                onQrDetected(payload)
                                                            }
                                                        } else {
                                                            // JSON sem campo tipo, libera para nova tentativa
                                                            jaProcessou.set(false)
                                                        }
                                                    } catch (_: Exception) {
                                                        // QR inválido, libera para nova tentativa
                                                        jaProcessou.set(false)
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay escuro ao redor do quadro de scan
        Box(modifier = Modifier.fillMaxSize()) {
            // Quadro de scan central
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            )

            // Cantos decorativos
            ScanCorners(modifier = Modifier.align(Alignment.Center))
        }

        // Instrução na parte inferior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aponte para o QR Code",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Escaneie o QR de entrada para registrar sua chegada,\nou o QR de pagamento para encerrar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))

            // Botão de demonstração de entrada
            OutlinedButton(
                onClick = onDemoEntrada,
                enabled = !isLoadingDemo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.15f)
                )
            ) {
                if (isLoadingDemo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregando...", color = Color.White)
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simular entrada (demo)", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ScanCorners(modifier: Modifier = Modifier) {
    val cornerColor = MaterialTheme.colorScheme.primary
    val cornerSize = 260.dp
    val strokeWidth = 4.dp
    val cornerLength = 32.dp

    Box(modifier = modifier.size(cornerSize)) {
        // Canto superior esquerdo
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(cornerLength)
                .height(strokeWidth)
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(strokeWidth)
                .height(cornerLength)
                .background(cornerColor)
        )
        // Canto superior direito
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(cornerLength)
                .height(strokeWidth)
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(strokeWidth)
                .height(cornerLength)
                .background(cornerColor)
        )
        // Canto inferior esquerdo
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(cornerLength)
                .height(strokeWidth)
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(strokeWidth)
                .height(cornerLength)
                .background(cornerColor)
        )
        // Canto inferior direito
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(cornerLength)
                .height(strokeWidth)
                .background(cornerColor)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(strokeWidth)
                .height(cornerLength)
                .background(cornerColor)
        )
    }
}

@Composable
internal fun VeiculoPickerDialog(
    veiculos: List<Veiculo>,
    onVeiculoSelecionado: (Veiculo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar veículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Com qual veículo você está?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                veiculos.forEach { veiculo ->
                    Card(
                        onClick = { onVeiculoSelecionado(veiculo) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(veiculo.placa, fontWeight = FontWeight.Bold)
                                Text(
                                    "${veiculo.nome} • ${veiculo.cor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permissão de câmera necessária",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Para escanear o QR Code do estacionamento, precisamos de acesso à câmera do seu dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conceder permissão")
        }
    }
}
