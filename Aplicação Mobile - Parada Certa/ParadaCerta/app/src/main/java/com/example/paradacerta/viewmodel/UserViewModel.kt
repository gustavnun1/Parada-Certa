package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.EntradaAppRequest
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _userData = MutableStateFlow<Cliente?>(null)
    val userData: StateFlow<Cliente?> = _userData.asStateFlow()

    private val _veiculoData = MutableStateFlow<Veiculo?>(null)
    val veiculoData: StateFlow<Veiculo?> = _veiculoData.asStateFlow()

    private val _enderecoData = MutableStateFlow<Endereco?>(null)
    val enderecoData: StateFlow<Endereco?> = _enderecoData.asStateFlow()

    private val _sessaoAtiva = MutableStateFlow<SessaoAtiva?>(null)
    val sessaoAtiva: StateFlow<SessaoAtiva?> = _sessaoAtiva.asStateFlow()

    fun setUser(cliente: Cliente, veiculo: Veiculo?, endereco: Endereco?) {
        _userData.value = cliente
        _veiculoData.value = veiculo
        _enderecoData.value = endereco
    }

    fun iniciarSessao(sessao: SessaoAtiva) {
        _sessaoAtiva.value = sessao
    }

    /**
     * Registra a entrada no backend e atualiza sessaoAtiva com o sessaoId real.
     * Deve ser chamado sempre que o usuário inicia uma sessão (demo ou QR de entrada).
     */
    fun iniciarSessaoComBackend(sessao: SessaoAtiva) {
        _sessaoAtiva.value = sessao           // exibe imediatamente na UI
        val cpf = _userData.value?.cpf ?: return
        viewModelScope.launch {
            runCatching {
                val response = ParadaCertaClient.service.registrarEntradaApp(
                    EntradaAppRequest(cpfUsuario = cpf, estacionamentoId = sessao.estacionamentoId)
                )
                if (response.isSuccessful) {
                    response.body()?.let { r ->
                        // Atualiza com o sessaoId real retornado pelo banco
                        _sessaoAtiva.value = sessao.copy(sessaoId = r.sessaoId)
                    }
                }
            }
        }
    }

    fun encerrarSessao() {
        _sessaoAtiva.value = null
    }

    fun setPremium(value: Boolean) {
        _userData.value = _userData.value?.copy(premium = value)
    }

    fun restaurarSessaoAtiva(cpf: String) {
        viewModelScope.launch {
            runCatching {
                val response = ParadaCertaClient.service.buscarSessaoAtiva(cpf)
                // 200 = sessão ativa encontrada, 204 = sem sessão ativa
                if (response.isSuccessful) {
                    response.body()?.let { r ->
                        _sessaoAtiva.value = SessaoAtiva(
                            estacionamentoId = r.estacionamentoId,
                            estacionamentoNome = r.estacionamentoNome,
                            modeloVeiculo = r.modeloVeiculo ?: "",
                            placa = r.placa ?: "",
                            precoHora = r.precoHora,
                            horaEntrada = r.horaEntrada,
                            sessaoId = r.sessaoId,
                            pixKey = r.pixKey ?: ""
                        )
                    }
                }
            }
        }
    }

    fun clearUser() {
        _userData.value = null
        _veiculoData.value = null
        _enderecoData.value = null
        _sessaoAtiva.value = null
    }
}
