package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager
import com.idat.movietime.repository.AuthResult
import com.idat.movietime.viewmodel.AuthViewModel

class SesionActivity : AppCompatActivity() {

    private lateinit var tilDocumento:  TextInputLayout
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etDocumento:   TextInputEditText
    private lateinit var etContrasena:  TextInputEditText
    private lateinit var btnIngresar:   Button
    private lateinit var tvOlvideContrasena: TextView

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sesion)

        RetrofitClient.init(SessionManager(this))


        if (SessionManager(this).isLoggedIn()) {
            irAPeliculas()
            return
        }

        tilDocumento  = findViewById(R.id.tilDocumento)
        tilContrasena = findViewById(R.id.tilContrasena)
        etDocumento   = findViewById(R.id.etDocumento)
        etContrasena  = findViewById(R.id.etContrasena)
        btnIngresar   = findViewById(R.id.btnIngresar)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena)

        btnIngresar.setOnClickListener {
            val doc = etDocumento.text.toString().trim()
            val pass = etContrasena.text.toString().trim()
            viewModel.login(doc, pass)
        }

        tvOlvideContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarPasswordActivity::class.java))
        }

        observarViewModel()
    }

    private fun observarViewModel() {
        viewModel.loginResult.observe(this) { resultado ->
            when (resultado) {
                is AuthResult.Success -> {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    irAPeliculas()
                }
                is AuthResult.Error -> {
                    Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show()
                }
                is AuthResult.SinConexion -> {
                    Toast.makeText(this, "Sin conexión o credenciales inválidas.", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.cargando.observe(this) { isLoading ->
            btnIngresar.isEnabled = !isLoading
            btnIngresar.text = if (isLoading) "Cargando..." else "Ingresar"
        }

        viewModel.errorDocumento.observe(this) { error ->
            tilDocumento.error = error
        }

        viewModel.errorPassword.observe(this) { error ->
            tilContrasena.error = error
        }
    }

    private fun irAPeliculas() {
        startActivity(Intent(this, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}