package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ApiResponse
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class DeleteViewModel : ViewModel() {

    private val _deleteState = MutableStateFlow(DeleteState())
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun deleteAccount(cpf: String) {
        viewModelScope.launch {
            try {
                _deleteState.value = DeleteState(isLoading = true)

                val response = ParadaCertaClient.service.deletarConta(cpf)
                val body = response.body()
                val mensagemErro = if (response.isSuccessful) body?.mensagem else lerErro(response)

                when {
                    response.isSuccessful && body?.sucesso == true -> {
                        _deleteState.value = DeleteState(isSuccess = true)
                    }

                    response.code() == 400 -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "CPF invalido ou dados incorretos: ${mensagemErro ?: "Verifique os dados informados"}"
                        )
                    }

                    response.code() == 404 -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Conta não encontrada. Você pode já ter sido removido do sistema ou o CPF está incorreto"
                        )
                    }

                    response.code() == 409 -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Conflito ao excluir conta: ${mensagemErro ?: "Existem dados relacionados que impedem a exclusao"}"
                        )
                    }

                    response.code() == 500 -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Erro interno no servidor ao excluir conta: ${mensagemErro ?: "Tente novamente mais tarde"}"
                        )
                    }

                    response.code() == 503 -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Servidor temporariamente indisponível. Aguarde alguns minutos e tente novamente"
                        )
                    }

                    body != null -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Falha ao excluir conta: ${body.mensagem}"
                        )
                    }

                    else -> {
                        _deleteState.value = DeleteState(
                            errorMessage = "Erro HTTP ${response.code()}: ${mensagemErro ?: "Falha desconhecida ao excluir conta"}"
                        )
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                _deleteState.value = DeleteState(
                    errorMessage = "Sem conexão com o servidor. Verifique se:\n• Você está conectado à internet\n• O servidor está acessível"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _deleteState.value = DeleteState(
                    errorMessage = "Tempo de resposta esgotado (timeout). O servidor demorou muito para responder. Tente novamente"
                )
            } catch (e: java.net.ConnectException) {
                _deleteState.value = DeleteState(
                    errorMessage = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando"
                )
            } catch (e: retrofit2.HttpException) {
                _deleteState.value = DeleteState(
                    errorMessage = "Erro HTTP ${e.code()}: ${e.message() ?: "Falha na comunicação com o servidor ao excluir conta"}"
                )
            } catch (e: com.google.gson.JsonSyntaxException) {
                _deleteState.value = DeleteState(
                    errorMessage = "Erro ao processar resposta do servidor. Resposta inválida ou corrompida"
                )
            } catch (e: Exception) {
                _deleteState.value = DeleteState(
                    errorMessage = "Erro inesperado ao excluir conta:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState()
    }

    private fun lerErro(response: retrofit2.Response<ApiResponse>): String? {
        response.body()?.mensagem?.let { return it }
        return try {
            val json = response.errorBody()?.string()
            if (!json.isNullOrBlank()) {
                com.google.gson.Gson().fromJson(json, ApiResponse::class.java)?.mensagem
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
