package com.idat.movietime

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idat.movietime.db.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

class RegistroActivity : AppCompatActivity() {

    private lateinit var tilNombres:           TextInputLayout
    private lateinit var tilApellidos:         TextInputLayout
    private lateinit var tilEmail:             TextInputLayout // <- Nueva variable
    private lateinit var tilDocumento:         TextInputLayout
    private lateinit var tilPassword:          TextInputLayout
    private lateinit var tilConfirmarPassword: TextInputLayout
    private lateinit var etNombres:            TextInputEditText
    private lateinit var etApellidos:          TextInputEditText
    private lateinit var etEmail:              TextInputEditText // <- Nueva variable
    private lateinit var etDocumento:          TextInputEditText
    private lateinit var etPassword:           TextInputEditText
    private lateinit var etConfirmarPassword:  TextInputEditText
    private lateinit var btnRegistrar:         Button
    private lateinit var tvYaTengoSesion:      TextView
    private lateinit var dbHelper:             DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        dbHelper = DatabaseHelper(this)

        tilNombres           = findViewById(R.id.tilNombres)
        tilApellidos         = findViewById(R.id.tilApellidos)
        tilEmail             = findViewById(R.id.tilEmail) // <- Vinculado al XML
        tilDocumento         = findViewById(R.id.tilDocumento)
        tilPassword          = findViewById(R.id.tilPassword)
        tilConfirmarPassword = findViewById(R.id.tilConfirmarPassword)

        etNombres            = findViewById(R.id.etNombres)
        etApellidos          = findViewById(R.id.etApellidos)
        etEmail              = findViewById(R.id.etEmail) // <- Vinculado al XML
        etDocumento          = findViewById(R.id.etDocumento)
        etPassword           = findViewById(R.id.etPassword)
        etConfirmarPassword  = findViewById(R.id.etConfirmarPassword)

        btnRegistrar         = findViewById(R.id.btnRegistrar)
        tvYaTengoSesion      = findViewById(R.id.tvYaTengoSesion)

        btnRegistrar.setOnClickListener { intentarRegistrar() }

        tvYaTengoSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
            finish()
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  PASO 1 — Validaciones locales en el UI thread (sin I/O)
    // ──────────────────────────────────────────────────────────────
    private fun intentarRegistrar() {
        tilNombres.error           = null
        tilApellidos.error         = null
        tilEmail.error             = null // <- Limpiar error de correo
        tilDocumento.error         = null
        tilPassword.error          = null
        tilConfirmarPassword.error = null

        val nombres   = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val email     = etEmail.text.toString().trim() // <- Capturar texto de correo
        val documento = etDocumento.text.toString().trim()
        val password  = etPassword.text.toString().trim()
        val confirmar = etConfirmarPassword.text.toString().trim()

        // Validaciones sin I/O — permanecen en el UI thread
        if (nombres.isBlank())    { tilNombres.error   = "Ingresa tus nombres";                         return }
        if (apellidos.isBlank())  { tilApellidos.error = "Ingresa tus apellidos";                       return }

        // <- Nueva validación para el correo electrónico
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Ingresa un correo electrónico válido"; return
        }

        if (documento.isBlank())  { tilDocumento.error = "Ingresa tu número de documento";              return }
        if (documento.length < 7) { tilDocumento.error = "El documento debe tener al menos 7 dígitos"; return }
        if (password.isBlank())   { tilPassword.error  = "Ingresa una contraseña";                     return }
        if (password.length < 6)  { tilPassword.error  = "Mínimo 6 caracteres";                        return }
        if (password != confirmar){ tilConfirmarPassword.error = "Las contraseñas no coinciden";        return }

        // PASO 2 — Todo lo que toca la BD va al hilo de fondo

