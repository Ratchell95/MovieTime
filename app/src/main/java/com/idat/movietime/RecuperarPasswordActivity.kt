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


    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var layoutPaso3: LinearLayout


    private lateinit var etDocumento:   TextInputEditText
    private lateinit var etCodigo:      TextInputEditText
    private lateinit var etNuevaPass:   TextInputEditText
    private lateinit var etConfirma:    TextInputEditText

    private var documentoIngresado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_password)

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

        btnEnviar.setOnClickListener {
            documentoIngresado = etDocumento.text.toString().trim()
            if (documentoIngresado.length < 7) {
                etDocumento.error = "Documento no válido"
                return@setOnClickListener
            }

            var usuarioExiste = false
            try {
                val dbHelper = com.idat.movietime.db.DatabaseHelper(this)
                val db = dbHelper.readableDatabase

                val cursor = db.rawQuery("SELECT id_usuario FROM usuarios WHERE documento = ?", arrayOf(documentoIngresado))


                if (cursor.moveToFirst()) {
                    usuarioExiste = true
                }
                cursor.close()
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }


            if (usuarioExiste) {

                layoutPaso1.visibility = View.GONE
                layoutPaso2.visibility = View.VISIBLE
                Toast.makeText(this, "Código enviado a tu correo", Toast.LENGTH_SHORT).show()
            } else {

                etDocumento.error = "Usuario no registrado"
                Toast.makeText(this, "No existe ninguna cuenta con ese documento", Toast.LENGTH_LONG).show()
            }
        }


        btnVerificar.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()

            if (codigo == "1234") {
                layoutPaso2.visibility = View.GONE
                layoutPaso3.visibility = View.VISIBLE
            } else {
                etCodigo.error = "Código incorrecto. Intenta con 1234."
            }
        }

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


            val nuevoSalt = generarSalt()
            val nuevoHash = hashSHA256ConSalt(nuevaPass, nuevoSalt)


            try {
                val dbHelper = DatabaseHelper(this)
                val db = dbHelper.writableDatabase
                val valores = ContentValues().apply {
                    put("password_hash", nuevoHash)
                }


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