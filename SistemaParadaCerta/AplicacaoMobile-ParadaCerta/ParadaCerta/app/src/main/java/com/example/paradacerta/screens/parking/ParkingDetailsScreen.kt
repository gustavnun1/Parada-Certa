package com.example.paradacerta.screens.parking

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.paradacerta.components.SeloQualidade
import com.example.paradacerta.components.StatusFuncionamentoBadge
import com.example.paradacerta.models.AvaliacaoItem
import com.example.paradacerta.models.EstacionamentoFotoItem
import com.example.paradacerta.models.EstacionamentoStatus
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.models.rememberEstacionamentoStatus
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.ui.theme.VerdePrincipal
import com.example.paradacerta.viewmodel.AvaliacaoListViewModel
import com.example.paradacerta.viewmodel.EstacionamentoFotosViewModel
import com.example.paradacerta.viewmodel.ParkingDetailsViewModel
import com.example.paradacerta.viewmodel.ReservaViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDetailsScreen(
    parkingId: Int,
    cpf: String = "",
    veiculos: List<Veiculo> = emptyList(),
    onBackClick: () -> Unit,
    onReservaFeita: (SessaoAtiva) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParkingDetailsViewModel = viewModel(),
    reservaViewModel: ReservaViewModel = viewModel(),
    avaliacaoListViewModel: AvaliacaoListViewModel = viewModel(),
    fotosViewModel: EstacionamentoFotosViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val reservaState by reservaViewModel.reservaState.collectAsState()
    val avaliacaoListState by avaliacaoListViewModel.state.collectAsState()
    val fotosState by fotosViewModel.state.collectAsState()
    val context = LocalContext.current
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    var showReservaDialog by remember { mutableStateOf(false) }
    var showAvaliacoes by remember { mutableStateOf(false) }
    var horarioSelecionado by remember { mutableStateOf<String?>(null) }
    var veiculoSelecionado by remember(veiculos) { mutableStateOf(veiculos.firstOrNull()) }
    var veiculoInvalidoDialog by remember { mutableStateOf(false) }

    val estacionamento = state.estacionamento

    // Reage ao sucesso da reserva
    LaunchedEffect(reservaState.isSuccess) {
        if (reservaState.isSuccess && estacionamento != null) {
            val r = reservaState.resposta ?: return@LaunchedEffect
            // Confia primeiro no veículo selecionado pelo usuário; usa o retorno
            // do backend apenas como complemento quando o local estiver vazio.
            val placaFinal = veiculoSelecionado?.placa?.takeIf { it.isNotBlank() }
                ?: r.placa?.takeIf { it.isNotBlank() }
                ?: ""
            val modeloFinal = veiculoSelecionado?.nome?.takeIf { it.isNotBlank() }
                ?: r.modeloVeiculo?.takeIf { it.isNotBlank() }
                ?: ""
            if (placaFinal.isBlank()) {
                veiculoInvalidoDialog = true
                reservaViewModel.resetReservaState()
                return@LaunchedEffect
            }
            val sessao = SessaoAtiva(
                estacionamentoId = estacionamento.id,
                estacionamentoNome = estacionamento.nome,
                modeloVeiculo = modeloFinal,
                placa = placaFinal,
                precoHora = estacionamento.precoHora,
                horaEntrada = r.horaEntrada,
                sessaoId = r.sessaoId,
                pixKey = r.pixKey ?: "",
                reservado = true,
                horarioReserva = horarioSelecionado
            )
            onReservaFeita(sessao)
            reservaViewModel.resetReservaState()
        }
    }

    if (veiculoInvalidoDialog) {
        AlertDialog(
            onDismissRequest = { veiculoInvalidoDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Veículo não identificado") },
            text = {
                Text("Não foi possível identificar o veículo selecionado. Selecione novamente e tente outra vez.")
            },
            confirmButton = {
                Button(onClick = { veiculoInvalidoDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    LaunchedEffect(parkingId) {
        viewModel.carregar(parkingId)
        avaliacaoListViewModel.carregar(parkingId)
        fotosViewModel.carregar(parkingId)
    }

    // Bottom sheet de reserva
    if (showReservaDialog && estacionamento != null) {
        val p = estacionamento
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val horarios = remember(p.horarioAbertura, p.horarioFechamento) {
            gerarHorarios(p.horarioAbertura, p.horarioFechamento)
        }
        val valorReembolso = p.precoHora * 0.15
        val valorRetido    = p.precoHora - valorReembolso

        ModalBottomSheet(
            onDismissRequest = {
                horarioSelecionado = null
                showReservaDialog = false
                reservaViewModel.resetReservaState()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Cabeçalho
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reservar Vaga",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = p.nome, style = MaterialTheme.typography.bodyMedium, color = CinzaMedio)
                }

                HorizontalDivider()

                // Horários disponíveis
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = "Horário de entrada", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    if (horarios.isEmpty()) {
                        Text(
                            text = "Horários não informados para este estacionamento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CinzaMedio
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            horarios.forEach { horario ->
                                FilterChip(
                                    selected = horarioSelecionado == horario,
                                    onClick = { horarioSelecionado = horario },
                                    label = { Text(horario) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Veículo
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = "Veículo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    if (veiculos.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CinzaMedio,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Nenhum veículo cadastrado. Adicione um veículo no seu perfil.",
                                    fontSize = 12.sp,
                                    color = CinzaMedio
                                )
                            }
                        }
                    } else if (veiculos.size == 1) {
                        val v = veiculos.first()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(text = v.nome, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(text = "Placa: ${v.placa}", fontSize = 12.sp, color = CinzaMedio)
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerdePrincipal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        // Múltiplos veículos: mostrar picker
                        veiculos.forEach { v ->
                            val selecionado = veiculoSelecionado?.placa == v.placa
                            Card(
                                onClick = { veiculoSelecionado = v },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selecionado)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(text = v.nome, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(text = "Placa: ${v.placa}", fontSize = 12.sp, color = CinzaMedio)
                                    }
                                    if (selecionado) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = VerdePrincipal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Política de cancelamento e reembolso
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = "Política de cancelamento", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Valor cobrado na reserva:", fontSize = 13.sp, color = CinzaMedio)
                                Text(moeda.format(p.precoHora), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reembolso no cancelamento:", fontSize = 13.sp, color = CinzaMedio)
                                Text("15%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VerdePrincipal)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Valor devolvido ao motorista:", fontSize = 13.sp, color = CinzaMedio)
                                Text(
                                    moeda.format(valorReembolso),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerdePrincipal
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Retido pelo estacionamento (85%):", fontSize = 13.sp, color = CinzaMedio)
                                Text(moeda.format(valorRetido), fontSize = 13.sp, color = CinzaMedio)
                            }
                            HorizontalDivider()
                            Text(
                                text = "Ao cancelar a reserva, apenas 15% do valor pago é reembolsado. " +
                                        "Os 85% restantes ficam com o estacionamento.",
                                fontSize = 11.sp,
                                color = CinzaMedio,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Mensagem de erro
                reservaState.errorMessage?.let { erro ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = erro,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            horarioSelecionado = null
                            showReservaDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    val placaSelecionadaValida =
                        veiculoSelecionado?.placa?.isNotBlank() == true
                    Button(
                        onClick = {
                            val vPlaca = veiculoSelecionado?.placa.orEmpty()
                            if (vPlaca.isBlank()) {
                                showReservaDialog = false
                                veiculoInvalidoDialog = true
                                return@Button
                            }
                            reservaViewModel.reservar(cpf, p.id, vPlaca)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !reservaState.isLoading
                            && placaSelecionadaValida
                            && (horarios.isEmpty() || horarioSelecionado != null)
                    ) {
                        if (reservaState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processando...")
                        } else {
                            Text("Confirmar reserva")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.carregar(parkingId) }) {
                            Text("Tentar novamente")
                        }
                    }
                }

                estacionamento != null -> {
                    val p = estacionamento
                    val statusFuncionamento by rememberEstacionamentoStatus(
                        p.horarioAbertura, p.horarioFechamento
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = p.nome,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        StatusFuncionamentoBadge(status = statusFuncionamento)

                        if (p.avaliacaoMedia > 4.5) {
                            SeloQualidade()
                        }

                        if (!p.descricao.isNullOrBlank()) {
                            Text(
                                text = p.descricao,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CinzaMedio
                            )
                        }

                        HorizontalDivider()

                        // Fotos do estacionamento
                        FotosEstacionamentoSection(
                            isLoading = fotosState.isLoading,
                            fotos = fotosState.fotos,
                            errorMessage = fotosState.errorMessage
                        )

                        HorizontalDivider()

                        // Endereço
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Endereço",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(text = p.endereco, color = CinzaMedio, fontSize = 14.sp)
                            }
                        }

                        // Horário
                        if (p.horarioAbertura != null && p.horarioFechamento != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Horário",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${p.horarioAbertura} – ${p.horarioFechamento}",
                                        color = CinzaMedio,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Preço por hora
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Preço por hora",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "R$ ${String.format("%.2f", p.precoHora)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Vagas disponíveis
                        val vagasColor = if (p.qtdVagasDisponiveis > 0) VerdePrincipal else Color(0xFFF44336)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = vagasColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Vagas disponíveis",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${p.qtdVagasDisponiveis} / ${p.qtdVagasTotais}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = vagasColor
                            )
                        }

                        // Avaliação (clicável para ver comentários)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAvaliacoes = !showAvaliacoes
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Avaliação",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Toque para ver comentários",
                                        fontSize = 11.sp,
                                        color = CinzaMedio
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = String.format("%.1f", p.avaliacaoMedia),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = if (showAvaliacoes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = CinzaMedio,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Painel de avaliações expansível
                        if (showAvaliacoes) {
                            AvaliacoesPainel(
                                avaliacoes = avaliacaoListState.avaliacoes,
                                isLoading = avaliacaoListState.isLoading
                            )
                        }

                        HorizontalDivider()

                        // ── Seção de Reserva ─────────────────────────────────────────────
                        Text(
                            text = "Reservar Vaga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        when {
                            !p.permiteReserva -> {
                                // Estacionamento não oferece reservas
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = CinzaMedio
                                        )
                                        Text(
                                            text = "Este estacionamento não oferece reservas de vagas",
                                            fontSize = 13.sp,
                                            color = CinzaMedio
                                        )
                                    }
                                }
                            }

                            p.qtdVagasReservaveis <= 0 -> {
                                // Sem vagas reserváveis disponíveis
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Nenhuma vaga disponível para reserva no momento",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            else -> {
                                // Reserva disponível
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BookmarkAdd,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Vagas para reserva",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${p.qtdVagasReservaveis} disponíveis",
                                                fontWeight = FontWeight.Bold,
                                                color = VerdePrincipal
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Valor da reserva:",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = moeda.format(p.precoHora),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Button(
                                            onClick = { showReservaDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !statusFuncionamento.ehFechado
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (statusFuncionamento.ehFechado) "Estacionamento fechado"
                                                else "Reservar vaga"
                                            )
                                        }
                                        if (statusFuncionamento.ehFechado) {
                                            Text(
                                                text = "Reserva indisponível enquanto o estacionamento estiver fechado.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Rota ─────────────────────────────────────────────────────────
                        Button(
                            onClick = {
                                val uri = Uri.parse(
                                    "google.navigation:q=${p.latitude},${p.longitude}&mode=d"
                                )
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    val fallback = Uri.parse(
                                        "geo:${p.latitude},${p.longitude}?q=${p.latitude},${p.longitude}(${p.nome})"
                                    )
                                    context.startActivity(Intent(Intent.ACTION_VIEW, fallback))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir rota no Google Maps")
                        }
                    }
                }
            }
        }
    }
}

// ── Painel de avaliações ──────────────────────────────────────────────────────

@Composable
private fun AvaliacoesPainel(
    avaliacoes: List<AvaliacaoItem>,
    isLoading: Boolean
) {
    val starColor = Color(0xFFFFC107)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Comentários",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                avaliacoes.isEmpty() -> {
                    Text(
                        text = "Nenhuma avaliação ainda.",
                        fontSize = 13.sp,
                        color = CinzaMedio
                    )
                }

                else -> {
                    avaliacoes.take(10).forEach { avaliacao ->
                        AvaliacaoCard(avaliacao = avaliacao, starColor = starColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvaliacaoCard(avaliacao: AvaliacaoItem, starColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { i ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i < avaliacao.nota) starColor else Color(0xFFE0E0E0),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (!avaliacao.comentario.isNullOrBlank()) {
            Text(text = avaliacao.comentario, fontSize = 13.sp)
        } else {
            Text(text = "Sem comentário", fontSize = 12.sp, color = CinzaMedio)
        }
    }
}

// ── Seção: Fotos do estacionamento ─────────────────────────────────────────────

@Composable
private fun FotosEstacionamentoSection(
    isLoading: Boolean,
    fotos: List<EstacionamentoFotoItem>,
    errorMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Fotos do estacionamento",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CinzaMedio,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Não foi possível carregar as fotos no momento.",
                            fontSize = 13.sp,
                            color = CinzaMedio
                        )
                    }
                }
            }

            fotos.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = CinzaMedio,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Este estacionamento ainda não possui fotos cadastradas.",
                            fontSize = 13.sp,
                            color = CinzaMedio
                        )
                    }
                }
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(fotos, key = { it.id }) { foto ->
                        FotoCard(foto = foto)
                    }
                }
            }
        }
    }
}

@Composable
private fun FotoCard(foto: EstacionamentoFotoItem) {
    val context = LocalContext.current
    val url = remember(foto.url) { montarUrlFoto(foto.url) }

    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Foto do estacionamento",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = CinzaMedio,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Imagem indisponível",
                        fontSize = 11.sp,
                        color = CinzaMedio
                    )
                }
            }
        )
    }
}

/** Constrói a URL absoluta da foto a partir do path retornado pela API (`/uploads/...`). */
private fun montarUrlFoto(urlRetornada: String): String {
    val raiz = ParadaCertaClient.serverRoot
    return when {
        urlRetornada.startsWith("http://", ignoreCase = true) ||
            urlRetornada.startsWith("https://", ignoreCase = true) -> urlRetornada
        urlRetornada.startsWith("/") -> raiz + urlRetornada
        else -> "$raiz/$urlRetornada"
    }
}

private fun gerarHorarios(abertura: String?, fechamento: String?): List<String> {
    if (abertura == null || fechamento == null) return emptyList()
    return try {
        val p1 = abertura.trim().split(":")
        val p2 = fechamento.trim().split(":")
        val inicioMin = p1[0].toInt() * 60 + p1[1].toInt()
        val fimMin = p2[0].toInt() * 60 + p2[1].toInt() - 60
        val slots = mutableListOf<String>()
        var min = inicioMin
        while (min <= fimMin) {
            slots.add(String.format("%02d:%02d", min / 60, min % 60))
            min += 60
        }
        slots
    } catch (e: Exception) {
        emptyList()
    }
}
