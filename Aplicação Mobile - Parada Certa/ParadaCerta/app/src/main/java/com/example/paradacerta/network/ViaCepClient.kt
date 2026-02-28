package com.example.paradacerta.network

import com.example.paradacerta.models.ViaCepResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// Interface com o endpoint da API
interface ViaCepService {
    @GET("ws/{cep}/json/")
    suspend fun buscarCep(@Path("cep") cep: String): Response<ViaCepResponse>
}

// Singleton do cliente Retrofit
object ViaCepClient {
    private const val BASE_URL = "https://viacep.com.br/"

    val service: ViaCepService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ViaCepService::class.java)
    }
}