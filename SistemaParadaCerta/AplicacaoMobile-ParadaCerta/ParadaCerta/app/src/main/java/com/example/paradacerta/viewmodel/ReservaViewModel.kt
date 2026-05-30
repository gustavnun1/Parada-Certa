package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ReservaRequest
import com.example.paradacerta.models.ReservaResponse
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReservaState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val resposta: ReservaResponse? = null,
    val errorMessage: String? = null
)

data class CancelReservaState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ReservaViewModel : ViewModel() {

    private val _reservaState = MutableStateFlow(ReservaState())
    val reservaState: StateFlow<ReservaState> = _reservaState.asStateFlow()

    private val _cancelState = MutableStateFlow(CancelReservaState())
    val cancelState: StateFlow<CancelReservaState> = _cancelState.asStateFlow()

    fun reservar(cpf: String, estacionamentoId: Int, placa: String, inicioReservaPrevisto: String) {
        if (cpf.isBlank() || estacionamentoId <= 0 || placa.isBlank() || inicioReservaPrevisto.isBlank()) return
        viewModelScope.launch {
            _reservaState.value = ReservaState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.criarReserva(
                    ReservaRequest(
                        cpf = cpf,
                        estacionamentoId = estacionamentoId,
                        placa = placa,
                        inicioReservaPrevisto = inicioReservaPrevisto
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _reservaState.value = ReservaState(isSuccess = true, resposta = response.body())
                } else {
                    val mensagem = try {
                        val body = response.errorBody()?.string()
                        org.json.JSONObject(body ?: "").optString("mensagem", null)
                            ?: "Erro ao criar reserva (${response.code()})"
                    } catch (e: Exception) {
                        "Erro ao criar reserva (${response.code()})"
                    }
                    _reservaState.value = ReservaState(errorMessage = mensagem)
                }
            } catch (e: java.net.UnknownHostException) {
                _reservaState.value = ReservaState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _reservaState.value = ReservaState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _reservaState.value = ReservaState(errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    fun cancelarReserva(sessaoId: String) {
        if (sessaoId.isBlank()) return
        viewModelScope.launch {
            _cancelState.value = CancelReservaState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.cancelarReserva(sessaoId)
                if (response.isSuccessful) {
                    _cancelState.value = CancelReservaState(isSuccess = true)
                } else {
                    val mensagem = try {
                        val body = response.errorBody()?.string()
                        org.json.JSONObject(body ?: "").optString("mensagem", null)
                            ?: "Erro ao cancelar reserva (${response.code()})"
                    } catch (e: Exception) {
                        "Erro ao cancelar reserva (${response.code()})"
                    }
                    _cancelState.value = CancelReservaState(errorMessage = mensagem)
                }
            } catch (e: java.net.UnknownHostException) {
                _cancelState.value = CancelReservaState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _cancelState.value = CancelReservaState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _cancelState.value = CancelReservaState(errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    fun finalizarReserva(sessaoId: String) {
        if (sessaoId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                ParadaCertaClient.service.finalizarReserva(sessaoId)
            }
        }
    }

    fun resetReservaState() { _reservaState.value = ReservaState() }
    fun resetCancelState() { _cancelState.value = CancelReservaState() }
}
