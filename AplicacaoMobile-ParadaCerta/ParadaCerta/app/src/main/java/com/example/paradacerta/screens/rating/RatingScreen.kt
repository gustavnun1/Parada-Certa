package com.example.paradacerta.screens.rating

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.viewmodel.AvaliacaoViewModel

private val StarColor = Color(0xFFFFC107)   // âmbar — igual ao Uber
private val StarEmpty  = Color(0xFFE0E0E0)

private val rotulos = listOf("Terrível", "Ruim", "Regular", "Bom", "Excelente")

@Composable
fun RatingScreen(
    estacionamentoId: Int,
    nomeEstacionamento: String,
    cpf: String,
    onConcluido: () -> Unit,
    viewModel: AvaliacaoViewModel = viewModel()
) {
    // Bloqueia o botão físico de voltar — o usuário deve avaliar para sair
    BackHandler(enabled = true) { /* navegação bloqueada até avaliação */ }

    val state by viewModel.state.collectAsState()
    var nota by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    // Navega automaticamente após sucesso
    LaunchedEffect(state.sucesso) {
        if (state.sucesso) onConcluido()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Cabeçalho verde ──────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Pagamento concluído!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Text(
                        text = nomeEstacionamento,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Pergunta ─────────────────────────────────────────────────────
            Text(
                text = "Como foi o estacionamento?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sua avaliação ajuda outros motoristas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Estrelas ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    StarButton(
                        index = i,
                        notaSelecionada = nota,
                        onClick = { nota = i }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rótulo da nota selecionada
            val rotulo = if (nota > 0) rotulos[nota - 1] else ""
            Text(
                text = rotulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (nota > 0) StarColor else Color.Transparent,
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Campo de comentário ──────────────────────────────────────────
            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= 300) comentario = it },
                label = { Text("Comentário (opcional)") },
                placeholder = { Text("Conte mais sobre sua experiência...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text(
                        text = "${comentario.length}/300",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Botão enviar ─────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.enviar(
                        estacionamentoId = estacionamentoId,
                        cpf = cpf,
                        nota = nota,
                        comentario = comentario
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                enabled = nota >= 1 && !state.isLoading,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviando...")
                } else {
                    Text(
                        text = "Enviar avaliação",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Mensagem de erro
            state.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onConcluido,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Pular e ir para início",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Componente de estrela animada ────────────────────────────────────────────

@Composable
private fun StarButton(
    index: Int,
    notaSelecionada: Int,
    onClick: () -> Unit
) {
    val preenchida = index <= notaSelecionada
    val cor by animateColorAsState(
        targetValue = if (preenchida) StarColor else StarEmpty,
        animationSpec = spring(),
        label = "star_color_$index"
    )
    val escala by animateFloatAsState(
        targetValue = if (preenchida) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "star_scale_$index"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .scale(escala)
    ) {
        Icon(
            imageVector = if (preenchida) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = "$index estrela${if (index > 1) "s" else ""}",
            tint = cor,
            modifier = Modifier.size(44.dp)
        )
    }
}
