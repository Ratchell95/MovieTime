package com.idat.movietime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.idat.movietime.repository.AuthRepository
import com.idat.movietime.repository.AuthResult
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    private val _loginResult  = MutableLiveData<AuthResult>()
    val loginResult: LiveData<AuthResult> = _loginResult

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> = _cargando

    private val _errorDocumento  = MutableLiveData<String?>()
    val errorDocumento: LiveData<String?> = _errorDocumento

    private val _errorPassword = MutableLiveData<String?>()
    val errorPassword: LiveData<String?> = _errorPassword

    fun login(documento: String, password: String) {


        _errorDocumento.value  = null
        _errorPassword.value = null

        if (documento.isBlank()) {
            _errorDocumento.value = "Ingresa tu número de documento"
            return
        }
        if (documento.length < 7) {
            _errorDocumento.value = "El documento debe tener al menos 7 dígitos"
            return
        }
        if (password.isBlank()) {
            _errorPassword.value = "Ingresa tu contraseña"
            return
        }
        if (password.length < 6) {
            _errorPassword.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }


        viewModelScope.launch {
            _cargando.value = true
            val resultado = repository.login(documento, password)
            _loginResult.value = resultado
            _cargando.value = false
        }
    }

    fun cerrarSesion() = repository.cerrarSesion()
    fun isLoggedIn()   = repository.isLoggedIn()
}
