package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.FormaPagamentoRequest
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PremiumPaymentState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class PremiumPaymentViewModel : ViewModel() {

    private val _state = MutableStateFlow(PremiumPaymentState())
    val state: StateFlow<PremiumPaymentState> = _state.asStateFlow()

    fun confirmarAssinatura(cpf: String, salvarCartao: Boolean, cartaoRequest: FormaPagamentoRequest?) {
        viewModelScope.launch {
            try {
                _state.value = PremiumPaymentState(isLoading = true)

                // Ativa premium no backend
                val premiumResponse = ParadaCertaClient.service.ativarPremium(cpf)
                if (!premiumResponse.isSuccessful) {
                    _state.value = PremiumPaymentState(
                        errorMessage = "Erro ao ativar o plano Premium (${premiumResponse.code()})"
                    )
                    return@launch
                }

                // Salva cartão se solicitado
                if (salvarCartao && cartaoRequest != null) {
                    val saveResponse = ParadaCertaClient.service.salvarFormaPagamento(cartaoRequest)
                    if (!saveResponse.isSuccessful) {
                        // Não bloqueia o sucesso — premium já foi ativado
                        _state.value = PremiumPaymentState(isSuccess = true)
                        return@launch
                    }
                }

                _state.value = PremiumPaymentState(isSuccess = true)

            } catch (e: java.net.UnknownHostException) {
                _state.value = PremiumPaymentState(errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = PremiumPaymentState(errorMessage = "Tempo de resposta esgotado")
            } catch (e: java.net.ConnectException) {
                _state.value = PremiumPaymentState(errorMessage = "Não foi possível conectar ao servidor")
            } catch (e: Exception) {
                _state.value = PremiumPaymentState(
                    errorMessage = "Erro inesperado: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun resetState() {
        _state.value = PremiumPaymentState()
    }
}
