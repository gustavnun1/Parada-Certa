package com.example.paradacerta.viewmodel

import androidx.lifecycle.ViewModel
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.Veiculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserViewModel : ViewModel() {

    private val _userData = MutableStateFlow<Cliente?>(null)
    val userData: StateFlow<Cliente?> = _userData.asStateFlow()

    private val _veiculoData = MutableStateFlow<Veiculo?>(null)
    val veiculoData: StateFlow<Veiculo?> = _veiculoData.asStateFlow()

    private val _enderecoData = MutableStateFlow<Endereco?>(null)
    val enderecoData: StateFlow<Endereco?> = _enderecoData.asStateFlow()

    fun setUser(cliente: Cliente, veiculo: Veiculo?, endereco: Endereco?) {
        _userData.value = cliente
        _veiculoData.value = veiculo
        _enderecoData.value = endereco
    }

    fun clearUser() {
        _userData.value = null
        _veiculoData.value = null
        _enderecoData.value = null
    }
}