package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.models.VeiculoAdicionarRequest
import com.example.paradacerta.models.VeiculoAtualizarRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.validation.PlacaValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VeiculoState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class VeiculoViewModel : ViewModel() {

    private val _veiculos = MutableStateFlow<List<Veiculo>>(emptyList())
    val veiculos: StateFlow<List<Veiculo>> = _veiculos.asStateFlow()

    private val _operacaoState = MutableStateFlow(VeiculoState())
    val operacaoState: StateFlow<VeiculoState> = _operacaoState.asStateFlow()

    fun carregarVeiculos(cpf: String) {
        viewModelScope.launch {
            _operacaoState.value = VeiculoState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.listarVeiculos(cpf)
                if (response.isSuccessful) {
                    _veiculos.value = response.body() ?: emptyList()
                    _operacaoState.value = VeiculoState()
                } else {
                    _operacaoState.value = VeiculoState(errorMessage = "Erro ao carregar veículos (${response.code()})")
                }
            } catch (e: Exception) {
                _operacaoState.value = VeiculoState(errorMessage = "Sem conexão com o servidor")
            }
        }
    }

    fun adicionarVeiculo(cpf: String, placa: String, modelo: String, cor: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _operacaoState.value = VeiculoState(isLoading = true)
            try {
                val placaNormalizada = PlacaValidator.normalizar(placa)
                if (!PlacaValidator.isValida(placaNormalizada)) {
                    _operacaoState.value = VeiculoState(errorMessage = PlacaValidator.MENSAGEM_FORMATO_INVALIDO)
                    return@launch
                }

                val response = ParadaCertaClient.service.adicionarVeiculo(
                    VeiculoAdicionarRequest(cpf = cpf, placa = placaNormalizada, modeloVeiculo = modelo, corVeiculo = cor)
                )
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    recarregarListaInterna(cpf)
                    _operacaoState.value = VeiculoState(isSuccess = true)
                    onSuccess()
                } else {
                    val msg = response.body()?.mensagem
                        ?: response.errorBody()?.string()?.let {
                            try { org.json.JSONObject(it).optString("mensagem", null) } catch (e: Exception) { null }
                        }
                        ?: "Erro ao adicionar veículo (${response.code()})"
                    _operacaoState.value = VeiculoState(errorMessage = msg)
                }
            } catch (e: Exception) {
                _operacaoState.value = VeiculoState(errorMessage = "Sem conexão com o servidor")
            }
        }
    }

    fun atualizarVeiculo(cpf: String, placa: String, modelo: String, cor: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _operacaoState.value = VeiculoState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.atualizarVeiculo(
                    placa,
                    VeiculoAtualizarRequest(cpf = cpf, modeloVeiculo = modelo, corVeiculo = cor)
                )
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    recarregarListaInterna(cpf)
                    _operacaoState.value = VeiculoState(isSuccess = true)
                    onSuccess()
                } else {
                    val msg = response.body()?.mensagem
                        ?: "Erro ao atualizar veículo (${response.code()})"
                    _operacaoState.value = VeiculoState(errorMessage = msg)
                }
            } catch (e: Exception) {
                _operacaoState.value = VeiculoState(errorMessage = "Sem conexão com o servidor")
            }
        }
    }

    fun removerVeiculo(cpf: String, placa: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _operacaoState.value = VeiculoState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.removerVeiculo(placa, cpf)
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    recarregarListaInterna(cpf)
                    _operacaoState.value = VeiculoState(isSuccess = true)
                    onSuccess()
                } else {
                    val msg = response.body()?.mensagem
                        ?: response.errorBody()?.string()?.let {
                            try { org.json.JSONObject(it).optString("mensagem", null) } catch (e: Exception) { null }
                        }
                        ?: "Erro ao remover veículo (${response.code()})"
                    _operacaoState.value = VeiculoState(errorMessage = msg)
                }
            } catch (e: Exception) {
                _operacaoState.value = VeiculoState(errorMessage = "Sem conexão com o servidor")
            }
        }
    }

    // Recarrega a lista inline sem mexer no operacaoState, para que isSuccess
    // seja a última emissão observada pelo LaunchedEffect da tela.
    private suspend fun recarregarListaInterna(cpf: String) {
        runCatching {
            val response = ParadaCertaClient.service.listarVeiculos(cpf)
            if (response.isSuccessful) {
                _veiculos.value = response.body() ?: emptyList()
            }
        }
    }

    fun resetOperacaoState() {
        _operacaoState.value = VeiculoState()
    }
}
