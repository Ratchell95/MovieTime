package com.idat.movietime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager
import com.idat.movietime.repository.AuthResult
import com.idat.movietime.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var btnIniciarSesion: Button
    private lateinit var btnGoogle:        Button
    private lateinit var btnInvitado:      Button
    private lateinit var tvRegistro:       TextView

    // 1. Instancias para Google y Firebase
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    // 2. Traemos el ViewModel aquí también
    private val viewModel: AuthViewModel by viewModels()

    // 3. Registramos el "lanzador" que abrirá la ventana de Google y esperará la respuesta
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Obtenemos la cuenta de Google exitosamente
                val account = task.getResult(ApiException::class.java)!!
                val email = account.email ?: ""
                val nombre = account.displayName ?: "Usuario Google"

                // Opcional: Si quieres autenticar también en Firebase (muy útil para FCM luego)
                firebaseAuthWithGoogle(account.idToken!!)

                // ¡LA MAGIA OCURRE AQUÍ! Mandamos los datos a Spring Boot
                btnGoogle.text = "Iniciando con Google..."
                btnGoogle.isEnabled = false
                viewModel.loginConGoogle(email, nombre)

            } catch (e: ApiException) {
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos Retrofit y revisamos si ya hay sesión
        RetrofitClient.init(SessionManager(this))
        if (viewModel.isLoggedIn()) {
            irAPeliculas()
            return
        }

        // Enlazamos las vistas
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnGoogle        = findViewById(R.id.btnGoogle)
        btnInvitado      = findViewById(R.id.btnInvitado)
        tvRegistro       = findViewById(R.id.tvRegistro)

        // Configurar Google Sign-In
        configurarGoogleSignIn()

        // Observar el resultado del Login con Google en el ViewModel
        observarViewModel()

        // --- CLICS DE LOS BOTONES ---

        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
        }

        btnInvitado.setOnClickListener {
            // Un invitado entra directo a las películas sin sesión
            irAPeliculas()
        }

        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // El clic que dispara la ventana de Google
        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun configurarGoogleSignIn() {
        firebaseAuth = FirebaseAuth.getInstance()

        // Configuración oficial. IMPORTANTE: Necesitas el Web Client ID de tu consola de Firebase
        // Por ahora, si no tienes el ID web estricto, esto pedirá al menos el correo
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
           .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Fallo en Firebase Auth", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun observarViewModel() {
        viewModel.loginResult.observe(this) { resultado ->
            btnGoogle.isEnabled = true
            btnGoogle.text = "Continuar con Google"

            when (resultado) {
                is AuthResult.Success -> irAPeliculas()
                is AuthResult.Error -> Toast.makeText(this, resultado.mensaje, Toast.LENGTH_LONG).show()
                is AuthResult.SinConexion -> Toast.makeText(this, "Sin conexión al servidor", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAPeliculas() {
        startActivity(Intent(this, PeliculasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}