package com.idat.movietime

import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.idat.movietime.db.DatabaseHelper

class RecuperarPasswordActivity : AppCompatActivity() {

    // Contenedores de los pasos
    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var layoutPaso3: LinearLayout

    // Inputs
    private lateinit var etDocumento:   TextInputEditText
    private lateinit var etCodigo:      TextInputEditText
    private lateinit var etNuevaPass:   TextInputEditText
    private lateinit var etConfirma:    TextInputEditText

    private var documentoIngresado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_password)

        // Inicializar vistas
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)
        layoutPaso3 = findViewById(R.id.layoutPaso3)

        etDocumento = findViewById(R.id.etDocumentoRecuperar)
        etCodigo    = findViewById(R.id.etCodigo)
        etNuevaPass = findViewById(R.id.etNuevaPass)
        etConfirma  = findViewById(R.id.etConfirmarPass)

        val btnAtras     = findViewById<TextView>(R.id.btnAtrasRecuperar)
        val btnEnviar    = findViewById<Button>(R.id.btnEnviarCodigo)
        val btnVerificar = findViewById<Button>(R.id.btnVerificarCodigo)
        val btnGuardar   = findViewById<Button>(R.id.btnGuardarPassword)

        btnAtras.setOnClickListener { finish() }

        // ── PASO 1: Enviar Documento ──────────────────────────────
        btnEnviar.setOnClickListener {
            documentoIngresado = etDocumento.text.toString().trim()
            if (documentoIngresado.length < 7) {
                etDocumento.error = "Documento no válido"
                return@setOnClickListener
            }

            // 1. Verificamos si el usuario existe en la base de datos local SQLite
            var usuarioExiste = false
            try {
                val dbHelper = com.idat.movietime.db.DatabaseHelper(this)
                val db = dbHelper.readableDatabase
                // Hacemos una consulta rápida buscando el documento
                val cursor = db.rawQuery("SELECT id_usuario FROM usuarios WHERE documento = ?", arrayOf(documentoIngresado))

                // Si moveToFirst() es true, significa que encontró al menos un registro
                if (cursor.moveToFirst()) {
                    usuarioExiste = true
                }
                cursor.close()
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Evaluamos el resultado
            if (usuarioExiste) {
                // El usuario SÍ existe. Pasamos al Paso 2.
                layoutPaso1.visibility = View.GONE
                layoutPaso2.visibility = View.VISIBLE
                Toast.makeText(this, "Código enviado a tu correo", Toast.LENGTH_SHORT).show()
            } else {
                // El usuario NO existe. Mostramos el error y detenemos el flujo.
                etDocumento.error = "Usuario no registrado"
                Toast.makeText(this, "No existe ninguna cuenta con ese documento", Toast.LENGTH_LONG).show()
            }
        }

        // ── PASO 2: Verificar Código ──────────────────────────────
        btnVerificar.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()

            // LA MAGIA DEL SIMULADOR ESTÁ AQUÍ
            if (codigo == "1234") {
                layoutPaso2.visibility = View.GONE
                layoutPaso3.visibility = View.VISIBLE
            } else {
                etCodigo.error = "Código incorrecto. Intenta con 1234."
            }
        }

        // ── PASO 3: Guardar Nueva Contraseña ──────────────────────
        btnGuardar.setOnClickListener {
            val nuevaPass = etNuevaPass.text.toString()
            val confirma  = etConfirma.text.toString()

            if (nuevaPass.length < 6) {
                etNuevaPass.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }
            if (nuevaPass != confirma) {
                etConfirma.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // 1. Encriptamos la nueva contraseña para mantener la seguridad
            val nuevoSalt = generarSalt()
            val nuevoHash = hashSHA256ConSalt(nuevaPass, nuevoSalt)

            // 2. Actualizamos la tabla usuarios en la base de datos local SQLite
            try {
                val dbHelper = DatabaseHelper(this)
                val db = dbHelper.writableDatabase
                val valores = ContentValues().apply {
                    put("password_hash", nuevoHash)
                }

                // Actualizamos específicamente al usuario que coincida con el documento
                db.update("usuarios", valores, "documento = ?", arrayOf(documentoIngresado))
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(this, "¡Contraseña actualizada con éxito!", Toast.LENGTH_LONG).show()
            finish() // Cierra esta pantalla y vuelve al Login
        }
    }

    private fun generarSalt(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashSHA256ConSalt(password: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
        return "$salt:${digest.joinToString("") { "%02x".format(it) }}"
    }
}