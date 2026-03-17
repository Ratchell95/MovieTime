package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class DetallePeliculaActivity : AppCompatActivity() {

    private var fechaSeleccionada: Date = Date()
    private val sdfDisplay  = SimpleDateFormat("EEE d MMM, yyyy", Locale("es","PE"))
    private val sdfDia      = SimpleDateFormat("d", Locale.getDefault())
    private val sdfMes      = SimpleDateFormat("MMMM", Locale("es","PE"))
    private val sdfNomCorto = SimpleDateFormat("EEE", Locale("es","PE"))

    private val fechaBtns = listOf(R.id.btnFecha1, R.id.btnFecha2, R.id.btnFecha3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_pelicula)

        val idPelicula = intent.getIntExtra("id_pelicula", 0)
        val titulo     = intent.getStringExtra("titulo") ?: ""
        val duracion   = intent.getIntExtra("duracion_min", 0)
        val clasif     = intent.getStringExtra("clasificacion") ?: ""
        val sinopsis   = intent.getStringExtra("sinopsis") ?: ""
        val imagenUrl  = intent.getStringExtra("imagen_url") ?: ""

        findViewById<TextView>(R.id.tvTitulo).text   = titulo.uppercase()
        findViewById<TextView>(R.id.tvSinopsis).text = sinopsis
        val h = duracion / 60; val m = duracion % 60
        findViewById<TextView>(R.id.tvDuracion).text = "$h hr $m min | $clasif"

        if (imagenUrl.isNotEmpty()) {
            Glide.with(this).load(imagenUrl).centerCrop()
                .into(findViewById(R.id.ivPoster))
        }

        val cal0 = Calendar.getInstance()
        val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
        val fechas = listOf(cal0.time, cal1.time, cal2.time)
        val labels = listOf("Hoy", "Mañana", sdfNomCorto.format(cal2.time))

        fechas.forEachIndexed { i, fecha ->
            val btnId = fechaBtns[i]
            val btn   = findViewById<LinearLayout>(btnId)
            // Día y mes
            val tvDia = when(i) { 0 -> R.id.tvDia1; 1 -> R.id.tvDia2; else -> R.id.tvDia3 }
            val tvMes = when(i) { 0 -> R.id.tvMes1; 1 -> R.id.tvMes2; else -> R.id.tvMes3 }
            val tvLbl = when(i) { 0 -> R.id.tvLabel1; 1 -> R.id.tvLabel2; else -> R.id.tvLabel3 }

            findViewById<TextView>(tvDia).text = sdfDia.format(fecha)
            findViewById<TextView>(tvMes).text = sdfMes.format(fecha)
            findViewById<TextView>(tvLbl).text = labels[i]

            btn.setOnClickListener {
                fechaSeleccionada = fecha
                // Reset todos a gris claro
                fechaBtns.forEach { id ->
                    findViewById<LinearLayout>(id)?.setBackgroundColor(
                        android.graphics.Color.parseColor("#EEEEEE"))
                    // Textos a gris
                    listOf(
                        when(id) {R.id.btnFecha1->R.id.tvDia1; R.id.btnFecha2->R.id.tvDia2; else->R.id.tvDia3},
                        when(id) {R.id.btnFecha1->R.id.tvMes1; R.id.btnFecha2->R.id.tvMes2; else->R.id.tvMes3},
                        when(id) {R.id.btnFecha1->R.id.tvLabel1; R.id.btnFecha2->R.id.tvLabel2; else->R.id.tvLabel3}
                    ).forEach { tvId -> findViewById<TextView>(tvId)?.setTextColor(
                        android.graphics.Color.parseColor("#777777")) }
                }
                // Activo: gris oscuro con texto blanco
                btn.setBackgroundColor(android.graphics.Color.parseColor("#333333"))
                listOf(tvDia, tvMes, tvLbl).forEach { tvId ->
                    findViewById<TextView>(tvId)?.setTextColor(android.graphics.Color.WHITE)
                }
            }
        }

        fechaSeleccionada = cal0.time
        findViewById<LinearLayout>(R.id.btnFecha1)?.setBackgroundColor(android.graphics.Color.parseColor("#333333"))
        listOf(R.id.tvDia1, R.id.tvMes1, R.id.tvLabel1).forEach {
            findViewById<TextView>(it)?.setTextColor(android.graphics.Color.WHITE)
        }

        setupHorario(R.id.btnHorarioChorrillos1, idPelicula, titulo, duracion, clasif, "CHORRILLOS",        "03:00 PM", "SALA: 01")
        setupHorario(R.id.btnHorarioChorrillos2, idPelicula, titulo, duracion, clasif, "CHORRILLOS",        "07:30 PM", "SALA: 02")
        setupHorario(R.id.btnHorarioIquitos1,    idPelicula, titulo, duracion, clasif, "MOVIETIME IQUITOS", "03:00 PM", "SALA: 04")
        setupHorario(R.id.btnHorarioIquitos2,    idPelicula, titulo, duracion, clasif, "MOVIETIME IQUITOS", "10:00 PM", "SALA: 05")
        setupHorario(R.id.btnHorarioVes1,        idPelicula, titulo, duracion, clasif, "VES1",              "04:00 PM", "SALA: 03")
    }

    private fun setupHorario(viewId: Int, idPelicula: Int, titulo: String,
                             duracion: Int, clasif: String, sede: String, hora: String, sala: String) {
        val btn = findViewById<LinearLayout>(viewId) ?: return
        btn.setOnClickListener {
            Intent(this, AsientosActivity::class.java).also {
                it.putExtra("id_pelicula",   idPelicula)
                it.putExtra("titulo",        titulo)
                it.putExtra("duracion_min",  duracion)
                it.putExtra("clasificacion", clasif)
                it.putExtra("sede",          sede)
                it.putExtra("hora",          hora)
                it.putExtra("sala",          sala)
                it.putExtra("fecha",         sdfDisplay.format(fechaSeleccionada))
                startActivity(it)
            }
        }
    }
}