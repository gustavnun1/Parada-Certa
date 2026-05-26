package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ApiResponse
import com.example.paradacerta.models.ConfirmarRecuperacaoRequest
import com.example.paradacerta.models.SolicitarRecuperacaoRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

enum class RecuperacaoEtapa { SOLICITAR, AGUARDANDO_CODIGO, SUCESSO }

data class ForgotPasswordState(
    val etapa: RecuperacaoEtapa = RecuperacaoEtapa.SOLICITAR,
    val isLoading: Boolean = false,
    val mensagemSucesso: String? = null, // ex: "Código enviado para g***@gmail.com"
    val errorMessage: String? = null,
    // guarda login e isCpf para reutilizar no passo 2
    val loginSalvo: String = "",
    val isCpfSalvo: Boolean = false
)

class ForgotPasswordViewModel : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    // ── Passo 1: solicita código por e-mail ──────────────────────────────────

    fun solicitarCodigo(login: String, isCpf: Boolean) {
        if (login.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ParadaCertaClient.service.solicitarCodigoRecuperacao(
                    SolicitarRecuperacaoRequest(login = login, cpf = isCpf)
                )
                val body = response.body()
                if (response.isSuccessful && body?.sucesso == true) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        etapa = RecuperacaoEtapa.AGUARDANDO_CODIGO,
                        mensagemSucesso = body.mensagem,
                        loginSalvo = login,
                        isCpfSalvo = isCpf
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = lerErro(response, "Usuário não encontrado")
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    // ── Passo 2: confirma código e redefine senha ─────────────────────────────

    fun confirmarCodigo(codigo: String, novaSenha: String) {
        if (codigo.isBlank() || novaSenha.isBlank()) return
        val login = _state.value.loginSalvo
        val isCpf = _state.value.isCpfSalvo
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ParadaCertaClient.service.confirmarCodigoRecuperacao(
                    ConfirmarRecuperacaoRequest(
                        login = login,
                        cpf = isCpf,
                        codigo = codigo,
                        novaSenha = novaSenha
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body?.sucesso == true) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        etapa = RecuperacaoEtapa.SUCESSO,
                        mensagemSucesso = body.mensagem
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = lerErro(response, "Código inválido ou expirado")
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Sem conexão com o servidor")
            } catch (e: java.net.SocketTimeoutException) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Tempo de resposta esgotado")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, errorMessage = "Erro: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    fun voltarParaSolicitar() {
        _state.value = ForgotPasswordState()
    }

    // Lê a mensagem de erro de respostas não-2xx (errorBody) ou do corpo normal
    private fun lerErro(response: Response<ApiResponse>, fallback: String): String {
        // body() é null para respostas não-2xx no Retrofit; usar errorBody()
        response.body()?.mensagem?.let { return it }
        return try {
            val json = response.errorBody()?.string()
            if (!json.isNullOrBlank()) Gson().fromJson(json, ApiResponse::class.java)?.mensagem ?: fallback
            else fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
