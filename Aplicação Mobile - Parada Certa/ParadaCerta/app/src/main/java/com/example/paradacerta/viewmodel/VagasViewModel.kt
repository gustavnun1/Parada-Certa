package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.launch

class VagasViewModel : ViewModel() {

    fun decrementar(estacionamentoId: Int) {
        if (estacionamentoId <= 0) return
        viewModelScope.launch {
            runCatching { ParadaCertaClient.service.decrementarVaga(estacionamentoId) }
        }
    }

    fun incrementar(estacionamentoId: Int) {
        if (estacionamentoId <= 0) return
        viewModelScope.launch {
            runCatching { ParadaCertaClient.service.incrementarVaga(estacionamentoId) }
        }
    }
}
