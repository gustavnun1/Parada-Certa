package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.EstacionamentoFotoItem
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstacionamentoFotosState(
    val isLoading: Boolean = false,
    val fotos: List<EstacionamentoFotoItem> = emptyList(),
    val errorMessage: String? = null,
    val carregadoParaId: Int? = null
)

class EstacionamentoFotosViewModel : ViewModel() {

    private val _state = MutableStateFlow(EstacionamentoFotosState())
    val state: StateFlow<EstacionamentoFotosState> = _state.asStateFlow()

    fun carregar(estacionamentoId: Int) {
        if (_state.value.carregadoParaId == estacionamentoId && _state.value.errorMessage == null) return

        viewModelScope.launch {
            _state.value = EstacionamentoFotosState(isLoading = true)
            try {
                val response = ParadaCertaClient.service.listarFotosEstacionamento(estacionamentoId)
                if (response.isSuccessful) {
                    val itens = (response.body() ?: emptyList())
                        .sortedWith(compareBy({ !it.principal }, { it.ordem }, { it.id }))
                    _state.value = EstacionamentoFotosState(
                        fotos = itens,
                        carregadoParaId = estacionamentoId
                    )
                } else {
                    _state.value = EstacionamentoFotosState(
                        errorMessage = "Não foi possível carregar as fotos no momento.",
                        carregadoParaId = estacionamentoId
                    )
                }
            } catch (_: java.net.UnknownHostException) {
                _state.value = EstacionamentoFotosState(
                    errorMessage = "Não foi possível carregar as fotos no momento.",
                    carregadoParaId = estacionamentoId
                )
            } catch (_: Exception) {
                _state.value = EstacionamentoFotosState(
                    errorMessage = "Não foi possível carregar as fotos no momento.",
                    carregadoParaId = estacionamentoId
                )
            }
        }
    }
}
