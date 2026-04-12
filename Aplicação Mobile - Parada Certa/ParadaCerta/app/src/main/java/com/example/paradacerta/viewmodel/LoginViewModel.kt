package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.LoginRequest
import com.example.paradacerta.models.UserCompleteData
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userData: UserCompleteData? = null
)

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    /**
     * Realiza login com e-mail ou CPF.
     * @param login  Valor digitado (e-mail ou CPF formatado)
     * @param senha  Senha do usuário
     * @param isCpf  true quando o usuário escolheu login por CPF
     */
    fun loginUser(login: String, senha: String, isCpf: Boolean = false) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState(isLoading = true)

                val request = if (isCpf) {
                    LoginRequest(cpf = login, senha = senha)
                } else {
                    LoginRequest(email = login, senha = senha)
                }

                val response = ParadaCertaClient.service.login(request)
                val body = response.body()

                when {
                    response.isSuccessful && body?.sucesso == true -> {
                        fetchUserCompleteData(login, isCpf)
                    }

                    response.code() == 401 || response.code() == 403 -> {
                        _loginState.value = LoginState(
                            errorMessage = if (isCpf)
                                "CPF ou senha incorretos. Verifique suas credenciais e tente novamente"
                            else
                                "E-mail ou senha incorretos. Verifique suas credenciais e tente novamente"
                        )
                    }

                    response.code() == 404 -> {
                        _loginState.value = LoginState(
                            errorMessage = if (isCpf)
                                "Usuário não encontrado. Verifique se o CPF está correto ou crie uma conta"
                            else
                                "Usuário não encontrado. Verifique se o e-mail está correto ou crie uma conta"
                        )
                    }

                    response.code() == 500 -> {
                        _loginState.value = LoginState(
                            errorMessage = "Erro interno no servidor ao processar login: ${body?.mensagem ?: "Tente novamente em alguns instantes"}"
                        )
                    }

                    body != null -> {
                        _loginState.value = LoginState(errorMessage = "Falha no login: ${body.mensagem}")
                    }

                    else -> {
                        _loginState.value = LoginState(
                            errorMessage = "Erro HTTP ${response.code()}: Falha desconhecida ao fazer login"
                        )
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _loginState.value = LoginState(
                    errorMessage = "Sem conexão com o servidor. Verifique sua internet e se o servidor está acessível"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _loginState.value = LoginState(
                    errorMessage = "Tempo de resposta esgotado. O servidor demorou muito para responder"
                )
            } catch (e: java.net.ConnectException) {
                _loginState.value = LoginState(
                    errorMessage = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando"
                )
            } catch (e: Exception) {
                _loginState.value = LoginState(
                    errorMessage = "Erro inesperado ao fazer login:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    private suspend fun fetchUserCompleteData(login: String, isCpf: Boolean) {
        try {
            val userResponse = if (isCpf) {
                ParadaCertaClient.service.getUserByCpf(login)
            } else {
                ParadaCertaClient.service.getUserByEmail(login)
            }

            when {
                userResponse.isSuccessful && userResponse.body() != null -> {
                    _loginState.value = LoginState(
                        isSuccess = true,
                        userData = userResponse.body()!!
                    )
                }

                userResponse.code() == 404 -> {
                    _loginState.value = LoginState(
                        errorMessage = "Dados do usuário não encontrados. Entre em contato com o suporte caso necessário"
                    )
                }

                userResponse.code() == 500 -> {
                    _loginState.value = LoginState(
                        errorMessage = "Erro ao buscar dados do usuário no servidor. Tente fazer login novamente"
                    )
                }

                else -> {
                    _loginState.value = LoginState(
                        errorMessage = "Erro HTTP ${userResponse.code()}: Falha ao carregar dados do usuário"
                    )
                }
            }
        } catch (e: java.net.UnknownHostException) {
            _loginState.value = LoginState(
                errorMessage = "Sem conexão ao buscar dados do usuário. Verifique sua internet"
            )
        } catch (e: java.net.SocketTimeoutException) {
            _loginState.value = LoginState(
                errorMessage = "Tempo esgotado ao buscar dados do usuário. Tente novamente"
            )
        } catch (e: Exception) {
            _loginState.value = LoginState(
                errorMessage = "Erro inesperado ao buscar dados:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
            )
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState()
    }
}
