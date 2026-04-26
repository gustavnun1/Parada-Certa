package com.example.paradacerta.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.components.AdBanner
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.viewmodel.ReservaViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userViewModel: UserViewModel = viewModel(),
    onScannerClick: () -> Unit = {},
    onPagarEstacionamento: (sessaoId: String, valor: Double, nome: String, pixKey: String) -> Unit = { _, _, _, _ -> },
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sessaoAtiva by userViewModel.sessaoAtiva.collectAsState()
    val veiculo by userViewModel.veiculoData.collectAsState()
    val cliente by userViewModel.userData.collectAsState()
    val reservaViewModel: ReservaViewModel = viewModel()
    val cancelState by reservaViewModel.cancelState.collectAsState()

    var showCancelReservaDialog by remember { mutableStateOf(false) }
    var showFinalizarReservaDialog by remember { mutableStateOf(false) }

    // Reage ao sucesso do cancelamento da reserva
    // O trigger TR_Sessao_AtualizaVagas já cuida de incrementar qtdVagasDisponiveis no DB
    LaunchedEffect(cancelState.isSuccess) {
        if (cancelState.isSuccess) {
            userViewModel.encerrarSessao()
            reservaViewModel.resetCancelState()
        }
    }

    // Diálogo de confirmação de finalização de reserva
    if (showFinalizarReservaDialog && sessaoAtiva != null) {
        AlertDialog(
            onDismissRequest = { showFinalizarReservaDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text("Finalizar uso da vaga") },
            text = {
                Text("Confirmar que utilizou a vaga reservada em ${sessaoAtiva!!.estacionamentoNome} e deseja encerrar?")
            },
            confirmButton = {
                Button(onClick = {
                    showFinalizarReservaDialog = false
                    reservaViewModel.finalizarReserva(sessaoAtiva!!.sessaoId)
                    userViewModel.encerrarSessao()
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizarReservaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmação de cancelamento
    if (showCancelReservaDialog && sessaoAtiva != null) {
        val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val reembolso = sessaoAtiva!!.precoHora * 0.9
        AlertDialog(
            onDismissRequest = { showCancelReservaDialog = false },
            icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            title = { Text("Cancelar reserva") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deseja cancelar sua reserva em ${sessaoAtiva!!.estacionamentoNome}?")
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor pago:", fontWeight = FontWeight.Medium)
                        Text(moeda.format(sessaoAtiva!!.precoHora))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reembolso (90%):", fontWeight = FontWeight.Medium)
                        Text(
                            moeda.format(reembolso),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelReservaDialog = false
                        reservaViewModel.cancelarReserva(sessaoAtiva!!.sessaoId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !cancelState.isLoading
                ) {
                    Text("Cancelar reserva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReservaDialog = false }) {
                    Text("Manter reserva")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parada Certa",
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
            AdBanner(
                isPremium = isPremium,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.Center)
                    .alpha(0.05f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    sessaoAtiva == null -> {
                        EmptyParkingState(
                            nomeUsuario = cliente?.nome?.split(" ")?.firstOrNull(),
                            onScannerClick = onScannerClick
                        )
                    }

                    sessaoAtiva!!.reservado -> {
                        ReservedSessionPanel(
                            sessao = sessaoAtiva!!,
                            modeloVeiculo = veiculo?.nome ?: sessaoAtiva!!.modeloVeiculo,
                            cancelState = cancelState,
                            onFinalizarReservaClick = { showFinalizarReservaDialog = true },
                            onCancelarReservaClick = { showCancelReservaDialog = true }
                        )
                    }

                    else -> {
                        ActiveSessionPanel(
                            sessao = sessaoAtiva!!,
                            modeloVeiculo = veiculo?.nome ?: sessaoAtiva!!.modeloVeiculo,
                            onPagarClick = {
                                val elapsedHours = (System.currentTimeMillis() - sessaoAtiva!!.horaEntrada) / 3_600_000.0
                                val valor = (Math.round(elapsedHours * sessaoAtiva!!.precoHora * 100) / 100.0)
                                    .coerceAtLeast(0.01)
                                onPagarEstacionamento(
                                    sessaoAtiva!!.sessaoId,
                                    valor,
                                    sessaoAtiva!!.estacionamentoNome,
                                    sessaoAtiva!!.pixKey
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyParkingState(nomeUsuario: String?, onScannerClick: () -> Unit = {}) {
    Icon(
        imageVector = Icons.Default.LocalParking,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (nomeUsuario != null) {
        Text(
            text = "Olá, $nomeUsuario!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(
        text = "Você não está em nenhuma vaga no momento",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Use o scanner na aba abaixo para registrar sua entrada em um estacionamento",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onScannerClick)
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Escaneie o QR Code na entrada",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReservedSessionPanel(
    sessao: SessaoAtiva,
    modeloVeiculo: String,
    cancelState: com.example.paradacerta.viewmodel.CancelReservaState,
    onFinalizarReservaClick: () -> Unit,
    onCancelarReservaClick: () -> Unit
) {
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val horaReservaFormatada = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
        .format(Date(sessao.horaEntrada))

    // Badge de reservado
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "VAGA RESERVADA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = sessao.estacionamentoNome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = modeloVeiculo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = sessao.placa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Valor pago",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = moeda.format(sessao.precoHora),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (sessao.horarioReserva != null) "Horário reservado" else "Reservado às",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sessao.horarioReserva ?: horaReservaFormatada,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Dirija-se ao estacionamento e escaneie o QR Code de saída para confirmar sua entrada",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Erro de cancelamento
    if (cancelState.errorMessage != null) {
        Text(
            text = cancelState.errorMessage!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Botão Finalizar uso (principal)
    Button(
        onClick = onFinalizarReservaClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !cancelState.isLoading
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Finalizar uso da vaga")
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Botão Cancelar reserva (secundário, destrutivo)
    OutlinedButton(
        onClick = onCancelarReservaClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !cancelState.isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        if (cancelState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancelando...")
        } else {
            Text("Cancelar reserva (reembolso de 90%)")
        }
    }
}

@Composable
private fun ActiveSessionPanel(sessao: SessaoAtiva, modeloVeiculo: String, onPagarClick: () -> Unit = {}) {
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val horaEntradaFormatada = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
        .format(Date(sessao.horaEntrada))

    Text(
        text = "Você está estacionado",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = sessao.estacionamentoNome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = modeloVeiculo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = sessao.placa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Preço / hora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = moeda.format(sessao.precoHora),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Entrada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = horaEntradaFormatada,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onPagarClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Pagar estacionamento", style = MaterialTheme.typography.titleSmall)
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Ou escaneie o QR Code de saída",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
