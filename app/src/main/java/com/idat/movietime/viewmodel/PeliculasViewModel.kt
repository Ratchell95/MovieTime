package com.idat.movietime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula
import kotlinx.coroutines.launch


class PeliculasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PeliculasRepository(application)


    private val _peliculas = MutableLiveData<List<Pelicula>>()
    val peliculas: LiveData<List<Pelicula>> = _peliculas

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    fun cargarPeliculas(estado: String) {
        _cargando.value = true
        _error.value    = null

        viewModelScope.launch {
            try {
                // repository.getPeliculasPorEstado ya usa Dispatchers.IO internamente
                val lista = repository.getPeliculasPorEstado(estado)
                _peliculas.value = lista
            } catch (e: Exception) {
                _error.value = "Error al cargar películas: ${e.localizedMessage}"
            } finally {
                _cargando.value = false
            }
        }
    }


    fun insertarPelicula(pelicula: Pelicula) {
        viewModelScope.launch {
            try {
                repository.insertarPelicula(pelicula)
                cargarPeliculas(pelicula.estado)
            } catch (e: Exception) {
                _error.value = "Error al guardar la película: ${e.localizedMessage}"
            }
        }
    }
}
