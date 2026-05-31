package com.example.paradacerta.screens.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.FormaPagamento
import com.example.paradacerta.models.FormaPagamentoRequest
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.SessaoStatus
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.viewmodel.CobrancaEstadiaViewModel
import com.example.paradacerta.viewmodel.PaymentMethodsViewModel
import com.example.paradacerta.viewmodel.ReservaViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val BANDEIRAS = listOf("Visa", "Mastercard", "American Express", "Elo", "Hipercard")

private fun luhnValido(numero: String): Boolean {
    val digits = numero.filter { it.isDigit() }
    if (digits.length < 13 || digits.length > 19) return false
    var soma = 0
    var dobrar = false
    for (i in digits.indices.reversed()) {
        var d = digits[i].digitToInt()
        if (dobrar) { d *= 2; if (d > 9) d -= 9 }
        soma += d
        dobrar = !dobrar
    }
    return soma % 10 == 0
}

private fun validadeNaoExpirada(validade: String): Boolean {
    if (validade.length != 5 || validade[2] != '/') return false
    val mes = validade.substring(0, 2).toIntOrNull() ?: return false
    val ano = validade.substring(3).toIntOrNull() ?: return false
    if (mes < 1 || mes > 12) return false
    val agora = Calendar.getInstance()
    val anoAtual = agora.get(Calendar.YEAR) % 100
    val mesAtual = agora.get(Calendar.MONTH) + 1
    return ano > anoAtual || (ano == anoAtual && mes >= mesAtual)
}

