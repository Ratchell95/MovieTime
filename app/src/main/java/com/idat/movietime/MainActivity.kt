package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnIniciarSesion: Button
    private lateinit var btnInvitado:      Button
    private lateinit var tvRegistro:       TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnInvitado      = findViewById(R.id.btnInvitado)
        tvRegistro       = findViewById(R.id.tvRegistro)

        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, SesionActivity::class.java))
        }

        btnInvitado.setOnClickListener {
            startActivity(Intent(this, PeliculasActivity::class.java))
        }

        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }
}