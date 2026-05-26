package com.example.paradacerta.models

import com.google.gson.annotations.SerializedName

data class ViaCepResponse(
    @SerializedName("cep")        val cep: String?,
    @SerializedName("logradouro") val logradouro: String?,
    @SerializedName("bairro")     val bairro: String?,
    @SerializedName("localidade") val cidade: String?,
    @SerializedName("uf")         val estado: String?,
    @SerializedName("erro")       val erro: Boolean?
)