private fun cvvValido(cvv: String): Boolean = cvv.length in 3..4 && cvv.all { it.isDigit() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    sessaoId: String,
    valor: Double,
    nome: String,
    pixKey: String,
    userViewModel: UserViewModel,
    paymentMethodsViewModel: PaymentMethodsViewModel = viewModel(),
    cobrancaViewModel: CobrancaEstadiaViewModel = viewModel(),
    reservaViewModel: ReservaViewModel = viewModel(),
    modoReserva: Boolean = false,
    reservaEstacionamentoId: Int = 0,
    reservaPlaca: String = "",
    reservaInicioPrevisto: String = "",
    reservaHorarioLabel: String = "",
    onBack: () -> Unit,
    onSuccess: (estacionamentoId: Int) -> Unit,
    onReservaCriada: (SessaoAtiva) -> Unit = {}
) {
    val context = LocalContext.current
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val scope = rememberCoroutineScope()

    val userData by userViewModel.userData.collectAsState()
    val sessaoAtiva by userViewModel.sessaoAtiva.collectAsState()
    val cpf = userData?.cpf ?: ""
    val savedMethodsState by paymentMethodsViewModel.state.collectAsState()
    val cobrancaState by cobrancaViewModel.state.collectAsState()
    val reservaState by reservaViewModel.reservaState.collectAsState()
    val finalizarUsoState by reservaViewModel.finalizarUsoState.collectAsState()

    // Busca o detalhamento (preço/hora, tempo permanência, tempo cobrado)
    // apenas para sessões NÃO-reservadas; reservas mostram somente o total.
    val ehReserva = modoReserva || sessaoAtiva?.reservado == true

    LaunchedEffect(Unit) {
        paymentMethodsViewModel.carregar(cpf)
    }

    LaunchedEffect(sessaoId, ehReserva) {
        if (sessaoId.isNotBlank() && !ehReserva) {
            cobrancaViewModel.calcular(sessaoId)
        }
    }

    var metodoPagamento by remember { mutableStateOf("PIX") }
    var selectedSavedCard by remember { mutableStateOf<FormaPagamento?>(null) }
    var showNewCardForm by remember { mutableStateOf(false) }

    var numeroCartao by remember { mutableStateOf(TextFieldValue("")) }
    var nomeCartao by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf(TextFieldValue("")) }
    var cvv by remember { mutableStateOf("") }
    var cvvVisivel by remember { mutableStateOf(false) }
    var expandedBandeira by remember { mutableStateOf(false) }
    var selectedBandeira by remember { mutableStateOf(BANDEIRAS.first()) }

    val digitosCartao = numeroCartao.text.filter { it.isDigit() }
    val cartaoCompleto = digitosCartao.length == 16
    val luhnOk = cartaoCompleto && luhnValido(digitosCartao)
    val validadeOk = validade.text.length == 5 && validadeNaoExpirada(validade.text)
    val cvvOk = cvvValido(cvv)
    val novoCartaoPronto = luhnOk && validadeOk && cvvOk && nomeCartao.isNotBlank()

    var copiado by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSalvarCartaoDialog by remember { mutableStateOf(false) }
    var estIdPosPagamento by remember { mutableStateOf(0) }
    var sessaoReservaCriada by remember { mutableStateOf<SessaoAtiva?>(null) }
    var salvarCartaoAposReserva by remember { mutableStateOf(false) }
    var isEncerrandoSessao by remember { mutableStateOf(false) }
    var pagamentoError by remember { mutableStateOf<String?>(null) }
    val pagamentoEmAndamento = reservaState.isLoading || finalizarUsoState.isLoading || isEncerrandoSessao

    val botaoHabilitado = when {
        metodoPagamento == "PIX" -> pixKey.isNotBlank()
        selectedSavedCard != null -> true
        showNewCardForm -> novoCartaoPronto
        else -> false
    }

    LaunchedEffect(reservaState.isSuccess) {
        if (modoReserva && reservaState.isSuccess) {
            val r = reservaState.resposta ?: return@LaunchedEffect
            val sessao = SessaoAtiva(
                estacionamentoId = r.estacionamentoId,
                estacionamentoNome = r.estacionamentoNome,
                modeloVeiculo = r.modeloVeiculo ?: "",
                placa = r.placa ?: reservaPlaca,
                precoHora = r.precoHora,
                horaEntrada = r.horaEntrada,
                inicioReservaPrevisto = r.inicioReservaPrevisto,
                sessaoId = r.sessaoId,
                pixKey = r.pixKey ?: pixKey,
                reservado = true,
                horarioReserva = reservaHorarioLabel,
                status = SessaoStatus.fromString(r.status ?: SessaoStatus.AGUARDANDO_CONFIRMACAO.name),
                valorPagoAntecipado = r.precoHora
            )
            sessaoReservaCriada = sessao
            reservaViewModel.resetReservaState()
            if (salvarCartaoAposReserva) {
                showSalvarCartaoDialog = true
            } else {
                onReservaCriada(sessao)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text("Confirmar pagamento") },
            text = { Text("Confirmar pagamento de ${moeda.format(valor)} para $nome?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    val estId = sessaoAtiva?.estacionamentoId ?: 0
                    estIdPosPagamento = estId
                    pagamentoError = null
                    val pagouComCartaoNovo =
                        metodoPagamento == "CARTAO" && showNewCardForm && novoCartaoPronto
                    if (modoReserva) {
                        val jaSalvoReserva = pagouComCartaoNovo && cartaoJaSalvo(
                            cards = savedMethodsState.cards,
                            final4 = digitosCartao.takeLast(4),
                            validade = validade.text,
                            bandeira = selectedBandeira
                        )
                        salvarCartaoAposReserva = pagouComCartaoNovo && cpf.isNotBlank() && !jaSalvoReserva
                        reservaViewModel.reservar(
                            cpf = cpf,
                            estacionamentoId = reservaEstacionamentoId,
                            placa = reservaPlaca,
                            inicioReservaPrevisto = reservaInicioPrevisto
                        )
                        return@Button
                    }

                    val jaSalvo = pagouComCartaoNovo && cartaoJaSalvo(
                        cards = savedMethodsState.cards,
                        final4 = digitosCartao.takeLast(4),
                        validade = validade.text,
                        bandeira = selectedBandeira
                    )

                    val deveSalvarCartao = pagouComCartaoNovo && cpf.isNotBlank() && !jaSalvo

                    if (sessaoAtiva?.reservado == true) {
                        if (cpf.isBlank()) {
                            pagamentoError = "Nao foi possivel identificar o usuario para finalizar a reserva."
                            return@Button
                        }
                        reservaViewModel.finalizarUso(
                            sessaoId = sessaoId,
                            cpf = cpf,
                            valorPagoAdicional = valor
                        ) {
                            reservaViewModel.resetFinalizarUsoState()
                            userViewModel.encerrarSessao()
                            if (deveSalvarCartao) {
                                showSalvarCartaoDialog = true
                            } else {
                                onSuccess(estId)
                            }
                        }
                        return@Button
                    }

                    scope.launch {
                        isEncerrandoSessao = true
                        try {
                            val response = ParadaCertaClient.service.encerrarSessao(
                                sessaoId = sessaoId,
                                valorPago = valor,
                                cpf = cpf
                            )
                            if (response.isSuccessful) {
                                userViewModel.encerrarSessao()
                                if (deveSalvarCartao) {
                                    showSalvarCartaoDialog = true
                                } else {
                                    onSuccess(estId)
                                }
                            } else {
                                pagamentoError = extrairMensagem(response, "Erro ao encerrar sessao")
                            }
                        } catch (_: java.net.UnknownHostException) {
                            pagamentoError = "Sem conexao com o servidor"
                        } catch (_: java.net.SocketTimeoutException) {
                            pagamentoError = "Tempo de resposta esgotado"
                        } catch (e: Exception) {
                            pagamentoError = "Erro ao encerrar sessao: ${e.message ?: "Motivo desconhecido"}"
                        } finally {
                            isEncerrandoSessao = false
                        }
                    }

                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showSalvarCartaoDialog) {
        val final4 = digitosCartao.takeLast(4)
        AlertDialog(
            onDismissRequest = {
                showSalvarCartaoDialog = false
                sessaoReservaCriada?.takeIf { modoReserva }?.let(onReservaCriada) ?: onSuccess(estIdPosPagamento)
            },
            icon = { Icon(imageVector = Icons.Default.CreditCard, contentDescription = null) },
            title = { Text("Salvar cartão?") },
            text = {
                Text(
                    "Deseja salvar o cartão $selectedBandeira •••• $final4 " +
                            "para usar em pagamentos futuros? Apenas os últimos 4 dígitos são armazenados. " +
                            "O CVV nunca é salvo."
                )
            },
            confirmButton = {
                Button(onClick = {
                    paymentMethodsViewModel.salvar(
                        FormaPagamentoRequest(
                            clienteCpf = cpf,
                            tipoPagamento = "CARTAO_CREDITO",
                            numeroCartao = digitosCartao,
                            nomeCartao = nomeCartao.trim(),
                            validade = validade.text,
                            bandeira = selectedBandeira
                        )
                    )
                    showSalvarCartaoDialog = false
                    sessaoReservaCriada?.takeIf { modoReserva }?.let(onReservaCriada) ?: onSuccess(estIdPosPagamento)
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSalvarCartaoDialog = false
                    sessaoReservaCriada?.takeIf { modoReserva }?.let(onReservaCriada) ?: onSuccess(estIdPosPagamento)
                }) { Text("Agora não") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pagamento",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = nome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Detalhamento da cobrança (apenas sessões não-reservadas com dados do backend)
            if (!ehReserva) {
                DetalhamentoCobrancaCard(
                    isLoading = cobrancaState.isLoading,
                    precoHora = cobrancaState.cobranca?.precoHora,
                    minutosPermanencia = cobrancaState.cobranca?.minutosPermanencia,
                    minutosCobrados = cobrancaState.cobranca?.minutosCobrados,
                    moeda = moeda
                )
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total a pagar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = moeda.format(valor),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Forma de pagamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = metodoPagamento == "PIX",
                    onClick = {
                        metodoPagamento = "PIX"
                        selectedSavedCard = null
                        showNewCardForm = false
                    },
                    label = { Text("PIX") },
                    leadingIcon = {
                        if (metodoPagamento == "PIX")
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = metodoPagamento == "CARTAO",
                    onClick = { metodoPagamento = "CARTAO" },
                    label = { Text("Cartão") },
                    leadingIcon = {
                        if (metodoPagamento == "CARTAO")
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── PIX ──────────────────────────────────────────────────────────
            if (metodoPagamento == "PIX") {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Pix",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                "Pagar com Pix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            "Chave Pix:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (pixKey.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    pixKey,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix", pixKey))
                                    copiado = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (copiado) "Chave copiada!" else "Copiar chave Pix")
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Chave Pix indisponível no momento. " +
                                                "Tente pagar com cartão ou procure o operador.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Cartão ───────────────────────────────────────────────────────
            if (metodoPagamento == "CARTAO") {
                val savedCards = savedMethodsState.cards.filter { it.tipoPagamento == "CARTAO_CREDITO" }

                if (savedCards.isNotEmpty()) {
                    Text(
                        "Cartões salvos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    savedCards.forEach { card ->
                        val isSelected = selectedSavedCard?.id == card.id
                        val final4 = card.numeroCartao?.takeLast(4) ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSavedCard = if (isSelected) null else card
                                    showNewCardForm = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CreditCard, null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${card.bandeira ?: "Cartão"} •••• $final4",
                                    fontWeight = FontWeight.Medium
                                )
                                if (!card.nomeCartao.isNullOrBlank()) {
                                    Text(
                                        card.nomeCartao,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                if (!showNewCardForm) {
                    OutlinedButton(
                        onClick = { showNewCardForm = true; selectedSavedCard = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (savedCards.isEmpty()) "Inserir dados do cartão" else "Pagar com outro cartão")
                    }
                } else {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Dados do cartão",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = {
                                    showNewCardForm = false
                                    if (savedCards.isEmpty()) metodoPagamento = "PIX"
                                }) { Text("Cancelar") }
                            }

                            ExposedDropdownMenuBox(
                                expanded = expandedBandeira,
                                onExpandedChange = { expandedBandeira = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    value = selectedBandeira,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Bandeira") },
                                    leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedBandeira) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(expandedBandeira, { expandedBandeira = false }) {
                                    BANDEIRAS.forEach { bandeira ->
                                        DropdownMenuItem(
                                            text = { Text(bandeira) },
                                            onClick = { selectedBandeira = bandeira; expandedBandeira = false },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = numeroCartao,
                                onValueChange = { new ->
                                    val digits = new.text.filter { it.isDigit() }.take(16)
                                    val formatted = digits.chunked(4).joinToString(" ")
                                    val oldDigits = new.text.take(new.selection.start).count { it.isDigit() }
                                    var pos = 0; var cnt = 0
                                    for (i in formatted.indices) {
                                        if (formatted[i].isDigit()) cnt++
                                        if (cnt >= oldDigits) { pos = i + 1; break }
                                    }
                                    numeroCartao = TextFieldValue(formatted, TextRange(pos.coerceAtMost(formatted.length)))
                                },
                                label = { Text("Número do cartão") },
                                placeholder = { Text("0000 0000 0000 0000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                                isError = cartaoCompleto && !luhnOk,
                                supportingText = if (cartaoCompleto && !luhnOk) {
                                    { Text("Número inválido", color = MaterialTheme.colorScheme.error) }
                                } else null
                            )

                            OutlinedTextField(
                                value = nomeCartao,
                                onValueChange = { nomeCartao = it },
                                label = { Text("Nome no cartão") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Person, null) }
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = validade,
                                    onValueChange = { new ->
                                        val digits = new.text.filter { it.isDigit() }.take(4)
                                        val formatted = if (digits.length <= 2) digits
                                            else "${digits.substring(0, 2)}/${digits.substring(2)}"
                                        validade = TextFieldValue(formatted, TextRange(formatted.length))
                                    },
                                    label = { Text("Validade") },
                                    placeholder = { Text("MM/AA") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    isError = validade.text.length == 5 && !validadeOk,
                                    supportingText = if (validade.text.length == 5 && !validadeOk) {
                                        { Text("Cartão expirado", color = MaterialTheme.colorScheme.error) }
                                    } else null
                                )
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                                    label = { Text("CVV") },
                                    placeholder = { Text("•••") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = if (cvvVisivel) VisualTransformation.None
                                                           else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { cvvVisivel = !cvvVisivel }) {
                                            Icon(
                                                imageVector = if (cvvVisivel) Icons.Default.VisibilityOff
                                                              else Icons.Default.Visibility,
                                                contentDescription = if (cvvVisivel) "Ocultar CVV" else "Mostrar CVV"
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isError = cvv.length >= 3 && !cvvOk,
                                    supportingText = if (cvv.length >= 3 && !cvvOk) {
                                        { Text("CVV inválido", color = MaterialTheme.colorScheme.error) }
                                    } else null
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock, null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Dados validados localmente. Apenas os últimos 4 dígitos são armazenados. CVV nunca é salvo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            (reservaState.errorMessage ?: finalizarUsoState.errorMessage ?: pagamentoError)?.let { erro ->
                Text(
                    text = erro,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = botaoHabilitado && !pagamentoEmAndamento
            ) {
                if (pagamentoEmAndamento) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (reservaState.isLoading) "Criando reserva..." else "Processando pagamento...")
                } else {
                    Text(
                        text = "Confirmar pagamento",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            val textoConfirmacao = when {
                modoReserva -> "Ao confirmar, sua reserva sera criada apos a confirmacao do pagamento."
                sessaoAtiva?.reservado == true -> "Ao confirmar, o uso da vaga reservada sera finalizado."
                else -> "Ao confirmar, sua sessao de estacionamento sera encerrada."
            }
            Text(
                text = textoConfirmacao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun <T> extrairMensagem(response: retrofit2.Response<T>, fallback: String): String {
    return runCatching {
        val body = response.errorBody()?.string().orEmpty()
        val json = org.json.JSONObject(body)
        json.optString("mensagem")
            .takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
            ?: "$fallback (${response.code()})"
    }.getOrElse {
        "$fallback (${response.code()})"
    }
}

@Composable
private fun DetalhamentoCobrancaCard(
    isLoading: Boolean,
    precoHora: Double?,
    minutosPermanencia: Long?,
    minutosCobrados: Long?,
    moeda: NumberFormat
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Detalhamento da estadia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Calculando cobrança...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (precoHora != null && minutosPermanencia != null && minutosCobrados != null) {
                LinhaDetalhe(rotulo = "Preço por hora", valor = moeda.format(precoHora))
                LinhaDetalhe(
                    rotulo = "Tempo de permanência",
                    valor = formatarMinutos(minutosPermanencia)
                )
                LinhaDetalhe(
                    rotulo = "Tempo cobrado",
                    valor = formatarMinutos(minutosCobrados),
                    destacar = true
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "A cobrança mínima é de 1 hora. Após esse período, o tempo é " +
                                "arredondado para o próximo bloco de 30 minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDetalhe(rotulo: String, valor: String, destacar: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacar) FontWeight.Bold else FontWeight.Medium,
            color = if (destacar) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Considera o cartão duplicado quando já existe na lista um registro do tipo
 * "CARTAO_CREDITO" com os mesmos últimos 4 dígitos, mesma validade e mesma bandeira.
 * O backend só armazena os 4 dígitos finais, então essa é a granularidade
 * possível de comparar sem expor o número completo.
 */
private fun cartaoJaSalvo(
    cards: List<FormaPagamento>,
    final4: String,
    validade: String,
    bandeira: String
): Boolean {
    if (final4.length != 4) return false
    return cards.any { c ->
        c.tipoPagamento == "CARTAO_CREDITO"
            && c.numeroCartao == final4
            && c.validade == validade
            && c.bandeira.equals(bandeira, ignoreCase = true)
    }
}

private fun formatarMinutos(minutos: Long): String {
    val horas = minutos / 60
    val mins = minutos % 60
    return when {
        horas == 0L -> "${mins}min"
        mins == 0L  -> "${horas}h"
        else        -> "${horas}h${String.format("%02d", mins)}min"
    }
}
