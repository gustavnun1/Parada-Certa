package com.example.paradacerta.screens.profile

import com.example.paradacerta.legal.LegalDocuments
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.Veiculo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.example.paradacerta.notifications.NotificationPreferencesManager
import com.example.paradacerta.ui.theme.CinzaMedio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onExitClick: () -> Unit = {},
    onConfigClick: () -> Unit = {},
    onPaymentMethodsClick: () -> Unit = {},
    onVeiculosClick: () -> Unit = {},
    cliente: Cliente? = null,
    veiculos: List<Veiculo> = emptyList(),
    endereco: Endereco? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var reservasEnabled by remember { mutableStateOf(NotificationPreferencesManager.isReservasEnabled(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado visível no sistema */ }

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
                text = cliente?.nome ?: "Nome do Usuário",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = cliente?.email ?: "email@exemplo.com",
                style = MaterialTheme.typography.bodyMedium,
                color = CinzaMedio
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card de resumo de veículos
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meus Veículos",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${veiculos.size}/5",
                            fontSize = 13.sp,
                            color = CinzaMedio
                        )
                    }

                    HorizontalDivider()

                    if (veiculos.isEmpty()) {
                        Text(
                            text = "Nenhum veículo cadastrado.",
                            color = CinzaMedio,
                            fontSize = 14.sp
                        )
                    } else {
                        val veiculo = veiculos.first()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Placa:", color = CinzaMedio)
                            Text(text = veiculo.placa, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Modelo:", color = CinzaMedio)
                            Text(text = veiculo.nome, fontWeight = FontWeight.Medium)
                        }
                        if (veiculos.size > 1) {
                            Text(
                                text = "+ ${veiculos.size - 1} outros veículos",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Opções do menu
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Configurações",
                    subtitle = "Preferências do app",
                    onClick = { onConfigClick() }
                )

                ProfileMenuItem(
                    icon = Icons.Default.DirectionsCar,
                    title = "Meus Veículos",
                    subtitle = "Gerenciar veículos cadastrados",
                    onClick = onVeiculosClick
                )

                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificações",
                    subtitle = "Gerencie alertas",
                    onClick = { showNotificationDialog = true }
                )

                ProfileMenuItem(
                    icon = Icons.Default.AccountBox,
                    title = "Formas de Pagamento",
                    subtitle = "Cartões salvos",
                    onClick = onPaymentMethodsClick
                )

                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "Termos de Uso",
                    subtitle = "Leia nossos termos",
                    onClick = { showTermsDialog = true }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Política de Privacidade",
                    subtitle = "Como protegemos seus dados",
                    onClick = { showPrivacyDialog = true }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Warning,
                    title = "Ajuda e Suporte",
                    subtitle = "Precisa de ajuda?",
                    onClick = { showSupportDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão sair
            OutlinedButton(
                onClick = { onExitClick() },  // ← CORRIGIDO: adicionado {}
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

    // Dialog - Política de Privacidade
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Política de Privacidade",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = LegalDocuments.POLITICA_PRIVACIDADE,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Dialog - Termos de Uso
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(
                    text = "Termos de Uso",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = LegalDocuments.TERMOS_DE_USO,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Dialog - Notificações
    if (showNotificationDialog) {
        val notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            title = { Text("Notificações", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!notifEnabled) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Notificações bloqueadas nas configurações do sistema. Ative-as para receber alertas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Lembretes de reserva
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lembretes de reserva", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Avisos 1h, 30min, 10min e na hora da reserva",
                                style = MaterialTheme.typography.bodySmall,
                                color = CinzaMedio
                            )
                        }
                        Switch(
                            checked = reservasEnabled,
                            onCheckedChange = { checked ->
                                reservasEnabled = checked
                                NotificationPreferencesManager.setReservasEnabled(context, checked)
                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Dialog - Ajuda e Suporte
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = {
                Text(
                    text = "Ajuda e Suporte",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Entre em contato conosco:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // E-mail
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
                            Text(
                                text = "E-mail",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "atendimento@paradacerta.com",
                                fontSize = 13.sp,
                                color = CinzaMedio
                            )
                        }
                    }

                    HorizontalDivider()

                    // Telefone
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
                            Text(
                                text = "Telefone",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "(11) 3000-0000",
                                fontSize = 13.sp,
                                color = CinzaMedio
                            )
                        }
                    }

                    HorizontalDivider()

                    // WhatsApp
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
                            Text(
                                text = "WhatsApp",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "(11) 99999-9999",
                                fontSize = 13.sp,
                                color = CinzaMedio
                            )
                        }
                    }

                    HorizontalDivider()

                    // Horário de atendimento
                    Text(
                        text = "Horário de atendimento:\nSegunda a Sexta, das 8h às 18h",
                        fontSize = 13.sp,
                        color = CinzaMedio
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Fechar")
                }
            }
        )
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
