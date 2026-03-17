package com.idat.movietime

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idat.movietime.db.MovieTimeDatabaseHelper
import java.security.MessageDigest

class RegistroActivity : AppCompatActivity() {

    private lateinit var tilNombres:          TextInputLayout
    private lateinit var tilApellidos:        TextInputLayout
    private lateinit var tilDocumento:        TextInputLayout
    private lateinit var tilPassword:         TextInputLayout
    private lateinit var tilConfirmarPassword: TextInputLayout
    private lateinit var etNombres:           TextInputEditText
    private lateinit var etApellidos:         TextInputEditText
    private lateinit var etDocumento:         TextInputEditText
    private lateinit var etPassword:          TextInputEditText
    private lateinit var etConfirmarPassword: TextInputEditText
    private lateinit var btnRegistrar:        Button
    private lateinit var tvYaTengoSesion:     TextView
    private lateinit var dbHelper:            MovieTimeDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        dbHelper = MovieTimeDatabaseHelper(this)

        tilNombres           = findViewById(R.id.tilNombres)
        tilApellidos         = findViewById(R.id.tilApellidos)
        tilDocumento         = findViewById(R.id.tilDocumento)
        tilPassword          = findViewById(R.id.tilPassword)
        tilConfirmarPassword = findViewById(R.id.tilConfirmarPassword)
        etNombres            = findViewById(R.id.etNombres)
        etApellidos          = findViewById(R.id.etApellidos)
        etDocumento          = findViewById(R.id.etDocumento)
        etPassword           = findViewById(R.id.etPassword)
        etConfirmarPassword  = findViewById(R.id.etConfirmarPassword)
        btnRegistrar         = findViewById(R.id.btnRegistrar)
        tvYaTengoSesion      = findViewById(R.id.tvYaTengoSesion)

        btnRegistrar.setOnClickListener { registrar() }

        tvYaTengoSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
            finish()
        }
    }

    private fun registrar() {

        tilNombres.error           = null
        tilApellidos.error         = null
        tilDocumento.error         = null
        tilPassword.error          = null
        tilConfirmarPassword.error = null

        val nombres   = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val documento = etDocumento.text.toString().trim()
        val password  = etPassword.text.toString().trim()
        val confirmar = etConfirmarPassword.text.toString().trim()


        if (nombres.isBlank())   { tilNombres.error = "Ingresa tus nombres"; return }
        if (apellidos.isBlank()) { tilApellidos.error = "Ingresa tus apellidos"; return }
        if (documento.isBlank()) { tilDocumento.error = "Ingresa tu número de documento"; return }
        if (documento.length < 7){ tilDocumento.error = "El documento debe tener al menos 7 dígitos"; return }
        if (password.isBlank())  { tilPassword.error = "Ingresa una contraseña"; return }
        if (password.length < 6) { tilPassword.error = "Mínimo 6 caracteres"; return }
        if (password != confirmar){ tilConfirmarPassword.error = "Las contraseñas no coinciden"; return }


        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id_usuario FROM usuarios WHERE email = ?",
            arrayOf(documento)
        )
        if (cursor.count > 0) {
            cursor.close()
            tilDocumento.error = "Este documento ya está registrado"
            return
        }
        cursor.close()


        val dbWrite = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombres",       nombres)
            put("apellidos",     apellidos)
            put("email",         documento)
            put("password_hash", hashMD5(password))
            put("id_rol",        3)
            put("estado",        "Activo")
        }

        val resultado = dbWrite.insert(
            MovieTimeDatabaseHelper.TABLE_USUARIOS, null, values
        )

        if (resultado != -1L) {
            Toast.makeText(
                this,
                "¡Cuenta creada! Ya puedes iniciar sesión",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, SesionActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hashMD5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
