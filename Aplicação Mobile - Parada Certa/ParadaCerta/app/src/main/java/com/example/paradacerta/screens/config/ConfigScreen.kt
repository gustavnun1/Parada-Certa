package com.example.paradacerta.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.models.Endereco
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paradacerta.viewmodel.DeleteState
import com.example.paradacerta.viewmodel.SaveState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    cliente: Cliente,
    veiculo: Veiculo?,
    endereco: Endereco?,
    onSaveChanges: (
        nome: String,
        email: String,
        senha: String,
        dataNascimento: String,
        numeroCelular: String,
        modeloVeiculo: String,
        corVeiculo: String,
        placa: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        estado: String
    ) -> Unit,
    onDeleteAccount: () -> Unit,
    saveState: SaveState = SaveState(),
    deleteState: DeleteState = DeleteState()
) {
    // Estados dos campos editáveis
    var nome by remember { mutableStateOf(cliente.nome) }
    var email by remember { mutableStateOf(cliente.email) }
    var senha by remember { mutableStateOf("") } // Deixa vazio, só preenche se quiser mudar
    var numeroCelular by remember { mutableStateOf(cliente.numeroCelular) }

    // Converte Date para String no formato DD/MM/AAAA
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var dataNascimento by remember {
        mutableStateOf(
            cliente.dataNascimento?.let { dateFormat.format(it) } ?: ""
        )
    }

    var nomeVeiculo by remember { mutableStateOf(veiculo?.nome ?: "") }
    var corVeiculo by remember { mutableStateOf(veiculo?.cor ?: "") }
    var placa by remember { mutableStateOf(veiculo?.placa ?: cliente.placa) }

    var cep by remember { mutableStateOf(endereco?.cep ?: "") }
    var logradouro by remember { mutableStateOf(endereco?.logradouro ?: "") }
    var numero by remember { mutableStateOf(endereco?.numero ?: "") }
    var complemento by remember { mutableStateOf(endereco?.complemento ?: "") }
    var bairro by remember { mutableStateOf(endereco?.bairro ?: "") }
    var cidade by remember { mutableStateOf(endereco?.cidade ?: "") }
    var estado by remember { mutableStateOf(endereco?.estado ?: "") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Configurações", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text("Dados Pessoais", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Nova Senha (deixe vazio para não alterar)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = dataNascimento,
                    onValueChange = { dataNascimento = it },
                    label = { Text("Data de Nascimento") },
                    placeholder = { Text("DD/MM/AAAA") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = numeroCelular,
                    onValueChange = { numeroCelular = it },
                    label = { Text("Celular") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Veículo", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = nomeVeiculo,
                    onValueChange = { nomeVeiculo = it },
                    label = { Text("Modelo do Veículo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = corVeiculo,
                    onValueChange = { corVeiculo = it },
                    label = { Text("Cor do Veículo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Endereço", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = cep,
                    onValueChange = { cep = it },
                    label = { Text("CEP") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = logradouro,
                    onValueChange = { logradouro = it },
                    label = { Text("Logradouro") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it },
                    label = { Text("Número") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = complemento,
                    onValueChange = { complemento = it },
                    label = { Text("Complemento") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = bairro,
                    onValueChange = { bairro = it },
                    label = { Text("Bairro") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = cidade,
                    onValueChange = { cidade = it },
                    label = { Text("Cidade") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = estado,
                    onValueChange = { estado = it },
                    label = { Text("Estado (UF)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Botão Salvar
            item {
                Button(
                    onClick = {
                        onSaveChanges(
                            nome,
                            email,
                            senha,
                            dataNascimento,
                            numeroCelular,
                            nomeVeiculo,
                            corVeiculo,
                            placa,
                            cep,
                            logradouro,
                            numero,
                            complemento,
                            bairro,
                            cidade,
                            estado
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saveState.isLoading && !deleteState.isLoading
                ) {
                    if (saveState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Salvar Alterações")
                }
            }

            // Mensagem de erro do Save
            if (saveState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = saveState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Botão Deletar
            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !saveState.isLoading && !deleteState.isLoading
                ) {
                    if (deleteState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Apagar Conta")
                }
            }

            // Mensagem de erro do Delete
            if (deleteState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = deleteState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Dialog de confirmação de exclusão
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount()
                }) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja apagar sua conta? Essa ação não pode ser desfeita.") }
        )
    }
}