package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.LoginRequest
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel : ViewModel() {

    // Estado do login
    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()


    // ----------------------------------------------------------------
    // Login via API Spring Boot
    // ----------------------------------------------------------------
    fun loginUser(
        email: String,
        senha: String
    ) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState(isLoading = true)

                val request = LoginRequest(
                    email          = email,
                    senha          = senha
                )

                val response = ParadaCertaClient.service.login(request)
                val body = response.body()

                when {
                    response.isSuccessful && body?.sucesso == true ->
                        _loginState.value = LoginState(isSuccess = true)

                    body != null ->
                        _loginState.value = LoginState(
                            errorMessage = body.mensagem
                        )

                    else ->
                        _loginState.value = LoginState(
                            errorMessage = "Erro ${response.code()}: não foi possível fazer login"
                        )
                }

            } catch (e: java.net.UnknownHostException) {
                _loginState.value = LoginState(
                    errorMessage = "Sem conexão com o servidor. Verifique a rede."
                )
            } catch (e: java.net.SocketTimeoutException) {
                _loginState.value = LoginState(
                    errorMessage = "O servidor demorou para responder. Tente novamente."
                )
            } catch (e: Exception) {
                _loginState.value = LoginState(
                    errorMessage = "Erro inesperado: ${e.message}"
                )
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState()
    }
}