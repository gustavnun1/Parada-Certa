package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.DevidaReservaExtra
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.EntradaAppRequest
import com.example.paradacerta.models.EntradaKioskRequest
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.SessaoStatus
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.network.ParadaCertaClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _userData = MutableStateFlow<Cliente?>(null)
    val userData: StateFlow<Cliente?> = _userData.asStateFlow()

    private val _veiculosData = MutableStateFlow<List<Veiculo>>(emptyList())
    val veiculosData: StateFlow<List<Veiculo>> = _veiculosData.asStateFlow()

    private val _enderecoData = MutableStateFlow<Endereco?>(null)
    val enderecoData: StateFlow<Endereco?> = _enderecoData.asStateFlow()

    private val _sessaoAtiva = MutableStateFlow<SessaoAtiva?>(null)
    val sessaoAtiva: StateFlow<SessaoAtiva?> = _sessaoAtiva.asStateFlow()

    private val _devidaReservaExtra = MutableStateFlow<DevidaReservaExtra?>(null)
    val devidaReservaExtra: StateFlow<DevidaReservaExtra?> = _devidaReservaExtra.asStateFlow()

    fun setDevidaReservaExtra(divida: DevidaReservaExtra?) {
        _devidaReservaExtra.value = divida
    }

    fun setUser(cliente: Cliente, veiculos: List<Veiculo>, endereco: Endereco?) {
        _userData.value = cliente
        _veiculosData.value = veiculos
        _enderecoData.value = endereco
    }

    fun iniciarSessao(sessao: SessaoAtiva) {
        _sessaoAtiva.value = sessao
    }

    fun iniciarSessaoComBackend(
        sessao: SessaoAtiva,
        placa: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val horaEntradaDispositivoMs = System.currentTimeMillis()
        val cpf = _userData.value?.cpf
        if (cpf.isNullOrBlank()) {
            onError("Nao foi possivel identificar o usuario.")
            return
        }

        viewModelScope.launch {
            try {
                val response = ParadaCertaClient.service.registrarEntradaApp(
                    EntradaAppRequest(
                        cpfUsuario = cpf,
                        estacionamentoId = sessao.estacionamentoId,
                        placa = placa,
                        horaEntradaDispositivoMs = horaEntradaDispositivoMs
                    )
                )
                if (!response.isSuccessful) {
                    onError(extrairMensagem(response, "Erro ao registrar entrada"))
                    return@launch
                }
                val r = response.body()
                if (r == null) {
                    onError("Entrada nao foi confirmada pelo servidor.")
                    return@launch
                }
                val pixKeyAtualizada = r.pixKey?.takeIf { it.isNotBlank() } ?: sessao.pixKey
                _sessaoAtiva.value = sessao.copy(
                    sessaoId = r.sessaoId,
                    horaEntrada = parseHoraEntrada(r.horaEntrada) ?: horaEntradaDispositivoMs,
                    pixKey = pixKeyAtualizada
                )
                onSuccess()
            } catch (_: java.net.UnknownHostException) {
                onError("Sem conexao com o servidor")
            } catch (_: java.net.SocketTimeoutException) {
                onError("Tempo de resposta esgotado")
            } catch (e: Exception) {
                onError("Erro ao registrar entrada: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    fun vincularSessaoKiosk(
        sessao: SessaoAtiva,
        placa: String,
        token: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cpf = _userData.value?.cpf
        if (cpf.isNullOrBlank()) {
            onError("Nao foi possivel identificar o usuario.")
            return
        }

        viewModelScope.launch {
            try {
                val response = ParadaCertaClient.service.vincularEntradaKiosk(
                    EntradaKioskRequest(
                        cpfUsuario = cpf,
                        token = token,
                        placa = placa
                    )
                )
                if (!response.isSuccessful) {
                    onError(extrairMensagem(response, "Erro ao vincular entrada"))
                    return@launch
                }
                val r = response.body()
                if (r == null) {
                    onError("Entrada nao foi confirmada pelo servidor.")
                    return@launch
                }
                val pixKeyAtualizada = r.pixKey?.takeIf { it.isNotBlank() } ?: sessao.pixKey
                _sessaoAtiva.value = sessao.copy(
                    sessaoId = r.sessaoId,
                    horaEntrada = parseHoraEntrada(r.horaEntrada) ?: sessao.horaEntrada,
                    pixKey = pixKeyAtualizada
                )
                onSuccess()
            } catch (_: java.net.UnknownHostException) {
                onError("Sem conexao com o servidor")
            } catch (_: java.net.SocketTimeoutException) {
                onError("Tempo de resposta esgotado")
            } catch (e: Exception) {
                onError("Erro ao vincular entrada: ${e.message ?: "Motivo desconhecido"}")
            }
        }
    }

    fun encerrarSessao() {
        _sessaoAtiva.value = null
        _devidaReservaExtra.value = null
    }

    fun restaurarSessaoAtiva(cpf: String) {
        viewModelScope.launch {
            try {
                val response = ParadaCertaClient.service.buscarSessaoAtiva(cpf)
                if (response.isSuccessful && response.body() == null) {
                    _sessaoAtiva.value = null
                    _devidaReservaExtra.value = null
                    return@launch
                }
                if (!response.isSuccessful) {
                    if (response.code() == 404) {
                        _sessaoAtiva.value = null
                        _devidaReservaExtra.value = null
                    }
                    return@launch
                }

                val r = response.body() ?: return@launch
                val status = SessaoStatus.fromString(r.status)
                _devidaReservaExtra.value = null
                _sessaoAtiva.value = SessaoAtiva(
                    estacionamentoId = r.estacionamentoId,
                    estacionamentoNome = r.estacionamentoNome,
                    modeloVeiculo = r.modeloVeiculo ?: "",
                    placa = r.placa ?: "",
                    precoHora = r.precoHora,
                    horaEntrada = r.horaEntrada,
                    inicioReservaPrevisto = r.inicioReservaPrevisto,
                    dataHoraConfirmacao = r.dataHoraConfirmacao,
                    sessaoId = r.sessaoId,
                    pixKey = r.pixKey ?: "",
                    reservado = r.reservado ?: false,
                    status = status,
                    valorPagoAntecipado = r.valorPagoAntecipado ?: 0.0
                )

                val reservaEmUso = r.reservado == true && (
                    status == SessaoStatus.EM_USO ||
                        (status == SessaoStatus.ATIVA && r.dataHoraConfirmacao != null)
                    )
                if (reservaEmUso) {
                    val baseUso = r.inicioReservaPrevisto ?: r.horaEntrada
                    val extraMin = (System.currentTimeMillis() - baseUso - 3_600_000L) / 60_000L
                    if (extraMin > 15) {
                        val extraHoras = kotlin.math.ceil(extraMin / 60.0)
                        _devidaReservaExtra.value = DevidaReservaExtra(
                            sessaoId = r.sessaoId,
                            valor = extraHoras * r.precoHora,
                            nomeEstacionamento = r.estacionamentoNome,
                            pixKey = r.pixKey ?: ""
                        )
                    }
                }
            } catch (_: Exception) {
                // Mantem o ultimo estado local se a restauracao falhar por rede/servidor.
            }
        }
    }

    fun atualizarDados(cpf: String) {
        viewModelScope.launch {
            runCatching {
                val response = ParadaCertaClient.service.getUserByCpf(cpf)
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        _userData.value = data.cliente
                        _veiculosData.value = data.veiculos
                        _enderecoData.value = data.endereco
                    }
                }
            }
        }
    }

    fun clearUser() {
        _userData.value = null
        _veiculosData.value = emptyList()
        _enderecoData.value = null
        _sessaoAtiva.value = null
        _devidaReservaExtra.value = null
    }

    private fun parseHoraEntrada(valor: String?): Long? {
        if (valor.isNullOrBlank()) return null
        return runCatching {
            val base = valor.take(19)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
            }.parse(base)?.time
        }.getOrNull()
    }

    private fun <T> extrairMensagem(response: retrofit2.Response<T>, fallback: String): String {
        return runCatching {
            val body = response.errorBody()?.string().orEmpty()
            val json = org.json.JSONObject(body)
            json.optString("mensagem")
                .takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: "$fallback (${response.code()})"
        }.getOrElse {
            "$fallback (${response.code()})"
        }
    }
}
