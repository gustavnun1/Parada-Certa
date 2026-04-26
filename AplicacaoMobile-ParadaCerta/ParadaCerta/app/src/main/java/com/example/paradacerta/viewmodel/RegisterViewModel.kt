package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ClientRequest
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

    private val _addressState = MutableStateFlow(AddressState())
    val addressState: StateFlow<AddressState> = _addressState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun fetchCep(cep: String) {
        val cepLimpo = cep.filter { it.isDigit() }

        if (cepLimpo.length != 8) {
            _addressState.value = AddressState(
                error = "CEP inválido: deve conter exatamente 8 dígitos (você digitou ${cepLimpo.length})"
            )
            return
        }

        viewModelScope.launch {
            try {
                val response = ViaCepClient.service.buscarCep(cepLimpo)

                when {
                    !response.isSuccessful -> {
                        val errorDetail = when (response.code()) {
                            400 -> "CEP inválido ou mal formatado"
                            404 -> "CEP não encontrado na base de dados"
                            500 -> "Erro no servidor do ViaCEP. Tente novamente em alguns instantes"
                            503 -> "Serviço ViaCEP temporariamente indisponível"
                            else -> "Erro ${response.code()}: Falha ao buscar CEP"
                        }
                        _addressState.value = AddressState(error = errorDetail)
                    }

                    response.body() == null ->
                        _addressState.value = AddressState(
                            error = "Resposta vazia do servidor ViaCEP. Verifique sua conexão e tente novamente"
                        )

                    response.body()!!.erro == true ->
                        _addressState.value = AddressState(
                            error = "CEP $cepLimpo não foi encontrado. Verifique se digitou corretamente"
                        )

                    else -> {
                        val body = response.body()!!
                        _addressState.value = AddressState(
                            logradouro = body.logradouro.orEmpty(),
                            bairro = body.bairro.orEmpty(),
                            cidade = body.cidade.orEmpty(),
                            estado = body.estado.orEmpty()
                        )
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                _addressState.value = AddressState(
                    error = "Sem conexão com a internet. Verifique sua rede WiFi ou dados móveis e tente novamente"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _addressState.value = AddressState(
                    error = "Tempo de resposta esgotado. A conexão está muito lenta. Tente novamente"
                )
            } catch (e: java.io.IOException) {
                _addressState.value = AddressState(
                    error = "Erro de conexão de rede. Verifique sua internet e tente novamente"
                )
            } catch (e: Exception) {
                _addressState.value = AddressState(
                    error = "Erro inesperado ao buscar CEP: ${e.javaClass.simpleName} - ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun registerUser(
        nome: String,
        email: String,
        senha: String,
        cpf: String,
        dataNascimento: String,
        numeroCelular: String,
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

                val request = ClientRequest(
                    nome = nome,
                    email = email,
                    senha = senha,
                    cpf = cpf.filter { it.isDigit() },
                    dataNascimento = dataNascimento,
                    numeroCelular = numeroCelular.filter { it.isDigit() },
                    placa = placa.uppercase(),
                    modeloVeiculo = modeloVeiculo,
                    corVeiculo = corVeiculo,
                    cep = cep.filter { it.isDigit() },
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    cidade = cidade,
                    estado = estado.uppercase()
                )

                val response = ParadaCertaClient.service.cadastrar(request)
                val body = response.body()

                when {
                    response.isSuccessful && body?.sucesso == true -> {
                        _registerState.value = RegisterState(isSuccess = true)
                    }

                    response.code() == 400 && body != null -> {
                        // Erro de validação do servidor
                        val detailedError = when {
                            body.mensagem.contains("CPF", ignoreCase = true) ->
                                "CPF inválido ou já cadastrado: ${body.mensagem}"
                            body.mensagem.contains("email", ignoreCase = true) ->
                                "E-mail inválido ou já cadastrado: ${body.mensagem}"
                            body.mensagem.contains("placa", ignoreCase = true) ->
                                "Placa inválida ou já cadastrada: ${body.mensagem}"
                            body.mensagem.contains("data", ignoreCase = true) ->
                                "Data de nascimento inválida: ${body.mensagem}"
                            else -> body.mensagem
                        }
                        _registerState.value = RegisterState(errorMessage = detailedError)
                    }

                    response.code() == 409 -> {
                        _registerState.value = RegisterState(
                            errorMessage = "Conflito: ${body?.mensagem ?: "Dados já existem no sistema (CPF, e-mail ou placa duplicados)"}"
                        )
                    }

                    response.code() == 500 -> {
                        _registerState.value = RegisterState(
                            errorMessage = "Erro interno no servidor: ${body?.mensagem ?: "Falha ao processar cadastro. Tente novamente em alguns instantes"}"
                        )
                    }

                    response.code() == 503 -> {
                        _registerState.value = RegisterState(
                            errorMessage = "Servidor temporariamente indisponível. Aguarde alguns minutos e tente novamente"
                        )
                    }

                    body != null -> {
                        _registerState.value = RegisterState(
                            errorMessage = "Erro ao cadastrar: ${body.mensagem}"
                        )
                    }

                    else -> {
                        _registerState.value = RegisterState(
                            errorMessage = "Erro HTTP ${response.code()}: Falha desconhecida ao cadastrar. Verifique os dados e tente novamente"
                        )
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                _registerState.value = RegisterState(
                    errorMessage = "Sem conexão com o servidor. Verifique se:\n• Está conectado à internet\n• O servidor está acessível\n• O endereço da API está correto"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _registerState.value = RegisterState(
                    errorMessage = "Tempo de resposta esgotado (timeout). O servidor demorou muito para responder. Verifique sua conexão e tente novamente"
                )
            } catch (e: java.net.ConnectException) {
                _registerState.value = RegisterState(
                    errorMessage = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando e se o endereço está correto"
                )
            } catch (e: retrofit2.HttpException) {
                _registerState.value = RegisterState(
                    errorMessage = "Erro HTTP ${e.code()}: ${e.message() ?: "Falha na comunicação com o servidor"}"
                )
            } catch (e: com.google.gson.JsonSyntaxException) {
                _registerState.value = RegisterState(
                    errorMessage = "Erro ao processar resposta do servidor. Resposta inválida ou corrompida"
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState(
                    errorMessage = "Erro inesperado durante o cadastro:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState()
    }
}