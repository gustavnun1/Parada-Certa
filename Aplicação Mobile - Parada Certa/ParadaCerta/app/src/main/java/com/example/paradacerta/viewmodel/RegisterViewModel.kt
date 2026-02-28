package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.CadastroRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.network.ViaCepClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddressState(
    val logradouro: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val estado: String = "",
    val error: String? = null
)

data class RegisterState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class RegisterViewModel : ViewModel() {

    // Estado do CEP
    private val _addressState = MutableStateFlow(AddressState())
    val addressState: StateFlow<AddressState> = _addressState.asStateFlow()

    // Estado do cadastro
    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    // ----------------------------------------------------------------
    // Busca CEP via Retrofit (ViaCEP)
    // ----------------------------------------------------------------
    fun fetchCep(cep: String) {
        val cepLimpo = cep.filter { it.isDigit() }

        if (cepLimpo.length != 8) {
            _addressState.value = AddressState(error = "CEP deve conter 8 dígitos")
            return
        }

        viewModelScope.launch {
            try {
                val response = ViaCepClient.service.buscarCep(cepLimpo)

                when {
                    !response.isSuccessful ->
                        _addressState.value = AddressState(
                            error = "Serviço de CEP indisponível (${response.code()})"
                        )

                    response.body() == null ->
                        _addressState.value = AddressState(error = "Resposta inválida do serviço de CEP")

                    response.body()!!.erro == true ->
                        _addressState.value = AddressState(error = "CEP não encontrado")

                    else -> {
                        val body = response.body()!!
                        _addressState.value = AddressState(
                            logradouro = body.logradouro.orEmpty(),
                            bairro     = body.bairro.orEmpty(),
                            cidade     = body.cidade.orEmpty(),
                            estado     = body.estado.orEmpty()
                        )
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _addressState.value = AddressState(error = "Sem conexão com a internet")
            } catch (e: java.net.SocketTimeoutException) {
                _addressState.value = AddressState(error = "Tempo de resposta esgotado. Tente novamente")
            } catch (e: Exception) {
                _addressState.value = AddressState(error = "Erro ao buscar CEP: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------------
    // Cadastro via API Spring Boot (sem JDBC no Android)
    // ----------------------------------------------------------------
    fun registerUser(
        nome: String,
        email: String,
        senha: String,
        cpf: String,
        dataNascimento: String,
        placa: String,
        modeloVeiculo: String,
        corVeiculo: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        estado: String
    ) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState(isLoading = true)

                val request = CadastroRequest(
                    nome           = nome,
                    email          = email,
                    senha          = senha,
                    cpf            = cpf.filter { it.isDigit() },
                    dataNascimento = dataNascimento,
                    placa          = placa.uppercase(),
                    modeloVeiculo  = modeloVeiculo,
                    corVeiculo     = corVeiculo,
                    cep            = cep.filter { it.isDigit() },
                    logradouro     = logradouro,
                    numero         = numero,
                    complemento    = complemento,
                    bairro         = bairro,
                    cidade         = cidade,
                    estado         = estado.uppercase()
                )

                val response = ParadaCertaClient.service.cadastrar(request)
                val body = response.body()

                when {
                    response.isSuccessful && body?.sucesso == true ->
                        _registerState.value = RegisterState(isSuccess = true)

                    body != null ->
                        _registerState.value = RegisterState(
                            errorMessage = body.mensagem
                        )

                    else ->
                        _registerState.value = RegisterState(
                            errorMessage = "Erro ${response.code()}: não foi possível cadastrar"
                        )
                }

            } catch (e: java.net.UnknownHostException) {
                _registerState.value = RegisterState(
                    errorMessage = "Sem conexão com o servidor. Verifique a rede."
                )
            } catch (e: java.net.SocketTimeoutException) {
                _registerState.value = RegisterState(
                    errorMessage = "O servidor demorou para responder. Tente novamente."
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState(
                    errorMessage = "Erro inesperado: ${e.message}"
                )
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState()
    }
}