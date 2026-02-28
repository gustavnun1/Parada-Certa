package com.example.paradacerta.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paradacerta.ui.theme.BrancoFundo
import com.example.paradacerta.ui.theme.CinzaMedio

/**
 * Tela de perfil do motorista
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil",
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar e informações principais
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "João Silva",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "joao.silva@email.com",
                style = MaterialTheme.typography.bodyMedium,
                color = CinzaMedio
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card de informações do veículo
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Informações do Veículo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Placa:",
                            color = CinzaMedio
                        )
                        Text(
                            text = "ABC-1234",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Modelo:",
                            color = CinzaMedio
                        )
                        Text(
                            text = "Honda Civic 2020",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card do plano
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Plano Gratuito",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Upgrade para Premium",
                            fontSize = 13.sp,
                            color = BrancoFundo
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Opções do menu
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.Star,
                    title = "Plano Premium",
                    subtitle = "Vantagens exclusivas",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Configurações",
                    subtitle = "Preferências do app",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificações",
                    subtitle = "Gerencie alertas",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.AccountBox,
                    title = "Formas de Pagamento",
                    subtitle = "Cartões salvos",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "Termos de Uso",
                    subtitle = "Leia nossos termos",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Política de Privacidade",
                    subtitle = "Como protegemos seus dados",
                    onClick = { /* Ação futura */ }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Warning,
                    title = "Ajuda e Suporte",
                    subtitle = "Precisa de ajuda?",
                    onClick = { /* Ação futura */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão sair
            OutlinedButton(
                onClick = {
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da conta")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Versão 1.0.0",
                fontSize = 12.sp,
                color = CinzaMedio
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = CinzaMedio
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = CinzaMedio
            )
        }
    }
}