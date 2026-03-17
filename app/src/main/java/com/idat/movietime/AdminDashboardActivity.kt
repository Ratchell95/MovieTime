package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.idat.movietime.network.SessionManager

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvBienvenida:    TextView
    private lateinit var cardPeliculas:   LinearLayout
    private lateinit var cardFunciones:   LinearLayout
    private lateinit var cardProductos:   LinearLayout
    private lateinit var cardUsuarios:    LinearLayout
    private lateinit var cardReportes:    LinearLayout
    private lateinit var cardPromociones: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val session = SessionManager(this)

        tvBienvenida    = findViewById(R.id.tvBienvenida)
        cardPeliculas   = findViewById(R.id.cardPeliculas)
        cardFunciones   = findViewById(R.id.cardFunciones)
        cardProductos   = findViewById(R.id.cardProductos)
        cardUsuarios    = findViewById(R.id.cardUsuarios)
        cardReportes    = findViewById(R.id.cardReportes)
        cardPromociones = findViewById(R.id.cardPromociones)

        tvBienvenida.text = "Bienvenido, ${session.getNombres()}"


        cardPeliculas.setOnClickListener {
            // startActivity(Intent(this, GestionPeliculasActivity::class.java))
        }
        cardFunciones.setOnClickListener {
            // startActivity(Intent(this, GestionFuncionesActivity::class.java))
        }
        cardProductos.setOnClickListener {
            // startActivity(Intent(this, GestionProductosActivity::class.java))
        }
        cardUsuarios.setOnClickListener {
            // startActivity(Intent(this, GestionUsuariosActivity::class.java))
        }
        cardReportes.setOnClickListener {
            // startActivity(Intent(this, ReportesActivity::class.java))
        }
        cardPromociones.setOnClickListener {
            // startActivity(Intent(this, GestionPromocionesActivity::class.java))
        }
    }
}
