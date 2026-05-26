package com.example.paradacerta.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.FormaPagamento
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.viewmodel.PaymentMethodsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    cpf: String,
    onBackClick: () -> Unit,
    viewModel: PaymentMethodsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var deleteTarget by remember { mutableStateOf<FormaPagamento?>(null) }

    LaunchedEffect(cpf) {
        viewModel.carregar(cpf)
    }

    // Diálogo de confirmação de exclusão
    deleteTarget?.let { card ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Excluir cartão?") },
            text = {
                val final4 = card.numeroCartao?.takeLast(4) ?: ""
                Text(
                    "Tem certeza que deseja remover o cartão " +
                    "${card.bandeira ?: ""} terminado em $final4?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletar(card.id, cpf)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de erro
    state.errorMessage?.let { erro ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Erro") },
            text = { Text(erro) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formas de Pagamento", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.cards.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCardOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CinzaMedio
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma forma de pagamento salva",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = CinzaMedio
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Seus cartões de crédito salvos aparecerão aqui.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = CinzaMedio
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.cards) { card ->
                            PaymentMethodCard(
                                card = card,
                                onDelete = { deleteTarget = card }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    card: FormaPagamento,
    onDelete: () -> Unit
) {
    val isPix = card.tipoPagamento == "PIX"
    val final4 = card.numeroCartao?.takeLast(4) ?: ""
    val maskedNumber = if (!isPix && card.numeroCartao != null)
        "**** **** **** $final4" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isPix) Icons.Default.Payments else Icons.Default.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                if (isPix) {
                    Text("PIX", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "${card.bandeira ?: "Cartão"} $maskedNumber",
                        fontWeight = FontWeight.Bold
                    )
                    if (!card.nomeCartao.isNullOrBlank()) {
                        Text(
                            text = card.nomeCartao,
                            style = MaterialTheme.typography.bodySmall,
                            color = CinzaMedio
                        )
                    }
                    if (!card.validade.isNullOrBlank()) {
                        Text(
                            text = "Validade: ${card.validade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CinzaMedio
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
