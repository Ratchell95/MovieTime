package com.idat.movietime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula
import kotlinx.coroutines.launch

/**
 * ViewModel para PeliculasTabFragment.
 *
 * Centraliza el acceso al repositorio y expone LiveData al Fragment.
 * viewModelScope cancela automáticamente los coroutines cuando el
 * ViewModel se destruye, evitando leaks.
 *
 * Uso en Fragment:
 *
 *   private val viewModel: PeliculasViewModel by viewModels()
 *
 *   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *       viewModel.cargarPeliculas("Activa")
 *
 *       viewModel.peliculas.observe(viewLifecycleOwner) { lista ->
 *           adapter.submitList(lista)
 *       }
 *       viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
 *           progressBar.isVisible = cargando
 *       }
 *       viewModel.error.observe(viewLifecycleOwner) { mensaje ->
 *           if (mensaje != null) Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
 *       }
 *   }
 */
class PeliculasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PeliculasRepository(application)

    // ── Observables expuestos al Fragment ──────────────────────────
    private val _peliculas = MutableLiveData<List<Pelicula>>()
    val peliculas: LiveData<List<Pelicula>> = _peliculas

    private val _cargando = MutableLiveData<Boolean>()
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ── Acción pública ─────────────────────────────────────────────

    /**
     * Carga películas por estado en un coroutine de background.
     * El Fragment nunca toca la BD directamente.
     */
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

    /**
     * Inserta una película y recarga la lista del mismo estado.
     */
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
