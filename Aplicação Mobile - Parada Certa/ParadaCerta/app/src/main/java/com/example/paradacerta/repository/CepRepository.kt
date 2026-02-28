package com.example.paradacerta.repository

import com.example.paradacerta.remote.RetrofitInstance

class CepRepository {

    suspend fun getAddressByCep(cep: String) =
        RetrofitInstance.cepService.getCep(cep)
}