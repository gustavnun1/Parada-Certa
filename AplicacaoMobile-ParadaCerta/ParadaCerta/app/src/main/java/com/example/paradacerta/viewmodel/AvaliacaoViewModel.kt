package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ApiResponse
import com.example.paradacerta.models.AvaliacaoRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

data class AvaliacaoState(
    val isLoading: Boolean = false,
    val sucesso: Boolean = false,
    val errorMessage: String? = null
)

class AvaliacaoViewModel : ViewModel() {

    private val _state = MutableStateFlow(AvaliacaoState())
    val state: StateFlow<AvaliacaoState> = _state.asStateFlow()

    fun enviar(estacionamentoId: Int, cpf: String, nota: Int, comentario: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ParadaCertaClient.service.avaliar(
                    AvaliacaoRequest(
                        estacionamentoId = estacionamentoId,
                        clienteCpf = cpf,
                        nota = nota,
                        comentario = comentario.trim().ifBlank { null }
                    )
                )
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    _state.value = _state.value.copy(isLoading = false, sucesso = true)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = lerErro(response, "Erro ao enviar avaliação")
                    )
                }
            } catch (_: java.net.UnknownHostException) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Sem conexão com o servidor")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Erro: ${e.message}")
            }
        }
    }

    private fun lerErro(response: Response<ApiResponse>, fallback: String): String {
        response.body()?.mensagem?.let { return it }
        return try {
            val json = response.errorBody()?.string()
            if (!json.isNullOrBlank()) Gson().fromJson(json, ApiResponse::class.java)?.mensagem ?: fallback
            else fallback
        } catch (_: Exception) { fallback }
    }
}
