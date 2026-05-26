package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.CobrancaEstadia
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CobrancaEstadiaState(
    val isLoading: Boolean = false,
    val cobranca: CobrancaEstadia? = null,
    val errorMessage: String? = null
)

class CobrancaEstadiaViewModel : ViewModel() {

    private val _state = MutableStateFlow(CobrancaEstadiaState())
    val state: StateFlow<CobrancaEstadiaState> = _state.asStateFlow()

    fun calcular(sessaoId: String, onSuccess: (CobrancaEstadia) -> Unit = {}) {
        if (sessaoId.isBlank()) return
        viewModelScope.launch {
            _state.value = CobrancaEstadiaState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.calcularCobrancaEstadia(sessaoId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _state.value = CobrancaEstadiaState(cobranca = body)
                    onSuccess(body)
                } else {
                    _state.value = CobrancaEstadiaState(
                        errorMessage = "Não foi possível calcular o valor da estadia"
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _state.value = CobrancaEstadiaState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = CobrancaEstadiaState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _state.value = CobrancaEstadiaState(
                    errorMessage = "Erro ao calcular o valor da estadia"
                )
            }
        }
    }

    fun limpar() {
        _state.value = CobrancaEstadiaState()
    }
}
