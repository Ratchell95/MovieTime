package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager
import com.idat.movietime.repository.AuthResult
import com.idat.movietime.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
class SesionActivity : AppCompatActivity() {

    private lateinit var tilDocumento:  TextInputLayout
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etDocumento:   TextInputEditText
    private lateinit var etContrasena:  TextInputEditText
    private lateinit var btnIngresar:   Button

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sesion)

        RetrofitClient.init(SessionManager(this))

        if (viewModel.isLoggedIn()) { irAPeliculas(); return }

        tilDocumento  = findViewById(R.id.tilDocumento)
        tilContrasena = findViewById(R.id.tilContrasena)
        etDocumento   = findViewById(R.id.etDocumento)
        etContrasena  = findViewById(R.id.etContrasena)
        btnIngresar   = findViewById(R.id.btnIngresar)
        val tvOlvideContrasena = findViewById<android.widget.TextView>(R.id.tvOlvideContrasena)
        observarViewModel()

        btnIngresar.setOnClickListener {
            tilDocumento.error  = null
            tilContrasena.error = null
            viewModel.login(
                etDocumento.text.toString().trim(),
                etContrasena.text.toString().trim()
            )
        }
        tvOlvideContrasena.setOnClickListener {
            val intent = Intent(this, RecuperarPasswordActivity::class.java)
            startActivity(intent)
        }
        observarViewModel()
    }

    private fun observarViewModel() {
        viewModel.cargando.observe(this) { cargando ->
            btnIngresar.isEnabled = !cargando
            btnIngresar.text = if (cargando) "Verificando..." else "Ingresar"
        }
        viewModel.errorDocumento.observe(this) { tilDocumento.error = it }
        viewModel.errorPassword.observe(this)  { tilContrasena.error = it }
        viewModel.loginResult.observe(this) { resultado ->
            when (resultado) {
                is AuthResult.Success     -> irAPeliculas()
                is AuthResult.Error       -> {
                    Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show()
                    etContrasena.text?.clear()
                }
                is AuthResult.SinConexion -> Toast.makeText(this,
                    "Sin conexión. Verifica tu red o credenciales.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAPeliculas() {
        startActivity(Intent(this, PeliculasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun enviarGoogleAlBackend(emailGoogle: String, nombreGoogle: String) {
        val request = mapOf("email" to emailGoogle, "nombres" to nombreGoogle)

        lifecycleScope.launch {
            val response = RetrofitClient.api.loginConGoogle(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val loginResponse = response.body()?.data

                if (loginResponse != null) {

                    SessionManager(this@SesionActivity).guardarSesion(
                        token = loginResponse.token,
                        idUsuario = loginResponse.idUsuario,
                        nombres = loginResponse.nombres,
                        rol = loginResponse.rol,
                        email = loginResponse.email
                    )

                    Toast.makeText(this@SesionActivity, "¡Ingreso con Google exitoso!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SesionActivity, InicioActivity::class.java))
                    finish()
                }
            }
        }
    }
}