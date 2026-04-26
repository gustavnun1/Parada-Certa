package com.example.paradacerta.screens.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.FormaPagamentoRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.viewmodel.PremiumPaymentViewModel
import com.example.paradacerta.viewmodel.UserViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.text.TextRange

private val BANDEIRAS = listOf("Visa", "Mastercard", "American Express", "Elo", "Hipercard")

// ── Validações locais (primeira linha de defesa, antes de enviar ao backend) ──

private fun luhnValido(numero: String): Boolean {
    val digits = numero.filter { it.isDigit() }
    if (digits.length < 13 || digits.length > 19) return false
    var soma = 0
    var dobrar = false
    for (i in digits.indices.reversed()) {
        var d = digits[i].digitToInt()
        if (dobrar) {
            d *= 2
            if (d > 9) d -= 9
        }
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
fun PremiumPaymentScreen(
    planType: String,
    planPrice: Double,
    userViewModel: UserViewModel,
    onBackClick: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PremiumPaymentViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val userData by userViewModel.userData.collectAsState()
    val context = LocalContext.current
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    val planLabel = if (planType == "MENSAL") "Mensal" else "Anual"
    val planDescription = if (planType == "MENSAL") "Cobrado mensalmente" else "Cobrado anualmente"

    // PIX key buscada do backend — nunca hardcoded no APK
    var pixKeyEmpresa by remember { mutableStateOf("") }
    var loadingPixKey by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        runCatching {
            val response = ParadaCertaClient.service.obterPixKeyEmpresa()
            if (response.isSuccessful) pixKeyEmpresa = response.body()?.pixKey ?: ""
        }
        loadingPixKey = false
    }

    var metodoPagamento by remember { mutableStateOf("PIX") }

    // Campos do cartão
    var numeroCartao by remember { mutableStateOf(TextFieldValue("")) }
    var nomeCartao   by remember { mutableStateOf("") }
    var validade     by remember { mutableStateOf(TextFieldValue("")) }
    var cvv          by remember { mutableStateOf("") }
    var cvvVisivel   by remember { mutableStateOf(false) }
    var expandedBandeira  by remember { mutableStateOf(false) }
    var selectedBandeira  by remember { mutableStateOf(BANDEIRAS.first()) }

    var copiado           by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSaveCardDialog by remember { mutableStateOf(false) }
    var pendingCartaoRequest by remember { mutableStateOf<FormaPagamentoRequest?>(null) }

    // Validações em tempo real para habilitar o botão de confirmação
    val digitosCartao = numeroCartao.text.filter { it.isDigit() }
    val cartaoCompleto = digitosCartao.length == 16
    val luhnOk         = cartaoCompleto && luhnValido(digitosCartao)
    val validadeOk     = validade.text.length == 5 && validadeNaoExpirada(validade.text)
    val cvvOk          = cvvValido(cvv)
    val nomeOk         = nomeCartao.isNotBlank()

    val cartaoPronto   = luhnOk && validadeOk && cvvOk && nomeOk

    val botaoHabilitado = !state.isLoading && (
        metodoPagamento == "PIX" || cartaoPronto
    )

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            userViewModel.setPremium(true)
            onPaymentSuccess()
        }
    }

    state.errorMessage?.let { erro ->
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Erro no pagamento") },
            text  = { Text(erro) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) { Text("OK") }
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon  = { Icon(Icons.Default.Star, contentDescription = null) },
            title = { Text("Confirmar assinatura") },
            text  = {
                Text("Confirmar assinatura do plano Premium $planLabel por ${moeda.format(planPrice)}?")
            },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    val cpf = userData?.cpf ?: return@Button
                    if (metodoPagamento == "CARTAO") {
                        // CVV validado localmente mas NÃO enviado ao backend
                        val cartao = FormaPagamentoRequest(
                            clienteCpf    = cpf,
                            tipoPagamento = "CARTAO_CREDITO",
                            numeroCartao  = digitosCartao,   // backend guarda só últimos 4
                            nomeCartao    = nomeCartao.trim(),
                            validade      = validade.text,
                            bandeira      = selectedBandeira
                        )
                        pendingCartaoRequest = cartao
                        showSaveCardDialog = true
                    } else {
                        viewModel.confirmarAssinatura(cpf, false, null)
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showSaveCardDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveCardDialog = false
                val cpf = userData?.cpf ?: return@AlertDialog
                viewModel.confirmarAssinatura(cpf, false, null)
            },
            icon  = { Icon(Icons.Default.CreditCard, contentDescription = null) },
            title = { Text("Salvar cartão?") },
            text  = { Text("Deseja salvar este cartão para futuras compras e renovações?") },
            confirmButton = {
                Button(onClick = {
                    showSaveCardDialog = false
                    val cpf = userData?.cpf ?: return@Button
                    viewModel.confirmarAssinatura(cpf, true, pendingCartaoRequest)
                }) { Text("Sim, salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveCardDialog = false
                    val cpf = userData?.cpf ?: return@TextButton
                    viewModel.confirmarAssinatura(cpf, false, null)
                }) { Text("Não") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assinatura Premium") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Card do plano
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Text("Plano Premium $planLabel",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(moeda.format(planPrice),
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(planDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Text("Forma de pagamento",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = metodoPagamento == "PIX",
                    onClick  = { metodoPagamento = "PIX" },
                    label    = { Text("PIX") },
                    leadingIcon = {
                        if (metodoPagamento == "PIX")
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = metodoPagamento == "CARTAO",
                    onClick  = { metodoPagamento = "CARTAO" },
                    label    = { Text("Cartão de Crédito") },
                    leadingIcon = {
                        if (metodoPagamento == "CARTAO")
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (metodoPagamento == "PIX") {
                PixPaymentSection(
                    pixKey  = pixKeyEmpresa,
                    loading = loadingPixKey,
                    valor   = moeda.format(planPrice),
                    copiado = copiado,
                    onCopiar = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix", pixKeyEmpresa))
                        copiado = true
                    }
                )
            } else {
                CardPaymentSection(
                    numeroCartao     = numeroCartao,
                    nomeCartao       = nomeCartao,
                    validade         = validade,
                    cvv              = cvv,
                    cvvVisivel       = cvvVisivel,
                    selectedBandeira = selectedBandeira,
                    expandedBandeira = expandedBandeira,
                    luhnOk           = luhnOk,
                    validadeOk       = validadeOk,
                    cvvOk            = cvvOk,
                    onNumeroCartaoChange = { new ->
                        val digits    = new.text.filter { it.isDigit() }.take(16)
                        val formatted = digits.chunked(4).joinToString(" ")
                        val oldDigits = new.text.take(new.selection.start).count { it.isDigit() }
                        var pos = 0; var cnt = 0
                        for (i in formatted.indices) {
                            if (formatted[i].isDigit()) cnt++
                            if (cnt >= oldDigits) { pos = i + 1; break }
                        }
                        numeroCartao = TextFieldValue(formatted, TextRange(pos.coerceAtMost(formatted.length)))
                    },
                    onNomeCartaoChange = { nomeCartao = it },
                    onValidadeChange   = { new ->
                        val digits    = new.text.filter { it.isDigit() }.take(4)
                        val formatted = if (digits.length <= 2) digits
                            else "${digits.substring(0, 2)}/${digits.substring(2)}"
                        validade = TextFieldValue(formatted, TextRange(formatted.length))
                    },
                    onCvvChange        = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                    onCvvVisivelChange = { cvvVisivel = it },
                    onBandeiraChange   = { selectedBandeira = it },
                    onExpandedChange   = { expandedBandeira = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick  = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled  = botaoHabilitado
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Confirmar assinatura", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "Ao confirmar, seu plano Premium será ativado imediatamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PixPaymentSection(
    pixKey: String,
    loading: Boolean,
    valor: String,
    copiado: Boolean,
    onCopiar: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payments, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Text("Pagamento via PIX",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text("Valor: $valor", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Chave PIX (empresa):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else {
                Surface(shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(pixKey, modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                OutlinedButton(
                    onClick  = onCopiar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = pixKey.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (copiado) "Chave copiada!" else "Copiar chave PIX")
                }
            }
            Text(
                "Após realizar o pagamento, clique em Confirmar assinatura.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPaymentSection(
    numeroCartao: TextFieldValue,
    nomeCartao: String,
    validade: TextFieldValue,
    cvv: String,
    cvvVisivel: Boolean,
    selectedBandeira: String,
    expandedBandeira: Boolean,
    luhnOk: Boolean,
    validadeOk: Boolean,
    cvvOk: Boolean,
    onNumeroCartaoChange: (TextFieldValue) -> Unit,
    onNomeCartaoChange: (String) -> Unit,
    onValidadeChange: (TextFieldValue) -> Unit,
    onCvvChange: (String) -> Unit,
    onCvvVisivelChange: (Boolean) -> Unit,
    onBandeiraChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit
) {
    val digitosCartao = numeroCartao.text.filter { it.isDigit() }
    val cartaoCompleto = digitosCartao.length == 16

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Bandeira
        ExposedDropdownMenuBox(
            expanded = expandedBandeira,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                value    = selectedBandeira,
                onValueChange = {},
                readOnly = true,
                label    = { Text("Bandeira") },
                leadingIcon  = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBandeira) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(expanded = expandedBandeira,
                onDismissRequest = { onExpandedChange(false) }) {
                BANDEIRAS.forEach { bandeira ->
                    DropdownMenuItem(
                        text = { Text(bandeira) },
                        onClick = { onBandeiraChange(bandeira); onExpandedChange(false) },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Número do cartão
        OutlinedTextField(
            value         = numeroCartao,
            onValueChange = onNumeroCartaoChange,
            label         = { Text("Número do cartão") },
            placeholder   = { Text("0000 0000 0000 0000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon  = { Icon(Icons.Default.CreditCard, contentDescription = null) },
            isError      = cartaoCompleto && !luhnOk,
            supportingText = if (cartaoCompleto && !luhnOk) {
                { Text("Número de cartão inválido", color = MaterialTheme.colorScheme.error) }
            } else null
        )

        // Nome no cartão
        OutlinedTextField(
            value         = nomeCartao,
            onValueChange = onNomeCartaoChange,
            label         = { Text("Nome no cartão") },
            modifier      = Modifier.fillMaxWidth(),
            leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        // Validade e CVV lado a lado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value         = validade,
                onValueChange = onValidadeChange,
                label         = { Text("Validade") },
                placeholder   = { Text("MM/AA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError  = validade.text.length == 5 && !validadeOk,
                supportingText = if (validade.text.length == 5 && !validadeOk) {
                    { Text("Cartão expirado", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            OutlinedTextField(
                value         = cvv,
                onValueChange = onCvvChange,
                label         = { Text("CVV") },
                placeholder   = { Text("•••") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (cvvVisivel) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onCvvVisivelChange(!cvvVisivel) }) {
                        Icon(
                            imageVector = if (cvvVisivel) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (cvvVisivel) "Ocultar CVV" else "Mostrar CVV"
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                isError  = cvv.length >= 3 && !cvvOk,
                supportingText = if (cvv.length >= 3 && !cvvOk) {
                    { Text("CVV inválido", color = MaterialTheme.colorScheme.error) }
                } else null
            )
        }

        // Aviso de segurança
        Surface(
            color  = MaterialTheme.colorScheme.surfaceVariant,
            shape  = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Seus dados são validados localmente. Apenas os últimos 4 dígitos são armazenados. CVV nunca é salvo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
