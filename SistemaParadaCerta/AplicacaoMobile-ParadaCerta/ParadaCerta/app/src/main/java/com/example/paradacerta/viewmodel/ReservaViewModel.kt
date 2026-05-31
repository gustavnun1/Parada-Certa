package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ConfirmacaoReservaResponse
import com.example.paradacerta.models.ConfirmarReservaRequest
import com.example.paradacerta.models.FinalizacaoUsoResponse
import com.example.paradacerta.models.FinalizarUsoRequest
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

data class ConfirmarReservaState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val resposta: ConfirmacaoReservaResponse? = null,
    val errorMessage: String? = null
)

data class FinalizarUsoState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val resposta: FinalizacaoUsoResponse? = null,
    val errorMessage: String? = null
)

data class CalcularFinalizacaoState(
    val isLoading: Boolean = false,
    val resposta: FinalizacaoUsoResponse? = null,
    val errorMessage: String? = null
)

class ReservaViewModel : ViewModel() {

    private val _reservaState = MutableStateFlow(ReservaState())
    val reservaState: StateFlow<ReservaState> = _reservaState.asStateFlow()

    private val _cancelState = MutableStateFlow(CancelReservaState())
    val cancelState: StateFlow<CancelReservaState> = _cancelState.asStateFlow()

    private val _confirmarState = MutableStateFlow(ConfirmarReservaState())
    val confirmarState: StateFlow<ConfirmarReservaState> = _confirmarState.asStateFlow()

    private val _finalizarUsoState = MutableStateFlow(FinalizarUsoState())
    val finalizarUsoState: StateFlow<FinalizarUsoState> = _finalizarUsoState.asStateFlow()

    private val _calcularFinalizacaoState = MutableStateFlow(CalcularFinalizacaoState())
    val calcularFinalizacaoState: StateFlow<CalcularFinalizacaoState> = _calcularFinalizacaoState.asStateFlow()

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
                    _reservaState.value = ReservaState(errorMessage = extrairMensagem(response, "Erro ao criar reserva"))
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
                    _cancelState.value = CancelReservaState(
                        errorMessage = extrairMensagem(response, "Erro ao cancelar reserva")
                    )
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

    /** Mantido por compat com builds antigos. Para o fluxo novo, use [finalizarUso]. */
    fun finalizarReserva(sessaoId: String) {
        if (sessaoId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                ParadaCertaClient.service.finalizarReserva(sessaoId)
            }
        }
    }

    /**
     * Confirma a reserva via QR Code escaneado no estacionamento.
     * On success, o backend marca dataHoraConfirmacao e muda status para EM_USO.
     * O caller deve então chamar [UserViewModel.restaurarSessaoAtiva] para refletir o novo estado.
     */
    fun confirmarReserva(qrCode: String, cpf: String, onSuccess: (ConfirmacaoReservaResponse) -> Unit = {}) {
        if (qrCode.isBlank() || cpf.isBlank()) return
        viewModelScope.launch {
            _confirmarState.value = ConfirmarReservaState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.confirmarReserva(
                    ConfirmarReservaRequest(qrCode = qrCode, cpf = cpf)
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _confirmarState.value = ConfirmarReservaState(isSuccess = true, resposta = body)
                    onSuccess(body)
                } else {
                    _confirmarState.value = ConfirmarReservaState(
                        errorMessage = extrairMensagem(response, "Erro ao confirmar reserva")
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _confirmarState.value = ConfirmarReservaState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _confirmarState.value = ConfirmarReservaState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _confirmarState.value = ConfirmarReservaState(errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    /**
     * Calcula o preview da finalização (sem efetivar). Usado para mostrar
     * o resumo de valor antes de confirmar.
     */
    fun calcularFinalizacao(sessaoId: String, cpf: String, onResult: (FinalizacaoUsoResponse) -> Unit) {
        if (sessaoId.isBlank() || cpf.isBlank()) {
            _calcularFinalizacaoState.value = CalcularFinalizacaoState(
                errorMessage = "Nao foi possivel identificar a reserva ou o usuario."
            )
            return
        }
        viewModelScope.launch {
            _calcularFinalizacaoState.value = CalcularFinalizacaoState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.calcularFinalizacaoUso(sessaoId, cpf)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _calcularFinalizacaoState.value = CalcularFinalizacaoState(resposta = body)
                    onResult(body)
                } else {
                    _calcularFinalizacaoState.value = CalcularFinalizacaoState(
                        errorMessage = extrairMensagem(response, "Erro ao calcular finalizacao da reserva")
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _calcularFinalizacaoState.value = CalcularFinalizacaoState(errorMessage = "Sem conexao com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _calcularFinalizacaoState.value = CalcularFinalizacaoState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _calcularFinalizacaoState.value = CalcularFinalizacaoState(errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    /**
     * Efetiva a finalização do uso. Quando há cobrança adicional, o caller
     * deve ter cobrado via Pix antes e passar o valor em `valorPagoAdicional`.
     */
    fun finalizarUso(
        sessaoId: String,
        cpf: String,
        valorPagoAdicional: Double? = null,
        onSuccess: (FinalizacaoUsoResponse) -> Unit = {}
    ) {
        if (sessaoId.isBlank() || cpf.isBlank()) return
        viewModelScope.launch {
            _finalizarUsoState.value = FinalizarUsoState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.finalizarUso(
                    sessaoId,
                    FinalizarUsoRequest(cpf = cpf, valorPagoAdicional = valorPagoAdicional)
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _finalizarUsoState.value = FinalizarUsoState(isSuccess = true, resposta = body)
                    onSuccess(body)
                } else {
                    _finalizarUsoState.value = FinalizarUsoState(
                        errorMessage = extrairMensagem(response, "Erro ao finalizar uso da vaga")
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _finalizarUsoState.value = FinalizarUsoState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _finalizarUsoState.value = FinalizarUsoState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _finalizarUsoState.value = FinalizarUsoState(errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    private fun <T> extrairMensagem(response: retrofit2.Response<T>, fallback: String): String {
        return try {
            val body = response.errorBody()?.string()
            val mensagem = org.json.JSONObject(body ?: "").optString("mensagem", "")
            mensagem.ifBlank { "$fallback (${response.code()})" }
        } catch (e: Exception) {
            "$fallback (${response.code()})"
        }
    }

    fun resetReservaState() { _reservaState.value = ReservaState() }
    fun resetCancelState() { _cancelState.value = CancelReservaState() }
    fun resetConfirmarState() { _confirmarState.value = ConfirmarReservaState() }
    fun resetCalcularFinalizacaoState() { _calcularFinalizacaoState.value = CalcularFinalizacaoState() }
    fun resetFinalizarUsoState() { _finalizarUsoState.value = FinalizarUsoState() }
}
