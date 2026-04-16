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
import com.idat.movietime.repository.AuthRepository
import com.idat.movietime.repository.AuthResult
import com.idat.movietime.viewmodel.AuthViewModel

class RegistroActivity : AppCompatActivity() {


    private lateinit var repository: AuthRepository

    private lateinit var tilNombres:           TextInputLayout
    private lateinit var tilApellidos:         TextInputLayout
    private lateinit var tilEmail:             TextInputLayout
    private lateinit var tilDocumento:         TextInputLayout
    private lateinit var tilPassword:          TextInputLayout
    private lateinit var tilConfirmarPassword: TextInputLayout

    private lateinit var etNombres:            TextInputEditText
    private lateinit var etApellidos:          TextInputEditText
    private lateinit var etEmail:              TextInputEditText
    private lateinit var etDocumento:          TextInputEditText
    private lateinit var etPassword:           TextInputEditText
    private lateinit var etConfirmarPassword:  TextInputEditText

    private lateinit var btnRegistrar:         Button
    private lateinit var tvYaTengoSesion:      TextView

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_registro)


        repository = AuthRepository(this)

        RetrofitClient.init(SessionManager(this))

        tilNombres           = findViewById(R.id.tilNombres)
        tilApellidos         = findViewById(R.id.tilApellidos)
        tilEmail             = findViewById(R.id.tilEmail)
        tilDocumento         = findViewById(R.id.tilDocumento)
        tilPassword          = findViewById(R.id.tilPassword)
        tilConfirmarPassword = findViewById(R.id.tilConfirmarPassword)

        etNombres            = findViewById(R.id.etNombres)
        etApellidos          = findViewById(R.id.etApellidos)
        etEmail              = findViewById(R.id.etEmail)
        etDocumento          = findViewById(R.id.etDocumento)
        etPassword           = findViewById(R.id.etPassword)
        etConfirmarPassword  = findViewById(R.id.etConfirmarPassword)

        btnRegistrar         = findViewById(R.id.btnRegistrar)
        tvYaTengoSesion      = findViewById(R.id.tvYaTengoSesion)

        btnRegistrar.setOnClickListener {
            if (validarCampos()) {
                val nombres   = etNombres.text.toString().trim()
                val apellidos = etApellidos.text.toString().trim()
                val email     = etEmail.text.toString().trim()
                val documento = etDocumento.text.toString().trim()
                val password  = etPassword.text.toString().trim()

                viewModel.registro(nombres, apellidos, email, documento, password)
            }
        }

        tvYaTengoSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
            finish()
        }

        observarViewModel()
    }


    private fun validarCampos(): Boolean {
        var valido = true

        tilNombres.error = null
        tilApellidos.error = null
        tilEmail.error = null
        tilDocumento.error = null
        tilPassword.error = null
        tilConfirmarPassword.error = null

        if (etNombres.text.isNullOrBlank()) {
            tilNombres.error = "Ingresa tus nombres"
            valido = false
        }

        if (etApellidos.text.isNullOrBlank()) {
            tilApellidos.error = "Ingresa tus apellidos"
            valido = false
        }

        val email = etEmail.text.toString().trim()
        if (email.isBlank()) {
            tilEmail.error = "Ingresa tu correo"
            valido = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo no válido"
            valido = false
        }

        val doc = etDocumento.text.toString().trim()
        if (doc.isBlank()) {
            tilDocumento.error = "Ingresa tu número de documento"
            valido = false
        } else if (doc.length < 8) {
            tilDocumento.error = "Mínimo 8 dígitos"
            valido = false
        }

        val pass = etPassword.text.toString()
        if (pass.isBlank()) {
            tilPassword.error = "Ingresa una contraseña"
            valido = false
        } else if (pass.length < 6) {
            tilPassword.error = "Mínimo 6 caracteres"
            valido = false
        }

        val confirmar = etConfirmarPassword.text.toString()
        if (confirmar.isBlank()) {
            tilConfirmarPassword.error = "Confirma tu contraseña"
            valido = false
        } else if (pass != confirmar) {
            tilConfirmarPassword.error = "Las contraseñas no coinciden"
            valido = false
        }

        return valido
    }


    private fun observarViewModel() {
        viewModel.registroResult.observe(this) { resultado ->
            when (resultado) {
                is AuthResult.Success -> {
                    Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, PeliculasActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                is AuthResult.Error -> {
                    Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show()
                }
                is AuthResult.SinConexion -> {
                    Toast.makeText(this, "Sin conexión o servidor no disponible", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.cargando.observe(this) { isLoading ->
            btnRegistrar.isEnabled = !isLoading
            btnRegistrar.text = if (isLoading) "Registrando..." else "Crear cuenta"
        }
    }
}