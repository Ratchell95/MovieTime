package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    private var drawableRes = 0

    private val drawableMap = mapOf(
        1 to R.drawable.ic_pelicula1,
        2 to R.drawable.ic_pelicula2,
        3 to R.drawable.ic_pelicula3,
        4 to R.drawable.ic_pelicula4,
        5 to R.drawable.ic_pelicula5,
        6 to R.drawable.ic_pelicula6,
        7 to R.drawable.ic_pelicula7,
        8 to R.drawable.ic_pelicula8
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_pelicula)


        findViewById<View>(R.id.btnAtras)?.setOnClickListener {
            finish()
        }

        val idPelicula  = intent.getIntExtra("id_pelicula", 0)
        val titulo      = intent.getStringExtra("titulo")        ?: ""
        val duracion    = intent.getIntExtra("duracion_min",     0)
        val clasif      = intent.getStringExtra("clasificacion") ?: ""
        val sinopsis    = intent.getStringExtra("sinopsis")      ?: ""
        val imagenUrl   = intent.getStringExtra("imagen_url")    ?: ""
        drawableRes     = intent.getIntExtra("drawable_res",     0)

        // SOLUCIÓN 2: Uso del operador seguro "?." para asignar textos.
        // Si el elemento no existe en el XML, simplemente se ignora y NO rompe la app.
        findViewById<TextView>(R.id.tvTitulo)?.text   = titulo.uppercase()
        findViewById<TextView>(R.id.tvSinopsis)?.text = sinopsis
        val h = duracion / 60; val m = duracion % 60
        findViewById<TextView>(R.id.tvDuracion)?.text = "$h hr $m min | $clasif"

        val ivPoster = findViewById<ImageView>(R.id.ivPoster)
        ivPoster?.scaleType = ImageView.ScaleType.CENTER_CROP

        val resDirecto = if (drawableRes != 0) drawableRes else drawableMap[idPelicula] ?: 0


        ivPoster?.let { poster ->
            when {
                resDirecto != 0 -> {
                    poster.setImageResource(resDirecto)
                }
                imagenUrl.isNotEmpty() -> {
                    Glide.with(this)
                        .load(imagenUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_pelicula_placeholder)
                        .error(R.drawable.ic_pelicula_placeholder)
                        .into(poster)
                }
                else -> {
                    poster.setImageResource(R.drawable.ic_pelicula_placeholder)
                }
            }
        }

        val cal0 = Calendar.getInstance()
        val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
        val fechas = listOf(cal0.time, cal1.time, cal2.time)
        val labels = listOf("Hoy", "Mañana", sdfNomCorto.format(cal2.time))

        fechas.forEachIndexed { i, fecha ->
            val btnId = fechaBtns[i]
            val btn   = findViewById<LinearLayout>(btnId)
            val tvDia = when(i) { 0 -> R.id.tvDia1; 1 -> R.id.tvDia2; else -> R.id.tvDia3 }
            val tvMes = when(i) { 0 -> R.id.tvMes1; 1 -> R.id.tvMes2; else -> R.id.tvMes3 }
            val tvLbl = when(i) { 0 -> R.id.tvLabel1; 1 -> R.id.tvLabel2; else -> R.id.tvLabel3 }

            findViewById<TextView>(tvDia)?.text = sdfDia.format(fecha)
            findViewById<TextView>(tvMes)?.text = sdfMes.format(fecha)
            findViewById<TextView>(tvLbl)?.text = labels[i]

            btn?.setOnClickListener {
                fechaSeleccionada = fecha
                fechaBtns.forEach { id ->
                    findViewById<LinearLayout>(id)?.setBackgroundColor(
                        android.graphics.Color.parseColor("#EEEEEE"))
                    listOf(
                        when(id) {R.id.btnFecha1->R.id.tvDia1; R.id.btnFecha2->R.id.tvDia2; else->R.id.tvDia3},
                        when(id) {R.id.btnFecha1->R.id.tvMes1; R.id.btnFecha2->R.id.tvMes2; else->R.id.tvMes3},
                        when(id) {R.id.btnFecha1->R.id.tvLabel1; R.id.btnFecha2->R.id.tvLabel2; else->R.id.tvLabel3}
                    ).forEach { tvId -> findViewById<TextView>(tvId)?.setTextColor(
                        android.graphics.Color.parseColor("#777777")) }
                }
                btn.setBackgroundColor(android.graphics.Color.parseColor("#333333"))
                listOf(tvDia, tvMes, tvLbl).forEach { tvId ->
                    findViewById<TextView>(tvId)?.setTextColor(android.graphics.Color.WHITE)
                }
            }
        }

        fechaSeleccionada = cal0.time
        findViewById<LinearLayout>(R.id.btnFecha1)?.setBackgroundColor(
            android.graphics.Color.parseColor("#333333"))
        listOf(R.id.tvDia1, R.id.tvMes1, R.id.tvLabel1).forEach {
            findViewById<TextView>(it)?.setTextColor(android.graphics.Color.WHITE)
        }

        setupHorario(R.id.btnHorarioChorrillos1, idPelicula, titulo, duracion, clasif, "CHORRILLOS",        "03:00 PM", "SALA: 01")
        setupHorario(R.id.btnHorarioChorrillos2, idPelicula, titulo, duracion, clasif, "CHORRILLOS",        "07:30 PM", "SALA: 02")
        setupHorario(R.id.btnHorarioIquitos1,    idPelicula, titulo, duracion, clasif, "MOVIETIME IQUITOS", "03:00 PM", "SALA: 04")
        setupHorario(R.id.btnHorarioIquitos2,    idPelicula, titulo, duracion, clasif, "MOVIETIME IQUITOS", "10:00 PM", "SALA: 05")
        setupHorario(R.id.btnHorarioVes1,        idPelicula, titulo, duracion, clasif, "VES1",              "04:00 PM", "SALA: 03")
    }

    private fun setupHorario(
        viewId: Int, idPelicula: Int, titulo: String,
        duracion: Int, clasif: String, sede: String, hora: String, sala: String
    ) {
        val btn = findViewById<LinearLayout>(viewId) ?: return
        btn.setOnClickListener {

            val res = if (drawableRes != 0) drawableRes else drawableMap[idPelicula] ?: 0
            Intent(this, AsientosActivity::class.java).also {
                it.putExtra("id_pelicula",   idPelicula)
                it.putExtra("titulo",        titulo)
                it.putExtra("duracion_min",  duracion)
                it.putExtra("clasificacion", clasif)
                it.putExtra("sede",          sede)
                it.putExtra("hora",          hora)
                it.putExtra("sala",          sala)
                it.putExtra("fecha",         sdfDisplay.format(fechaSeleccionada))
                it.putExtra("drawable_res",  res)
                startActivity(it)
            }
        }
    }
}