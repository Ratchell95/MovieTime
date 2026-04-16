package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var btnIniciarSesion: Button
    private lateinit var btnGoogle:        MaterialButton
    private lateinit var btnInvitado:      Button
    private lateinit var tvRegistro:       TextView
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                procesarLoginGoogle(account)
            }

        } catch (e: ApiException) {
            Log.w("MainActivity", "Google sign in failed", e)
            Toast.makeText(this, "Error al iniciar con Google", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(SessionManager(this))

        if (SessionManager(this).isLoggedIn()) {
            startActivity(Intent(this, InicioActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnGoogle        = findViewById(R.id.btnGoogle)
        btnInvitado      = findViewById(R.id.btnInvitado)
        tvRegistro       = findViewById(R.id.tvRegistro)

        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
        }

        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
        btnInvitado.setOnClickListener {
            irAPeliculas()
        }
        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun procesarLoginGoogle(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val nombreCompleto = account.displayName ?: "Usuario Google"

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        var idUsuarioLocal = -1
        val cursor = db.rawQuery("SELECT id_usuario FROM usuarios WHERE email = ?", arrayOf(email))

        if (cursor.moveToFirst()) {

            idUsuarioLocal = cursor.getInt(0)
        } else {

            val cv = android.content.ContentValues().apply {
                put("nombres", nombreCompleto)
                put("apellidos", "")
                put("email", email)
                put("id_rol", 5)
                put("estado", "Activo")
                put("password_hash", "google_auth")
            }
            idUsuarioLocal = db.insertWithOnConflict("usuarios", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE).toInt()
        }
        cursor.close()

        if (idUsuarioLocal > 0) {

            SessionManager(this).guardarSesion(
                token = "google_token",
                idUsuario = idUsuarioLocal,
                nombres = nombreCompleto,
                rol = "Cliente",
                email = email
            )
            Toast.makeText(this, "Bienvenido $nombreCompleto", Toast.LENGTH_SHORT).show()
            irAPeliculas()
        } else {
            Toast.makeText(this, "Error al sincronizar cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irAPeliculas() {
        startActivity(Intent(this, InicioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}