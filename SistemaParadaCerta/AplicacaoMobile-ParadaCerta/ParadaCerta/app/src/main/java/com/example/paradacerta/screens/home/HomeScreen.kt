package com.example.paradacerta.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.DevidaReservaExtra
import com.example.paradacerta.models.SessaoAtiva
import androidx.compose.ui.platform.LocalContext
import com.example.paradacerta.notifications.ReservationNotificationScheduler
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.viewmodel.CancelReservaState
import com.example.paradacerta.viewmodel.CobrancaEstadiaViewModel
import com.example.paradacerta.viewmodel.ReservaViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

@Composable
fun HomeScreen(
    userViewModel: UserViewModel = viewModel(),
    onScannerClick: () -> Unit = {},
    onConfirmarReservaClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPagarEstacionamento: (sessaoId: String, valor: Double, nome: String, pixKey: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessaoAtiva by userViewModel.sessaoAtiva.collectAsState()
    val devidaReservaExtra by userViewModel.devidaReservaExtra.collectAsState()
    val veiculos by userViewModel.veiculosData.collectAsState()
    val cliente by userViewModel.userData.collectAsState()

    // Durante uma sessão ativa, o veículo do header deve ser o mesmo da sessão
    // (escolhido pelo usuário no scanner/reserva), encontrado pela placa.
    // Sem sessão ativa, mostra o primeiro veículo cadastrado como referência.
    val veiculoHeader = sessaoAtiva?.let { s ->
        veiculos.firstOrNull { it.placa == s.placa }
    } ?: veiculos.firstOrNull()
    val reservaViewModel: ReservaViewModel = viewModel()
    val cancelState by reservaViewModel.cancelState.collectAsState()
    val cobrancaViewModel: CobrancaEstadiaViewModel = viewModel()
    val cobrancaState by cobrancaViewModel.state.collectAsState()

    var showCancelReservaDialog by remember { mutableStateOf(false) }
    var showFinalizarReservaDialog by remember { mutableStateOf(false) }
    var showCobraExtraDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cancelState.isSuccess) {
        if (cancelState.isSuccess) {
            ReservationNotificationScheduler.cancel(context)
            userViewModel.encerrarSessao()
            reservaViewModel.resetCancelState()
        }
    }

    cobrancaState.errorMessage?.let { erro ->
        AlertDialog(
            onDismissRequest = { cobrancaViewModel.limpar() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Não foi possível calcular o valor") },
            text = { Text(erro) },
            confirmButton = {
                TextButton(onClick = { cobrancaViewModel.limpar() }) { Text("OK") }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = { Icon(Icons.Default.Help, contentDescription = null) },
            title = { Text("Ajuda e Suporte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Use Procurar Estacionamento para encontrar vagas próximas no mapa.")
                    Text("• Escanear QR Code registra sua entrada ou saída de um estacionamento.")
                    Text("• Em Gerenciar Perfil você edita seus dados pessoais e do veículo.")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Entre em contato conosco:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(text = "E-mail", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "atendimento@paradacerta.com", fontSize = 13.sp, color = CinzaMedio)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(text = "Telefone", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "(11) 3000-0000", fontSize = 13.sp, color = CinzaMedio)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(text = "WhatsApp", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "(11) 99999-9999", fontSize = 13.sp, color = CinzaMedio)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Horário de atendimento:\nSegunda a Sexta, das 8h às 18h",
                        fontSize = 13.sp,
                        color = CinzaMedio
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

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
                    val sessao = sessaoAtiva
                    val cpf = cliente?.cpf
                    if (sessao != null && !cpf.isNullOrBlank()) {
                        ReservationNotificationScheduler.cancel(context)
                        // Backend é a fonte da verdade do cálculo. Pedimos o preview e
                        // decidimos: cobrança extra → fluxo Pix, sem extra → encerra direto.
                        reservaViewModel.calcularFinalizacao(sessao.sessaoId, cpf) { calc ->
                            if (calc.exigeCobrancaAdicional) {
                                userViewModel.setDevidaReservaExtra(
                                    DevidaReservaExtra(
                                        sessaoId = sessao.sessaoId,
                                        valor = calc.valorRestante,
                                        nomeEstacionamento = sessao.estacionamentoNome,
                                        pixKey = sessao.pixKey
                                    )
                                )
                                showCobraExtraDialog = true
                            } else {
                                reservaViewModel.finalizarUso(sessao.sessaoId, cpf) {
                                    userViewModel.encerrarSessao()
                                }
                            }
                        }
                    }
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

    if (showCobraExtraDialog && devidaReservaExtra != null) {
        val divida = devidaReservaExtra!!
        val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val extraMin = sessaoAtiva?.let {
            val baseReserva = it.inicioReservaPrevisto ?: it.horaEntrada
            ((System.currentTimeMillis() - baseReserva - 3_600_000L) / 60_000L).coerceAtLeast(0L)
        } ?: 0L
        val horas = extraMin / 60
        val mins = extraMin % 60
        AlertDialog(
            onDismissRequest = { showCobraExtraDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Cobrança adicional") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A reserva cobre apenas 1 hora de estacionamento.")
                    Text(
                        buildString {
                            append("Tempo excedente: ")
                            if (horas > 0) append("${horas}h ")
                            append("${mins}min")
                        }
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor a pagar:", fontWeight = FontWeight.Medium)
                        Text(
                            text = moeda.format(divida.valor),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider()
                    Text(
                        text = "Enquanto não regularizado, o acesso a Escanear QR e Procurar Estacionamento ficará bloqueado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCobraExtraDialog = false
                        onPagarEstacionamento(
                            divida.sessaoId,
                            divida.valor,
                            divida.nomeEstacionamento,
                            divida.pixKey
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Pagar agora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCobraExtraDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    if (showCancelReservaDialog && sessaoAtiva != null) {
        val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val valorPago = sessaoAtiva!!.precoHora
        val reembolso = valorPago * 0.15
        val retido = valorPago - reembolso
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
                        Text(moeda.format(valorPago))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reembolso (15%):", fontWeight = FontWeight.Medium)
                        Text(
                            moeda.format(reembolso),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Retido pelo estacionamento (85%):", fontWeight = FontWeight.Medium)
                        Text(moeda.format(retido), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "Ao cancelar, apenas 15% do valor pago é reembolsado ao motorista. " +
                                "Os 85% restantes ficam com o estacionamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelReservaDialog = false
                        reservaViewModel.cancelarReserva(sessaoAtiva!!.sessaoId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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

    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // SEÇÃO 1: Painel do usuário
            UserHeaderPanel(
                nome = cliente?.nome,
                modeloVeiculo = veiculoHeader?.nome ?: sessaoAtiva?.modeloVeiculo,
                placa = veiculoHeader?.placa ?: sessaoAtiva?.placa ?: cliente?.placa
            )

            // Painel de sessão ativa (se houver)
            sessaoAtiva?.let { sessao ->
                Spacer(modifier = Modifier.height(20.dp))
                if (sessao.reservado) {
                    ReservedSessionPanel(
                        sessao = sessao,
                        modeloVeiculo = sessao.modeloVeiculo.ifBlank {
                            veiculos.firstOrNull { it.placa == sessao.placa }?.nome ?: "Veículo"
                        },
                        cancelState = cancelState,
                        devidaReservaExtra = devidaReservaExtra,
                        onConfirmarReservaClick = onConfirmarReservaClick,
                        onFinalizarReservaClick = {
                            val divida = devidaReservaExtra
                            if (divida != null) {
                                onPagarEstacionamento(
                                    divida.sessaoId,
                                    divida.valor,
                                    divida.nomeEstacionamento,
                                    divida.pixKey
                                )
                            } else {
                                showFinalizarReservaDialog = true
                            }
                        },
                        onCancelarReservaClick = { showCancelReservaDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    ActiveSessionPanel(
                        sessao = sessao,
                        modeloVeiculo = sessao.modeloVeiculo.ifBlank {
                            veiculos.firstOrNull { it.placa == sessao.placa }?.nome ?: "Veículo"
                        },
                        isLoading = cobrancaState.isLoading,
                        onPagarClick = {
                            cobrancaViewModel.calcular(sessao.sessaoId) { cobranca ->
                                onPagarEstacionamento(
                                    sessao.sessaoId,
                                    cobranca.valorTotal,
                                    sessao.estacionamentoNome,
                                    sessao.pixKey
                                )
                                cobrancaViewModel.limpar()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // SEÇÃO 2: Botões de acesso rápido
            Spacer(modifier = Modifier.height(24.dp))
            ActionButtonsGrid(
                onProcurarEstacionamento = onMapClick,
                onScanearQRCode = onScannerClick,
                onGerenciarPerfil = onProfileClick,
                onAjuda = { showHelpDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── SEÇÃO 1 ─────────────────────────────────────────────────────────────────

@Composable
private fun UserHeaderPanel(
    nome: String?,
    modeloVeiculo: String?,
    placa: String?,
    modifier: Modifier = Modifier
) {
    val primeiroNome = nome?.split(" ")?.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (primeiroNome != null) "Olá, $primeiroNome!" else "Bem-vindo!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (modeloVeiculo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = modeloVeiculo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                if (placa != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = placa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 3.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── SEÇÃO 2 ─────────────────────────────────────────────────────────────────

@Composable
private fun ActionButtonsGrid(
    onProcurarEstacionamento: () -> Unit,
    onScanearQRCode: () -> Unit,
    onGerenciarPerfil: () -> Unit,
    onAjuda: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class ButtonItem(val icon: ImageVector, val label: String, val onClick: () -> Unit)

    val buttons = listOf(
        ButtonItem(Icons.Default.Search, "Procurar\nEstacionamento", onProcurarEstacionamento),
        ButtonItem(Icons.Default.QrCodeScanner, "Escanear\nQR Code", onScanearQRCode),
        ButtonItem(Icons.Default.ManageAccounts, "Gerenciar\nPerfil", onGerenciarPerfil),
        ButtonItem(Icons.Default.Help, "Ajuda", onAjuda)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Acesso rápido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        buttons.chunked(2).forEach { rowButtons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowButtons.forEach { item ->
                    ActionButton(
                        icon = item.icon,
                        label = item.label,
                        onClick = item.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Painéis de sessão ativa ──────────────────────────────────────────────────

@Composable
private fun ReservedSessionPanel(
    sessao: SessaoAtiva,
    modeloVeiculo: String,
    cancelState: CancelReservaState,
    devidaReservaExtra: DevidaReservaExtra?,
    onConfirmarReservaClick: () -> Unit,
    onFinalizarReservaClick: () -> Unit,
    onCancelarReservaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aguardandoConfirmacao = sessao.aguardandoConfirmacao
    val emUso = sessao.emUso
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val horaReservaFormatada = SimpleDateFormat("HH:mm", Locale("pt", "BR")).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(Date(sessao.inicioReservaPrevisto ?: sessao.horaEntrada))

    Column(modifier = modifier) {
        // Banner de dívida pendente
        if (devidaReservaExtra != null) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Cobrança adicional pendente",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${moeda.format(devidaReservaExtra.valor)} — Pague para liberar o app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (aguardandoConfirmacao) Icons.Default.QrCodeScanner else Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    aguardandoConfirmacao -> "RESERVA AGUARDANDO CONFIRMAÇÃO"
                    emUso -> "RESERVA EM USO"
                    else -> "VAGA RESERVADA"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            text = when {
                aguardandoConfirmacao ->
                    "Escaneie o QR Code do estacionamento ao chegar para iniciar o uso da vaga."
                emUso ->
                    "Sua reserva está em uso. Toque em \"Finalizar uso da vaga\" ao sair do estacionamento."
                else ->
                    "Dirija-se ao estacionamento para iniciar o uso da vaga reservada"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cancelState.errorMessage != null) {
            Text(
                text = cancelState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            aguardandoConfirmacao -> {
                // Botão primário: Escanear QR Code da reserva (vai para scanner em modo confirmação)
                Button(
                    onClick = onConfirmarReservaClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cancelState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear QR Code da reserva")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancelamento só é permitido enquanto aguardando confirmação
                OutlinedButton(
                    onClick = onCancelarReservaClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cancelState.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                        Text("Cancelar reserva (reembolso de 15%)")
                    }
                }
            }

            emUso -> {
                // Apenas o botão de finalizar uso. Cancelamento bloqueado.
                Button(
                    onClick = onFinalizarReservaClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cancelState.isLoading,
                    colors = if (devidaReservaExtra != null)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else
                        ButtonDefaults.buttonColors()
                ) {
                    Icon(
                        imageVector = if (devidaReservaExtra != null)
                            Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (devidaReservaExtra != null) "Pagar cobrança extra" else "Finalizar uso da vaga")
                }
            }

            else -> {
                // Estado intermediário: nada a fazer
            }
        }
    }
}

@Composable
private fun ActiveSessionPanel(
    sessao: SessaoAtiva,
    modeloVeiculo: String,
    isLoading: Boolean = false,
    onPagarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val horaEntradaFormatada = SimpleDateFormat("HH:mm", Locale("pt", "BR")).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(Date(sessao.horaEntrada))

    Column(modifier = modifier) {
        Text(
            text = "Você está estacionado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calculando...", style = MaterialTheme.typography.titleSmall)
            } else {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pagar estacionamento", style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ou escaneie o QR Code de saída",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
