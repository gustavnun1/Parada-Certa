package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.FormaPagamento
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentMethodsState(
    val isLoading: Boolean = false,
    val cards: List<FormaPagamento> = emptyList(),
    val errorMessage: String? = null,
    val deleteSuccess: Boolean = false
)

class PaymentMethodsViewModel : ViewModel() {

    private val _state = MutableStateFlow(PaymentMethodsState())
    val state: StateFlow<PaymentMethodsState> = _state.asStateFlow()

    fun carregar(cpf: String) {
        viewModelScope.launch {
            try {
                _state.value = PaymentMethodsState(isLoading = true)
                val response = ParadaCertaClient.service.listarFormasPagamento(cpf)
                when {
                    response.isSuccessful -> {
                        _state.value = PaymentMethodsState(cards = response.body() ?: emptyList())
                    }
                    response.code() == 404 -> {
                        _state.value = PaymentMethodsState(cards = emptyList())
                    }
                    else -> {
                        _state.value = PaymentMethodsState(
                            errorMessage = "Erro ao carregar formas de pagamento (${response.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = PaymentMethodsState(
                    errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun deletar(id: Int, cpf: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val response = ParadaCertaClient.service.deletarFormaPagamento(id, cpf)
                if (response.isSuccessful) {
                    carregar(cpf)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Erro ao excluir (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
