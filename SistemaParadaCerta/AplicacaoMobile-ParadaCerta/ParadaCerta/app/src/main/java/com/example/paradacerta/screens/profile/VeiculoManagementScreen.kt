package com.example.paradacerta.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.screens.common.VEHICLE_BRANDS
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.validation.PlacaValidator
import com.example.paradacerta.viewmodel.VeiculoViewModel
import kotlinx.coroutines.launch

private const val LIMITE_VEICULOS = 5
private val CORES_VEICULO = listOf(
    "Branco", "Preto", "Prata", "Cinza", "Vermelho",
    "Azul", "Verde", "Amarelo", "Marrom", "Bege"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeiculoManagementScreen(
    cpf: String,
    onBackClick: () -> Unit,
    onVeiculosAlterados: () -> Unit = {},
    veiculoViewModel: VeiculoViewModel = viewModel()
) {
    val veiculos by veiculoViewModel.veiculos.collectAsState()
    val operacaoState by veiculoViewModel.operacaoState.collectAsState()

    var showAdicionarDialog by remember { mutableStateOf(false) }
    var veiculoParaEditar by remember { mutableStateOf<Veiculo?>(null) }
    var veiculoParaRemover by remember { mutableStateOf<Veiculo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cpf) {
        veiculoViewModel.carregarVeiculos(cpf)
    }

    LaunchedEffect(operacaoState.isSuccess) {
        if (operacaoState.isSuccess) {
            onVeiculosAlterados()
            scope.launch { snackbarHostState.showSnackbar("Veículos atualizados com sucesso.") }
            veiculoViewModel.resetOperacaoState()
        }
    }

    LaunchedEffect(operacaoState.errorMessage) {
        operacaoState.errorMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            veiculoViewModel.resetOperacaoState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Veículos", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            if (veiculos.size < LIMITE_VEICULOS) {
                ExtendedFloatingActionButton(
                    text = { Text("Adicionar") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { showAdicionarDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                operacaoState.isLoading && veiculos.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                veiculos.isEmpty() -> EmptyState(
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { CountHeader(quantidade = veiculos.size) }
                        items(veiculos, key = { it.placa }) { veiculo ->
                            VeiculoCard(
                                veiculo = veiculo,
                                onEditar = { veiculoParaEditar = veiculo },
                                onRemover = { veiculoParaRemover = veiculo }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdicionarDialog) {
        VeiculoFormDialog(
            titulo = "Adicionar Veículo",
            placaInicial = "",
            modeloInicial = "",
            corInicial = CORES_VEICULO.first(),
            placaEditavel = true,
            isLoading = operacaoState.isLoading,
            onConfirmar = { placa, modelo, cor ->
                veiculoViewModel.adicionarVeiculo(cpf, placa, modelo, cor) {
                    showAdicionarDialog = false
                }
            },
            onDismiss = { showAdicionarDialog = false }
        )
    }

    veiculoParaEditar?.let { v ->
        VeiculoFormDialog(
            titulo = "Editar Veículo",
            placaInicial = v.placa,
            modeloInicial = v.nome,
            corInicial = if (v.cor.isBlank()) CORES_VEICULO.first() else v.cor,
            placaEditavel = false,
            isLoading = operacaoState.isLoading,
            onConfirmar = { _, modelo, cor ->
                veiculoViewModel.atualizarVeiculo(cpf, v.placa, modelo, cor) {
                    veiculoParaEditar = null
                }
            },
            onDismiss = { veiculoParaEditar = null }
        )
    }

    veiculoParaRemover?.let { v ->
        AlertDialog(
            onDismissRequest = { veiculoParaRemover = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remover veículo") },
            text = {
                Text("Deseja remover o veículo ${v.placa} — ${v.nome}? Essa ação não pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        veiculoViewModel.removerVeiculo(cpf, v.placa) {
                            veiculoParaRemover = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { veiculoParaRemover = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CountHeader(quantidade: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Veículos cadastrados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$quantidade/$LIMITE_VEICULOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = CinzaMedio
        )
        Text(
            text = "Nenhum veículo cadastrado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Toque em \"Adicionar\" para cadastrar seu primeiro veículo. Você pode ter até $LIMITE_VEICULOS veículos.",
            style = MaterialTheme.typography.bodyMedium,
            color = CinzaMedio
        )
    }
}

@Composable
private fun VeiculoCard(
    veiculo: Veiculo,
    onEditar: () -> Unit,
    onRemover: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = veiculo.placa,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = veiculo.nome,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = CinzaMedio,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = veiculo.cor.ifBlank { "Cor não informada" },
                    style = MaterialTheme.typography.bodySmall,
                    color = CinzaMedio
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onRemover,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remover")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeiculoFormDialog(
    titulo: String,
    placaInicial: String,
    modeloInicial: String,
    corInicial: String,
    placaEditavel: Boolean,
    isLoading: Boolean,
    onConfirmar: (placa: String, modelo: String, cor: String) -> Unit,
    onDismiss: () -> Unit
) {
    var placa by remember { mutableStateOf(placaInicial) }
    var modelo by remember {
        mutableStateOf(
            modeloInicial.takeIf { it.isNotBlank() && VEHICLE_BRANDS.contains(it) }.orEmpty()
        )
    }
    var cor by remember {
        mutableStateOf(corInicial.takeIf { CORES_VEICULO.contains(it) } ?: CORES_VEICULO.first())
    }

    var expandedModelo by remember { mutableStateOf(false) }
    var expandedCor by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val placaValida = PlacaValidator.isValida(placa)
    val modeloValido = modelo.isNotBlank()
    val formValido = placaValida && modeloValido && cor.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { input ->
                        placa = PlacaValidator.normalizar(input)
                    },
                    label = { Text("Placa") },
                    placeholder = { Text("ABC1D23") },
                    enabled = placaEditavel,
                    isError = placa.isNotBlank() && !placaValida,
                    supportingText = if (placa.isNotBlank() && !placaValida) {
                        { Text(PlacaValidator.MENSAGEM_FORMATO_INVALIDO) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedModelo,
                    onExpandedChange = { expandedModelo = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modelo do veículo") },
                        placeholder = { Text("Selecione a marca") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelo)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModelo,
                        onDismissRequest = { expandedModelo = false },
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        VEHICLE_BRANDS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    modelo = option
                                    expandedModelo = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedCor,
                    onExpandedChange = { expandedCor = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = cor,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cor") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCor)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCor,
                        onDismissRequest = { expandedCor = false }
                    ) {
                        CORES_VEICULO.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    cor = option
                                    expandedCor = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(placa, modelo, cor) },
                enabled = !isLoading && formValido
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") }
        }
    )
}
