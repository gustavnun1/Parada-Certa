package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.ClienteUpdateRequest
import com.example.paradacerta.network.ParadaCertaClient
import com.example.paradacerta.network.ViaCepClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddressStateSave(
    val logradouro: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val estado: String = "",
    val error: String? = null
)

data class SaveState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class SaveViewModel : ViewModel() {

    private val _addressStateSave = MutableStateFlow(AddressStateSave())
    val addressStateSave: StateFlow<AddressStateSave> = _addressStateSave.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun fetchCep(cep: String) {
        val cepLimpo = cep.filter { it.isDigit() }

        if (cepLimpo.length != 8) {
            _addressStateSave.value = AddressStateSave(
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
                            500 -> "Erro no servidor do ViaCEP"
                            503 -> "Serviço ViaCEP temporariamente indisponível"
                            else -> "Erro ${response.code()}: Falha ao buscar CEP"
                        }
                        _addressStateSave.value = AddressStateSave(error = errorDetail)
                    }

                    response.body() == null ->
                        _addressStateSave.value = AddressStateSave(
                            error = "Resposta vazia do servidor ViaCEP"
                        )

                    response.body()!!.erro == true ->
                        _addressStateSave.value = AddressStateSave(
                            error = "CEP $cepLimpo não encontrado"
                        )

                    else -> {
                        val body = response.body()!!
                        _addressStateSave.value = AddressStateSave(
                            logradouro = body.logradouro.orEmpty(),
                            bairro = body.bairro.orEmpty(),
                            cidade = body.cidade.orEmpty(),
                            estado = body.estado.orEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _addressStateSave.value = AddressStateSave(
                    error = "Erro ao buscar CEP: ${e.javaClass.simpleName} - ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun saveUser(
        nome: String,
        email: String,
        senha: String,
        cpf: String,
        dataNascimento: String,
        numeroCelular: String,
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
                _saveState.value = SaveState(isLoading = true)

                val request = ClienteUpdateRequest(
                    cpf = cpf.filter { it.isDigit() },
                    nome = nome,
                    email = email,
                    senha = senha,
                    dataNascimento = dataNascimento,
                    numeroCelular = numeroCelular.filter { it.isDigit() },
                    cep = cep.filter { it.isDigit() },
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    cidade = cidade,
                    estado = estado.uppercase()
                )

                val response = ParadaCertaClient.service.salvar(request)
                val body = response.body()

                when {
                    response.isSuccessful && body?.sucesso == true -> {
                        _saveState.value = SaveState(isSuccess = true)
                    }

                    response.code() == 400 && body != null -> {
                        val detailedError = when {
                            body.mensagem.contains("CPF", ignoreCase = true) ->
                                "CPF inválido: ${body.mensagem}"
                            body.mensagem.contains("email", ignoreCase = true) ->
                                "E-mail inválido ou já em uso: ${body.mensagem}"
                            body.mensagem.contains("data", ignoreCase = true) ->
                                "Data de nascimento inválida: ${body.mensagem}"
                            else -> "Dados inválidos: ${body.mensagem}"
                        }
                        _saveState.value = SaveState(errorMessage = detailedError)
                    }

                    response.code() == 404 -> {
                        _saveState.value = SaveState(
                            errorMessage = "Usuário não encontrado. Você pode ter sido removido do sistema"
                        )
                    }

                    response.code() == 409 -> {
                        _saveState.value = SaveState(
                            errorMessage = "Conflito: ${body?.mensagem ?: "E-mail já em uso por outro usuário"}"
                        )
                    }

                    response.code() == 500 -> {
                        _saveState.value = SaveState(
                            errorMessage = "Erro interno no servidor ao salvar: ${body?.mensagem ?: "Tente novamente em alguns instantes"}"
                        )
                    }

                    body != null -> {
                        _saveState.value = SaveState(
                            errorMessage = "Erro ao salvar alterações: ${body.mensagem}"
                        )
                    }

                    else -> {
                        _saveState.value = SaveState(
                            errorMessage = "Erro HTTP ${response.code()}: Falha desconhecida ao salvar"
                        )
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                _saveState.value = SaveState(
                    errorMessage = "Sem conexão com o servidor. Verifique sua internet"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _saveState.value = SaveState(
                    errorMessage = "Tempo de resposta esgotado. Tente novamente"
                )
            } catch (e: java.net.ConnectException) {
                _saveState.value = SaveState(
                    errorMessage = "Não foi possível conectar ao servidor"
                )
            } catch (e: Exception) {
                _saveState.value = SaveState(
                    errorMessage = "Erro inesperado ao salvar:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState()
    }
}
