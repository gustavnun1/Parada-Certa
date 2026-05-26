package com.example.paradacerta.network

import com.example.paradacerta.models.ApiResponse
import com.example.paradacerta.models.ClientRequest
import com.example.paradacerta.models.ClienteUpdateRequest
import com.example.paradacerta.models.CobrancaEstadia
import com.example.paradacerta.models.EntradaAppRequest
import com.example.paradacerta.models.EntradaAppResponse
import com.example.paradacerta.models.EntradaKioskRequest
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.models.EstacionamentoFotoItem
import com.example.paradacerta.models.FormaPagamento
import com.example.paradacerta.models.FormaPagamentoRequest
import com.example.paradacerta.models.PixKeyResponse
import com.example.paradacerta.models.SessaoAtivaResponse
import com.example.paradacerta.models.UserCompleteData
import com.example.paradacerta.models.Veiculo
import com.example.paradacerta.models.VeiculoAdicionarRequest
import com.example.paradacerta.models.VeiculoAtualizarRequest
import com.example.paradacerta.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.paradacerta.models.AvaliacaoItem
import com.example.paradacerta.models.AvaliacaoRequest
import com.example.paradacerta.models.ConfirmarRecuperacaoRequest
import com.example.paradacerta.models.LoginRequest
import com.example.paradacerta.models.ReservaRequest
import com.example.paradacerta.models.SolicitarRecuperacaoRequest
import com.example.paradacerta.models.ReservaResponse
import retrofit2.http.Query

// Interface com todos os endpoints da API Spring Boot
interface ParadaCertaService {

    @POST("api/cadastro")
    suspend fun cadastrar(@Body request: ClientRequest): Response<ApiResponse>

    @PUT("api/salvar")
    suspend fun salvar(@Body request: ClienteUpdateRequest): Response<ApiResponse>

    // ── Veículos ─────────────────────────────────────────────────────────────

    @GET("api/veiculo/cliente/{cpf}")
    suspend fun listarVeiculos(@Path("cpf") cpf: String): Response<List<Veiculo>>

    @POST("api/veiculo")
    suspend fun adicionarVeiculo(@Body request: VeiculoAdicionarRequest): Response<ApiResponse>

    @PUT("api/veiculo/{placa}")
    suspend fun atualizarVeiculo(
        @Path("placa") placa: String,
        @Body request: VeiculoAtualizarRequest
    ): Response<ApiResponse>

    @DELETE("api/veiculo/{placa}")
    suspend fun removerVeiculo(
        @Path("placa") placa: String,
        @Query("cpf") cpf: String
    ): Response<ApiResponse>
    @DELETE("api/conta/{cpf}")
    suspend fun deletarConta(@Path("cpf") cpf: String): Response<ApiResponse>
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse>
    @GET("api/usuario/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): Response<UserCompleteData>

    @GET("api/usuario/cpf/{cpf}")
    suspend fun getUserByCpf(@Path("cpf") cpf: String): Response<UserCompleteData>

    @GET("api/verificar/cpf/{cpf}")
    suspend fun verificarCpf(@Path("cpf") cpf: String): Response<ApiResponse>

    @GET("api/verificar/placa/{placa}")
    suspend fun verificarPlaca(@Path("placa") placa: String): Response<ApiResponse>

    @GET("api/health")
    suspend fun health(): Response<ApiResponse>

    @GET("api/estacionamentos")
    suspend fun listarEstacionamentos(): Response<List<Estacionamento>>

    @GET("api/estacionamentos/proximos")
    suspend fun buscarEstacionamentosProximos(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("raio") raioKm: Double = 5.0
    ): Response<List<Estacionamento>>

    @GET("api/estacionamentos/{id}")
    suspend fun buscarEstacionamentoPorId(@Path("id") id: Int): Response<Estacionamento>

    @GET("api/estacionamentos/{id}/fotos")
    suspend fun listarFotosEstacionamento(
        @Path("id") estacionamentoId: Int
    ): Response<List<EstacionamentoFotoItem>>

    // ── Formas de Pagamento ──────────────────────────────────────────────

    @GET("api/pagamento/{cpf}")
    suspend fun listarFormasPagamento(@Path("cpf") cpf: String): Response<List<FormaPagamento>>

    @POST("api/pagamento")
    suspend fun salvarFormaPagamento(@Body request: FormaPagamentoRequest): Response<ApiResponse>

    @DELETE("api/pagamento/{id}")
    suspend fun deletarFormaPagamento(
        @Path("id") id: Int,
        @Query("cpf") cpf: String
    ): Response<ApiResponse>

    @GET("api/sessao/ativa/{cpf}")
    suspend fun buscarSessaoAtiva(@Path("cpf") cpf: String): Response<SessaoAtivaResponse>

    @POST("api/sessao/entrada/app")
    suspend fun registrarEntradaApp(@Body request: EntradaAppRequest): Response<EntradaAppResponse>

    @POST("api/sessao/entrada/kiosk")
    suspend fun vincularEntradaKiosk(@Body request: EntradaKioskRequest): Response<EntradaAppResponse>

    @POST("api/sessao/encerrar/{sessaoId}")
    suspend fun encerrarSessao(
        @Path("sessaoId") sessaoId: String,
        @Query("valorPago") valorPago: Double
    ): Response<ApiResponse>

    @GET("api/sessao/{sessaoId}/calculo-cobranca")
    suspend fun calcularCobrancaEstadia(
        @Path("sessaoId") sessaoId: String
    ): Response<CobrancaEstadia>

    @POST("api/recuperar-senha/solicitar")
    suspend fun solicitarCodigoRecuperacao(@Body request: SolicitarRecuperacaoRequest): Response<ApiResponse>

    @POST("api/recuperar-senha/confirmar")
    suspend fun confirmarCodigoRecuperacao(@Body request: ConfirmarRecuperacaoRequest): Response<ApiResponse>

    // ── Reserva de Vagas ─────────────────────────────────────────────────

    @POST("api/reserva")
    suspend fun criarReserva(@Body request: ReservaRequest): Response<ReservaResponse>

    @DELETE("api/reserva/{sessaoId}")
    suspend fun cancelarReserva(@Path("sessaoId") sessaoId: String): Response<ApiResponse>

    @POST("api/reserva/{sessaoId}/finalizar")
    suspend fun finalizarReserva(@Path("sessaoId") sessaoId: String): Response<ApiResponse>

    // ── Configuração ─────────────────────────────────────────────────────────

    @GET("api/config/pix-key")
    suspend fun obterPixKeyEmpresa(): Response<PixKeyResponse>

    // ── Avaliação ────────────────────────────────────────────────────────────

    @POST("api/avaliacao")
    suspend fun avaliar(@Body request: AvaliacaoRequest): Response<ApiResponse>

    @GET("api/avaliacao/estacionamento/{id}")
    suspend fun listarAvaliacoes(@Path("id") estacionamentoId: Int): Response<List<AvaliacaoItem>>
}

// Singleton do cliente Retrofit apontando para a API local
object ParadaCertaClient {

    private const val BASE_URL = BuildConfig.API_BASE_URL

    /** Raiz do servidor sem barra final — usada para montar URLs absolutas de recursos estáticos (`/uploads/...`). */
    val serverRoot: String = BASE_URL.trimEnd('/')

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: ParadaCertaService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParadaCertaService::class.java)
    }
}
