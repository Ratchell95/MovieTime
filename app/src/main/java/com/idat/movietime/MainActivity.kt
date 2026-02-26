package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnIniciarSesion: Button
    private lateinit var btnInvitado: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ✓ coincide con el XML

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnInvitado = findViewById(R.id.btnInvitado)

        btnIniciarSesion.setOnClickListener {
            // Ir a la pantalla de login (activity_sesion.xml)
            val intent = Intent(this, SesionActivity::class.java)
            startActivity(intent)
        }

        btnInvitado.setOnClickListener {
            // Ir directo a películas sin login
            val intent = Intent(this, PeliculasActivity::class.java)
            startActivity(intent)
        }
    }
}