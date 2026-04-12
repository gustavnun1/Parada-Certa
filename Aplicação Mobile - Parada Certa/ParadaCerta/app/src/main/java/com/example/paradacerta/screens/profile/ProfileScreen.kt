package com.example.paradacerta.screens.profile

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
import androidx.compose.ui.graphics.Color
import com.example.paradacerta.ui.theme.BrancoFundo
import com.example.paradacerta.ui.theme.CinzaMedio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onExitClick: () -> Unit = {},
    onConfigClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onPaymentMethodsClick: () -> Unit = {},
    cliente: Cliente? = null,
    veiculo: Veiculo? = null,
    endereco: Endereco? = null,
    modifier: Modifier = Modifier
) {
    val isPremium = cliente?.premium ?: false
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

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
                            text = veiculo?.placa ?: "---",
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
                            text = veiculo?.nome ?: "---",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cor:",
                            color = CinzaMedio
                        )
                        Text(
                            text = veiculo?.cor ?: "---",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card do plano
            val premiumGold = Color(0xFFFFC107)
            Card(
                onClick = onPremiumClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.secondaryContainer
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
                            text = if (isPremium) "Plano Premium" else "Plano Gratuito",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isPremium) premiumGold else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isPremium) "Você é assinante Premium!" else "Upgrade para Premium",
                            fontSize = 13.sp,
                            color = if (isPremium) Color.White.copy(alpha = 0.8f) else BrancoFundo
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPremium) premiumGold else MaterialTheme.colorScheme.secondary,
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
                    onClick = onPremiumClick
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Configurações",
                    subtitle = "Preferências do app",
                    onClick = { onConfigClick() }
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
                        text = """
POLÍTICA DE PRIVACIDADE - PARADA CERTA

1. CONTROLADORA DE DADOS

O Parada Certa atua como Controladora dos dados coletados, em conformidade com a LGPD (Lei 13.709/2018).

2. DADOS COLETADOS DE MOTORISTAS

Poderão ser coletados:
• Dados cadastrais: nome, e-mail, CPF, telefone, data de nascimento
• Dados de veículo: placa, modelo, cor
• Dados de uso: histórico de estacionamentos, reservas, avaliações
• Dados de localização (quando autorizado)
• Dados de pagamento (quando aplicável)

3. FINALIDADES DO TRATAMENTO

Os dados serão usados para:
• Cadastro e autenticação de usuários
• Funcionamento das funcionalidades (mapa, vagas, reservas)
• Oferecer informações sobre vagas e preços
• Melhorar a experiência de uso
• Cumprir obrigações legais
• Exibir anúncios personalizados (quando aplicável)

4. COMPARTILHAMENTO DE DADOS

Os dados de motoristas poderão ser:
• Compartilhados com estacionamentos para execução de serviços
• Usados de forma anonimizada para estatísticas
• Compartilhados com intermediadores de pagamento
• Compartilhados com autoridades mediante ordem judicial

5. BASE LEGAL

O tratamento é realizado com base em:
• Consentimento do titular
• Cumprimento de obrigação legal
• Execução de contrato
• Legítimo interesse do controlador

6. ARMAZENAMENTO E SEGURANÇA

Os dados são armazenados em ambiente seguro com:
• Servidores em nuvem
• Criptografia
• Controle de acesso

7. DIREITOS DOS TITULARES (LGPD)

Você pode:
• Confirmar existência de tratamento
• Acessar seus dados pessoais
• Solicitar correção de dados
• Solicitar eliminação de dados
• Solicitar portabilidade
• Revogar consentimento

Para exercer seus direitos: atendimento@paradacerta.com

8. RETENÇÃO DOS DADOS

Os dados serão mantidos:
• Pelo período necessário ao uso da plataforma
• Pelo prazo exigido por lei
• Ou até pedido de exclusão

9. ALTERAÇÕES

Esta Política pode ser alterada a qualquer tempo. Você será notificado por e-mail.

Contato: atendimento@paradacerta.com
                        """.trimIndent(),
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
                        text = """
1. APRESENTAÇÃO

O presente documento tem por finalidade estabelecer os Termos de Uso do aplicativo Parada Certa, solução tecnológica voltada à conexão entre motoristas e estacionamentos privados.

Ao utilizar o Parada Certa, o usuário declara estar ciente, compreender e concordar integralmente com as disposições aqui previstas, em conformidade com a LGPD.

2. ACEITE DOS TERMOS

O aceite deste Termo ocorre quando o usuário:
• Realiza o cadastro no aplicativo
• Marca "Li e concordo com os Termos de Uso"
• Continua utilizando o sistema

3. CADASTRO E ELEGIBILIDADE

Para utilizar o aplicativo como motorista, é necessário:
• Ser maior de 18 anos
• Fornecer dados verdadeiros: nome, e-mail, CPF, placa do veículo

O usuário é responsável por manter a confidencialidade de sua senha.

4. DESCRIÇÃO DOS SERVIÇOS

O Parada Certa oferece:
• Visualização de estacionamentos próximos
• Consulta de vagas disponíveis em tempo real
• Visualização de valores, horários e avaliações
• Reserva antecipada de vagas
• Planos gratuitos e pagos com funcionalidades adicionais

5. RESPONSABILIDADES DO MOTORISTA

O motorista compromete-se a:
• Utilizar o aplicativo de forma lícita
• Fornecer apenas avaliações verdadeiras
• Respeitar as regras de cada estacionamento

6. LIMITAÇÕES DE RESPONSABILIDADE

O Parada Certa atua como intermediador, não sendo responsável por:
• Danos, furtos ou roubos em estacionamentos
• Divergências entre informações cadastradas
• Interrupções temporárias de acesso

7. ALTERAÇÕES

Este Termo pode ser alterado a qualquer tempo. O uso continuado após atualização será interpretado como aceite.

Para dúvidas: atendimento@paradacerta.com
                        """.trimIndent(),
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