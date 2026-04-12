package com.example.paradacerta.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            // Ícone de carro semi-opaco como watermark de fundo
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
                if (sessaoAtiva == null) {
                    // ── Estado: sem vaga ──────────────────────────────────
                    EmptyParkingState(
                        nomeUsuario = cliente?.nome?.split(" ")?.firstOrNull(),
                        onScannerClick = onScannerClick
                    )
                } else {
                    // ── Estado: em vaga ───────────────────────────────────
                    val sessao = sessaoAtiva!!
                    ActiveSessionPanel(
                        sessao = sessao,
                        modeloVeiculo = veiculo?.nome ?: sessao.modeloVeiculo,
                        onPagarClick = {
                            val elapsedHours = (System.currentTimeMillis() - sessao.horaEntrada) / 3_600_000.0
                            val valor = (Math.round(elapsedHours * sessao.precoHora * 100) / 100.0)
                                .coerceAtLeast(0.01)
                            onPagarEstacionamento(sessao.sessaoId, valor, sessao.estacionamentoNome, sessao.pixKey)
                        }
                    )
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
            // Nome do estacionamento
            Text(
                text = sessao.estacionamentoNome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Veículo + placa
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

            // Preço e hora de entrada
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
