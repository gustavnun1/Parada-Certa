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
import com.example.paradacerta.models.QrCodePayload
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.viewmodel.MapViewModel
import com.example.paradacerta.viewmodel.ReservaViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val veiculo by userViewModel.veiculoData.collectAsState()
    val mapState by mapViewModel.mapState.collectAsState()
    val sessaoAtiva by userViewModel.sessaoAtiva.collectAsState()
    val reservaViewModel: ReservaViewModel = viewModel()
    var loadingEntradaDemo by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Diálogo: tentativa de entrada com sessão já ativa
    var sessaoAtivaDialog by remember { mutableStateOf(false) }

    // Estado para o diálogo de confirmação de saída de sessão reservada
    // Triple(estacionamentoId, estacionamentoNome, precoHora)
    var reservaExitDialog by remember { mutableStateOf<Triple<Int, String, Double>?>(null) }

    // AtomicBoolean elevado para o nível do composable para poder resetar a partir do diálogo
    val jaProcessou = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // Carrega estacionamentos para o botão de demo
    LaunchedEffect(Unit) {
        mapViewModel.carregarTodos()
    }

    fun simularEntradaAleatoria() {
        if (sessaoAtiva != null) {
            sessaoAtivaDialog = true
            return
        }
        val lista = mapState.estacionamentos
        if (lista.isEmpty()) return
        val est = lista.random()
        val sessao = SessaoAtiva(
            estacionamentoId = est.id,
            estacionamentoNome = est.nome,
            modeloVeiculo = veiculo?.nome ?: "Veículo",
            placa = veiculo?.placa ?: "",
            precoHora = est.precoHora
        )
        userViewModel.iniciarSessaoComBackend(sessao)
        onEntrada()
    }

    // Diálogo: já possui sessão/reserva ativa — bloqueio de nova entrada
    if (sessaoAtivaDialog) {
        val mensagem = if (sessaoAtiva?.reservado == true)
            "Você possui uma reserva ativa em ${sessaoAtiva!!.estacionamentoNome}. Finalize ou cancele antes de escanear uma nova entrada."
        else
            "Você já está estacionado em ${sessaoAtiva?.estacionamentoNome ?: "um estacionamento"}. Pague e saia antes de escanear uma nova entrada."
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
        AlertDialog(
            onDismissRequest = {
                reservaExitDialog = null
                jaProcessou.set(false)
            },
            icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
            title = { Text("Confirmar saída") },
            text = {
                Text("Você possui uma reserva em $estNome. Confirmar entrada no estacionamento? Sua vaga já está paga.")
            },
            confirmButton = {
                Button(onClick = {
                    val sessaoId = sessaoAtiva?.sessaoId ?: ""
                    reservaExitDialog = null
                    reservaViewModel.finalizarReserva(sessaoId)  // fecha sessão no DB (trigger cuida da vaga)
                    userViewModel.encerrarSessao()
                    onEntrada()
                }) {
                    Text("Confirmar")
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
                            when (payload.tipo.uppercase()) {
                                "ENTRADA" -> {
                                    if (sessaoAtiva != null) {
                                        // Já tem sessão ou reserva ativa — bloqueia e avisa
                                        sessaoAtivaDialog = true
                                    } else {
                                        val sessao = SessaoAtiva(
                                            estacionamentoId = payload.id ?: 0,
                                            estacionamentoNome = payload.nome ?: "Estacionamento",
                                            modeloVeiculo = veiculo?.nome ?: "Veículo",
                                            placa = veiculo?.placa ?: "",
                                            precoHora = payload.precoHora ?: 0.0
                                        )
                                        userViewModel.iniciarSessaoComBackend(sessao)
                                        onEntrada()
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview da câmera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()

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
                                                        if (payload?.tipo != null) {
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
