package com.example.paradacerta.network

import com.example.paradacerta.models.ApiResponse
import com.example.paradacerta.models.ClientRequest
import com.example.paradacerta.models.EntradaAppRequest
import com.example.paradacerta.models.EntradaAppResponse
import com.example.paradacerta.models.Estacionamento
import com.example.paradacerta.models.FormaPagamento
import com.example.paradacerta.models.FormaPagamentoRequest
import com.example.paradacerta.models.SessaoAtivaResponse
import com.example.paradacerta.models.UserCompleteData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.paradacerta.models.LoginRequest
import retrofit2.http.Query

// Interface com todos os endpoints da API Spring Boot
interface ParadaCertaService {

    @POST("api/cadastro")
    suspend fun cadastrar(@Body request: ClientRequest): Response<ApiResponse>

    @PUT("api/salvar")
    suspend fun salvar(@Body request: ClientRequest): Response<ApiResponse>
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

    // ── Formas de Pagamento ──────────────────────────────────────────────

    @GET("api/pagamento/{cpf}")
    suspend fun listarFormasPagamento(@Path("cpf") cpf: String): Response<List<FormaPagamento>>

    @POST("api/pagamento")
    suspend fun salvarFormaPagamento(@Body request: FormaPagamentoRequest): Response<ApiResponse>

    @DELETE("api/pagamento/{id}")
    suspend fun deletarFormaPagamento(@Path("id") id: Int): Response<ApiResponse>

    // ── Premium ──────────────────────────────────────────────────────────

    @PUT("api/premium/{cpf}")
    suspend fun ativarPremium(@Path("cpf") cpf: String): Response<ApiResponse>

    @PUT("api/premium/cancelar/{cpf}")
    suspend fun cancelarPremium(@Path("cpf") cpf: String): Response<ApiResponse>

    @PUT("api/estacionamentos/{id}/entrada")
    suspend fun decrementarVaga(@Path("id") id: Int): Response<ApiResponse>

    @PUT("api/estacionamentos/{id}/saida")
    suspend fun incrementarVaga(@Path("id") id: Int): Response<ApiResponse>

    @GET("api/sessao/ativa/{cpf}")
    suspend fun buscarSessaoAtiva(@Path("cpf") cpf: String): Response<SessaoAtivaResponse>

    @POST("api/sessao/entrada/app")
    suspend fun registrarEntradaApp(@Body request: EntradaAppRequest): Response<EntradaAppResponse>
}

// Singleton do cliente Retrofit apontando para a API local
object ParadaCertaClient {

    private const val BASE_URL = "http://192.168.1.14:8080/"
    // private const val BASE_URL = "http://192.168.1.100:8080/" // dispositivo físico

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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