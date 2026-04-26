package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.AvaliacaoItem
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AvaliacaoListState(
    val isLoading: Boolean = false,
    val avaliacoes: List<AvaliacaoItem> = emptyList(),
    val errorMessage: String? = null
)

class AvaliacaoListViewModel : ViewModel() {

    private val _state = MutableStateFlow(AvaliacaoListState())
    val state: StateFlow<AvaliacaoListState> = _state.asStateFlow()

    fun carregar(estacionamentoId: Int) {
        viewModelScope.launch {
            _state.value = AvaliacaoListState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.listarAvaliacoes(estacionamentoId)
                if (response.isSuccessful) {
                    _state.value = AvaliacaoListState(avaliacoes = response.body() ?: emptyList())
                } else {
                    _state.value = AvaliacaoListState(errorMessage = "Erro ao carregar avaliações")
                }
            } catch (_: java.net.UnknownHostException) {
                _state.value = AvaliacaoListState(errorMessage = "Sem conexão com o servidor")
            } catch (e: Exception) {
                _state.value = AvaliacaoListState(errorMessage = "Erro: ${e.message}")
            }
        }
    }
}
