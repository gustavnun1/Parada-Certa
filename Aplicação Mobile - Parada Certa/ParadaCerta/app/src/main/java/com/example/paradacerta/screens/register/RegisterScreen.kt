package com.example.paradacerta.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.viewmodel.RegisterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onFinishRegister: () -> Unit = {},
    viewModel: RegisterViewModel = viewModel()
) {
    var step by remember { mutableStateOf(1) }

    // Dados Pessoais
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var dtnascimento by remember { mutableStateOf("") }

    // Dados do Veículo
    var plate by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    val vehicleTypes = listOf("Carro", "Moto", "Caminhão", "Van")
    val colors = listOf(
        "Branco", "Preto", "Prata", "Cinza", "Vermelho",
        "Azul", "Verde", "Amarelo", "Marrom", "Bege"
    )

    var expandedVehicleType by remember { mutableStateOf(false) }
    var expandedVehicleColor by remember { mutableStateOf(false) }
    var selectedVehicleType by remember { mutableStateOf(vehicleTypes.first()) }
    var selectedVehicleColor by remember { mutableStateOf(colors.first()) }

    // Endereço
    val addressState by viewModel.addressState.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    var cep by remember { mutableStateOf("") }
    var logradouro by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }

    var isLoadingCep by remember { mutableStateOf(false) }
    var cepError by remember { mutableStateOf<String?>(null) }

    // Termos de Uso e Política de Privacidade
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Atualiza os campos de endereço quando o CEP é buscado
    LaunchedEffect(addressState) {
        isLoadingCep = false
        cepError = addressState.error

        if (addressState.error == null) {
            logradouro = addressState.logradouro
            bairro = addressState.bairro
            cidade = addressState.cidade
            estado = addressState.estado
        }
    }

    // Observa o estado de registro
    LaunchedEffect(registerState) {
        if (registerState.isSuccess) {
            // Cadastro realizado com sucesso
            onFinishRegister()
            viewModel.resetRegisterState()
        }
    }

    // Dialog de loading durante cadastro
    if (registerState.isLoading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Cadastrando...") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { }
        )
    }

    // Dialog de erro
    registerState.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.resetRegisterState() },
            title = { Text("Erro no Cadastro") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetRegisterState() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro - Etapa $step de 4") },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = {
                            step--
                            showError = false
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // STEP 1 - Dados Pessoais
                if (step == 1) {
                    Text(
                        text = "Dados Pessoais",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome completo") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha (mín. 6 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Repetir Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            "As senhas não coincidem",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = cpf,
                        onValueChange = { cpf = it },
                            //onValueChange = {
                            // Formata CPF: 000.000.000-00
                            //val digits = it.filter { char -> char.isDigit() }.take(11)
                                 
                            //cpf = when {
                            //    digits.length <= 3 -> digits
                            //    digits.length <= 6 -> "${digits.substring(0, 3)}.${digits.substring(3)}"
                            //    digits.length <= 9 -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6)}"
                            //    else -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
                            //}
                            //},
                        label = { Text("CPF") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dtnascimento,
                        onValueChange = { dtnascimento = it },
                        //onValueChange = {
                        //    // Formata data: DD/MM/AAAA
                        //    val digits = it.filter { char -> char.isDigit() }.take(8)
                        //    dtnascimento = when {
                        //        digits.length <= 2 -> digits
                        //        digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
                        //        else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
                        //    }
                        //},
                        label = { Text("Data de Nascimento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // STEP 2 - Dados do Veículo
                if (step == 2) {
                    Text(
                        text = "Dados do Veículo",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = plate,
                        onValueChange = {
                            plate = it.uppercase().take(7)
                        },
                        label = { Text("Placa") },
                        placeholder = { Text("ABC1234") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleType,
                        onExpandedChange = { expandedVehicleType = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Veículo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleType)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedVehicleType,
                            onDismissRequest = { expandedVehicleType = false }
                        ) {
                            vehicleTypes.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedVehicleType = selectionOption
                                        expandedVehicleType = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo do veículo") },
                        placeholder = { Text("Ex: Civic, PCX, Hilux") },
                        leadingIcon = {
                            Icon(Icons.Default.Build, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleColor,
                        onExpandedChange = { expandedVehicleColor = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleColor,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cor do Veículo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleColor)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedVehicleColor,
                            onDismissRequest = { expandedVehicleColor = false }
                        ) {
                            colors.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        selectedVehicleColor = selectionOption
                                        expandedVehicleColor = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                // STEP 3 - Endereço
                if (step == 3) {
                    Text(
                        text = "Endereço",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = cep,
                        onValueChange = {
                            cep = it.filter { char -> char.isDigit() }.take(8)

                            if (cep.length == 8) {
                                isLoadingCep = true
                                cepError = null
                                viewModel.fetchCep(cep)
                            }
                        },
                        label = { Text("CEP") },
                        placeholder = { Text("00000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = cepError != null,
                        trailingIcon = {
                            if (isLoadingCep) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )

                    cepError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = logradouro,
                        onValueChange = { logradouro = it },
                        label = { Text("Logradouro") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it },
                        label = { Text("Número") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = complemento,
                        onValueChange = { complemento = it },
                        label = { Text("Complemento (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bairro,
                        onValueChange = { bairro = it },
                        label = { Text("Bairro") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cidade,
                        onValueChange = { cidade = it },
                        label = { Text("Cidade") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = estado,
                        onValueChange = { estado = it.uppercase().take(2) },
                        label = { Text("Estado (UF)") },
                        placeholder = { Text("SP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // STEP 4 - Termos e Política
                if (step == 4) {
                    Text(
                        text = "Termos e Política",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Para finalizar seu cadastro, é necessário aceitar nossos Termos de Uso e Política de Privacidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Checkbox Termos de Uso com link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = {
                                acceptedTerms = it
                                if (showError) showError = false
                            }
                        )
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Li e aceito os ")
                            TextButton(
                                onClick = { showTermsDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.offset(y = (-8).dp)
                            ) {
                                Text(
                                    text = "Termos de Uso",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Checkbox Política de Privacidade com link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedPrivacy,
                            onCheckedChange = {
                                acceptedPrivacy = it
                                if (showError) showError = false
                            }
                        )
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Li e aceito a ")
                            TextButton(
                                onClick = { showPrivacyDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.offset(y = (-8).dp)
                            ) {
                                Text(
                                    text = "Política de Privacidade",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Mensagem de erro
                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "⚠️ Você precisa aceitar os Termos de Uso e a Política de Privacidade para continuar",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de navegação
            Button(
                onClick = {
                    when (step) {
                        1 -> {
                            // Validação básica step 1
                            if (name.isBlank() || email.isBlank() ||
                                password.length < 6 || password != confirmPassword ||
                                cpf.replace("[^0-9]".toRegex(), "").length != 11 ||
                                !dtnascimento.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        2 -> {
                            // Validação básica step 2
                            if (plate.isBlank() || model.isBlank()) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        3 -> {
                            // Validação básica step 3
                            if (cep.length != 8 || logradouro.isBlank() ||
                                numero.isBlank() || bairro.isBlank() ||
                                cidade.isBlank() || estado.length != 2) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        4 -> {
                            // Validação e envio ao banco
                            if (acceptedTerms && acceptedPrivacy) {
                                viewModel.registerUser(
                                    nome = name,
                                    email = email,
                                    senha = password,
                                    cpf = cpf,
                                    dataNascimento = dtnascimento,
                                    placa = plate,
                                    modeloVeiculo = model,
                                    corVeiculo = selectedVehicleColor,
                                    cep = cep,
                                    logradouro = logradouro,
                                    numero = numero,
                                    complemento = complemento,
                                    bairro = bairro,
                                    cidade = cidade,
                                    estado = estado
                                )
                            } else {
                                showError = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !registerState.isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (step < 4) "Próximo" else "Finalizar Cadastro")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }

    // Dialogs dos Termos e Política (mantidos do código original)
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Termos de Uso", fontWeight = FontWeight.Bold) },
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

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Política de Privacidade", fontWeight = FontWeight.Bold) },
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
}