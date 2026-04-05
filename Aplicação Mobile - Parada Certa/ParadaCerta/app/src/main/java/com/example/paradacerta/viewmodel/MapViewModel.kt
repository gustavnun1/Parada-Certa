package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.network.ParadaCertaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapState(
    val isLoading: Boolean = false,
    val estacionamentos: List<Estacionamento> = emptyList(),
    val errorMessage: String? = null,
    val userLatitude: Double = -23.550520,  // São Paulo (padrão)
    val userLongitude: Double = -46.633308
)

class MapViewModel : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    fun carregarEstacionamentosProximos(latitude: Double, longitude: Double, raioKm: Double = 5.0) {
        viewModelScope.launch {
            try {
                _mapState.value = _mapState.value.copy(
                    isLoading = true,
                    userLatitude = latitude,
                    userLongitude = longitude
                )

                val response = ParadaCertaClient.service.buscarEstacionamentosProximos(
                    latitude, longitude, raioKm
                )

                when {
                    response.isSuccessful && response.body() != null -> {
                        val estacionamentos = response.body()!!

                        if (estacionamentos.isEmpty()) {
                            _mapState.value = _mapState.value.copy(
                                isLoading = false,
                                estacionamentos = emptyList(),
                                errorMessage = "Nenhum estacionamento encontrado em um raio de ${raioKm}km da sua localização (Lat: ${"%.4f".format(latitude)}, Lng: ${"%.4f".format(longitude)})"
                            )
                        } else {
                            _mapState.value = _mapState.value.copy(
                                isLoading = false,
                                estacionamentos = estacionamentos,
                                errorMessage = null
                            )
                        }
                    }

                    response.code() == 400 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Parâmetros inválidos: Verifique se a latitude ($latitude) e longitude ($longitude) estão corretas"
                        )
                    }

                    response.code() == 404 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Endpoint não encontrado. A API pode estar com problemas"
                        )
                    }

                    response.code() == 500 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Erro interno no servidor ao buscar estacionamentos. Tente novamente em alguns instantes"
                        )
                    }

                    response.code() == 503 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Servidor temporariamente indisponível. Aguarde alguns minutos"
                        )
                    }

                    else -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Erro HTTP ${response.code()}: Falha desconhecida ao buscar estacionamentos próximos"
                        )
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Sem conexão com o servidor. Verifique sua internet e tente novamente"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Tempo de resposta esgotado ao buscar estacionamentos. Sua conexão pode estar lenta"
                )
            } catch (e: java.net.ConnectException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar ao servidor. Verifique se o servidor está rodando"
                )
            } catch (e: retrofit2.HttpException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro HTTP ${e.code()}: ${e.message() ?: "Falha na comunicação com o servidor"}"
                )
            } catch (e: com.google.gson.JsonSyntaxException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro ao processar resposta do servidor. Dados inválidos ou corrompidos"
                )
            } catch (e: IllegalArgumentException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Argumentos inválidos: ${e.message ?: "Latitude ou longitude fora do intervalo permitido"}"
                )
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro inesperado ao buscar estacionamentos:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }

    fun carregarTodos() {
        viewModelScope.launch {
            try {
                _mapState.value = _mapState.value.copy(isLoading = true)

                val response = ParadaCertaClient.service.listarEstacionamentos()

                when {
                    response.isSuccessful && response.body() != null -> {
                        val estacionamentos = response.body()!!

                        if (estacionamentos.isEmpty()) {
                            _mapState.value = _mapState.value.copy(
                                isLoading = false,
                                estacionamentos = emptyList(),
                                errorMessage = "Nenhum estacionamento cadastrado no sistema ainda"
                            )
                        } else {
                            _mapState.value = _mapState.value.copy(
                                isLoading = false,
                                estacionamentos = estacionamentos,
                                errorMessage = null
                            )
                        }
                    }

                    response.code() == 404 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Endpoint não encontrado. A API pode estar com problemas"
                        )
                    }

                    response.code() == 500 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Erro interno no servidor ao listar estacionamentos. Tente novamente"
                        )
                    }

                    response.code() == 503 -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Servidor temporariamente indisponível. Aguarde alguns minutos"
                        )
                    }

                    else -> {
                        _mapState.value = _mapState.value.copy(
                            isLoading = false,
                            errorMessage = "Erro HTTP ${response.code()}: Falha desconhecida ao listar estacionamentos"
                        )
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Sem conexão com o servidor. Verifique sua internet"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Tempo de resposta esgotado. Conexão muito lenta"
                )
            } catch (e: java.net.ConnectException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar ao servidor"
                )
            } catch (e: retrofit2.HttpException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro HTTP ${e.code()}: ${e.message() ?: "Falha na comunicação"}"
                )
            } catch (e: com.google.gson.JsonSyntaxException) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro ao processar resposta do servidor. Dados inválidos"
                )
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro inesperado:\n${e.javaClass.simpleName}: ${e.message ?: "Motivo desconhecido"}"
                )
            }
        }
    }
}