        registrarEnSegundoPlano(nombres, apellidos, email, documento, password)
    }

    // ──────────────────────────────────────────────────────────────
    //  PASO 2 — Coroutine: I/O en Dispatchers.IO, UI en Main
    // ──────────────────────────────────────────────────────────────
    private fun registrarEnSegundoPlano(
        nombres:   String,
        apellidos: String,
        email:     String, // <- Nuevo parámetro
        documento: String,
        password:  String
    ) {
        setUiCargando(true)

        lifecycleScope.launch {
            // ► Hilo de fondo — operaciones de base de datos
            val resultado: ResultadoRegistro = withContext(Dispatchers.IO) {
                // -> Pasamos el email a la función de BD
                ejecutarRegistroEnBD(nombres, apellidos, email, documento, password)
            }

            // ► Hilo principal — actualizar la UI con el resultado
            setUiCargando(false)

            when (resultado) {
                ResultadoRegistro.DocumentoDuplicado ->
                    tilDocumento.error = "Este documento ya está registrado"

                ResultadoRegistro.ErrorBD ->
                    Toast.makeText(this@RegistroActivity,
                        "Error al crear la cuenta", Toast.LENGTH_SHORT).show()

                ResultadoRegistro.Exito -> {
                    Toast.makeText(this@RegistroActivity,
                        "¡Cuenta creada! Ya puedes iniciar sesión",
                        Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@RegistroActivity, SesionActivity::class.java))
                    finish()
                }
            }
        }
    }
    // ──────────────────────────────────────────────────────────────
    //  Lógica pura de BD — SIEMPRE se llama desde Dispatchers.IO
    //  ¡NUNCA tocar vistas aquí!
    // ──────────────────────────────────────────────────────────────
    private fun ejecutarRegistroEnBD(
        nombres:   String,
        apellidos: String,
        email:     String,
        documento: String,
        password:  String
    ): ResultadoRegistro {

        // 1. Verificar duplicado
        dbHelper.readableDatabase.rawQuery(
            "SELECT id_usuario FROM usuarios WHERE documento = ?",
            arrayOf(documento)
        ).use { cursor ->
            if (cursor.count > 0) return ResultadoRegistro.DocumentoDuplicado
        }

        // 2. Hash seguro SHA-256 + salt
        val salt         = generarSalt()
        val passwordHash = hashSHA256ConSalt(password, salt)

        // 3. Insertar usuario
        val values = ContentValues().apply {
            put("nombres",       nombres)
            put("apellidos",     apellidos)
            put("email",         email)                        // <--- CAMBIO: Ahora usa el correo real
            put("documento",     documento)                    // ← columna correcta para login
            put("password_hash", passwordHash)
            put("id_rol",        3)
            put("estado",        "Activo")
        }
        val rowId = dbHelper.writableDatabase
            .insert("usuarios", null, values)

        return if (rowId != -1L) ResultadoRegistro.Exito
        else              ResultadoRegistro.ErrorBD
    }

    // ──────────────────────────────────────────────────────────────
    //  UI helpers
    // ──────────────────────────────────────────────────────────────
    private fun setUiCargando(cargando: Boolean) {
        btnRegistrar.isEnabled = !cargando
        btnRegistrar.text      = if (cargando) "Registrando..." else "Registrar"
    }

    // ──────────────────────────────────────────────────────────────
    //  Hash seguro
    // ──────────────────────────────────────────────────────────────

    /** Genera salt aleatorio de 16 bytes como hex. */
    private fun generarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Devuelve "<salt>:<sha256(salt+password)>" para almacenar en BD.
     *
     * Para verificar en login (AuthViewModel):
     *   val (salt, hash) = storedHash.split(":")
     *   val esValido = hashSHA256ConSalt(passwordIngresado, salt) == storedHash
     */
    private fun hashSHA256ConSalt(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
        return "$salt:${digest.joinToString("") { "%02x".format(it) }}"
    }

    // ──────────────────────────────────────────────────────────────
    //  Resultado tipado (evita magic numbers / booleans ambiguos)
    // ──────────────────────────────────────────────────────────────
    private sealed class ResultadoRegistro {
        object Exito              : ResultadoRegistro()
        object DocumentoDuplicado : ResultadoRegistro()
        object ErrorBD            : ResultadoRegistro()
    }
}