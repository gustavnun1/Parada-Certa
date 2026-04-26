package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CancelPremiumState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CancelPremiumViewModel : ViewModel() {

    private val _state = MutableStateFlow(CancelPremiumState())
    val state: StateFlow<CancelPremiumState> = _state.asStateFlow()

    fun cancelar(cpf: String) {
        viewModelScope.launch {
            try {
                _state.value = CancelPremiumState(isLoading = true)
                val response = ParadaCertaClient.service.cancelarPremium(cpf)
                if (response.isSuccessful) {
                    _state.value = CancelPremiumState(isSuccess = true)
                } else {
                    _state.value = CancelPremiumState(
                        errorMessage = "Erro ao cancelar plano (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _state.value = CancelPremiumState(
                    errorMessage = "Erro: ${e.message ?: "Tente novamente"}"
                )
            }
        }
    }

    fun resetState() {
        _state.value = CancelPremiumState()
    }
}
