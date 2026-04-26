package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParkingDetailsState(
    val isLoading: Boolean = false,
    val estacionamento: Estacionamento? = null,
    val errorMessage: String? = null
)

class ParkingDetailsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ParkingDetailsState())
    val state: StateFlow<ParkingDetailsState> = _state.asStateFlow()

    fun carregar(id: Int) {
        viewModelScope.launch {
            try {
                _state.value = ParkingDetailsState(isLoading = true)
                val response = ParadaCertaClient.service.buscarEstacionamentoPorId(id)
                when {
                    response.isSuccessful && response.body() != null -> {
                        _state.value = ParkingDetailsState(estacionamento = response.body())
                    }
                    response.code() == 404 -> {
                        _state.value = ParkingDetailsState(errorMessage = "Estacionamento não encontrado")
                    }
                    else -> {
                        _state.value = ParkingDetailsState(
                            errorMessage = "Erro ao carregar detalhes (${response.code()})"
                        )
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _state.value = ParkingDetailsState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = ParkingDetailsState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: java.net.ConnectException) {
                _state.value = ParkingDetailsState(errorMessage = "Não foi possível conectar ao servidor")
            } catch (e: Exception) {
                _state.value = ParkingDetailsState(
                    errorMessage = "Erro inesperado: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }
}
