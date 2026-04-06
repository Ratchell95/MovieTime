package com.idat.movietime

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.db.DatabaseHelper

enum class AsientoTipo { LIBRE, OCUPADA, ESPACIO, ETIQUETA_FILA, NUMERO_COL }

data class AsientoCelda(
    val fila:  String,
    val col:   Int,
    val tipo:  AsientoTipo,
    val label: String = ""
)

private const val VT_ASIENTO  = 0
private const val VT_ESPACIO  = 1
private const val VT_ETIQUETA = 2
private const val VT_NUM_COL  = 3

class AsientosActivity : AppCompatActivity() {

    private lateinit var tvTituloAsiento:   TextView
    private lateinit var tvDuracionAsiento: TextView
    private lateinit var tvSedeAsiento:     TextView
    private lateinit var tvFechaAsiento:    TextView
    private lateinit var tvHoraAsiento:     TextView
    private lateinit var tvSalaAsiento:     TextView
    private lateinit var tvCronometro:      TextView
    private lateinit var tvSeleccionadas:   TextView
    private lateinit var recyclerAsientos:  RecyclerView
    private lateinit var btnContinuar:      Button
    private lateinit var seatAdapter:       AsientoAdapter
    private lateinit var ivPosterAsiento:   ImageView

    private val butacasSeleccionadas = mutableListOf<String>()
    private var timer: CountDownTimer? = null

    private var idPeliculaExtra = 0
    private var tituloExtra     = ""
    private var duracionExtra   = 0
    private var clasificExtra   = ""
    private var sedeExtra       = ""
    private var horaExtra       = ""
    private var salaExtra       = ""
    private var fechaExtra      = ""
    private var drawableResExtra = 0
    private var idFuncionExtra   = 0

    private val filas = listOf("M","L","K","J","I","H","G","F","E","D","C","B","A")
    private val colsPorFila = mapOf(
        "M" to (1..17).toList(), "L" to (1..17).toList(),
        "K" to (1..17).toList(), "J" to (1..17).toList(),
        "I" to (1..17).toList(), "H" to (1..17).toList(),
        "G" to (1..17).toList(), "F" to (1..13).toList(),
        "E" to (1..13).toList(), "D" to (1..11).toList(),
        "C" to (1..9).toList(),  "B" to (1..7).toList(),
        "A" to (1..5).toList()
    )

    // Asientos ocupados permanentemente para pruebas
    private val ocupadas  = setOf("K8","K9","E4","E5","M13","M14")
    private val GRID_COLS = 18

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asientos)

        // Vincular vistas
        tvTituloAsiento   = findViewById(R.id.tvTituloAsiento)
        tvDuracionAsiento = findViewById(R.id.tvDuracionAsiento)
        tvSedeAsiento     = findViewById(R.id.tvSedeAsiento)
        tvFechaAsiento    = findViewById(R.id.tvFechaAsiento)
        tvHoraAsiento     = findViewById(R.id.tvHoraAsiento)
        tvSalaAsiento     = findViewById(R.id.tvSalaAsiento)
        tvCronometro      = findViewById(R.id.tvCronometro)
        tvSeleccionadas   = findViewById(R.id.tvSeleccionadas)
        recyclerAsientos  = findViewById(R.id.recyclerAsientos)
        btnContinuar      = findViewById(R.id.btnContinuar)
        ivPosterAsiento   = findViewById(R.id.ivPosterAsiento)

        // Recuperar extras
        idPeliculaExtra  = intent.getIntExtra("id_pelicula",   0)
        tituloExtra      = intent.getStringExtra("titulo")        ?: ""
        duracionExtra    = intent.getIntExtra("duracion_min",    0)
        clasificExtra    = intent.getStringExtra("clasificacion") ?: ""
        sedeExtra        = intent.getStringExtra("sede")          ?: "MOVIETIME IQUITOS"
        horaExtra        = intent.getStringExtra("hora")          ?: "10:00 PM"
        salaExtra        = intent.getStringExtra("sala")          ?: "SALA: 05"
        fechaExtra       = intent.getStringExtra("fecha")         ?: "Hoy"
        drawableResExtra = intent.getIntExtra("drawable_res",    0)
        idFuncionExtra   = intent.getIntExtra("id_funcion",      0)

        val dbHelper = DatabaseHelper(this)
        val bloqueadasBD = dbHelper.getButacasBloqueadasActivas(idFuncionExtra)
        val compradasBD = dbHelper.getButacasOcupadasPermanentes(idFuncionExtra)

        // Poblar info
        tvTituloAsiento.text   = "$tituloExtra (DOB)"
        tvDuracionAsiento.text = "${duracionExtra/60} hr ${duracionExtra%60} min | $clasificExtra"
        tvSedeAsiento.text     = sedeExtra
        tvFechaAsiento.text    = fechaExtra
        tvHoraAsiento.text     = horaExtra
        tvSalaAsiento.text     = salaExtra

        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }
        if (drawableResExtra != 0) {
            ivPosterAsiento.setImageResource(drawableResExtra)
        } else {
            // Si por alguna razón no llega, le pones un placeholder
            ivPosterAsiento.setImageResource(R.drawable.ic_pelicula_placeholder) // Asegúrate de tener esta imagen o usa mipmap/ic_launcher
        }
        iniciarCronometro()

        setupAsientos(bloqueadasBD, compradasBD)

        btnContinuar.setOnClickListener {
            // ✅ REGISTRAR BLOQUEO TEMPORAL EN LA BD ANTES DE SALIR
            dbHelper.bloquearButacasTemporales(butacasSeleccionadas, idFuncionExtra)

            timer?.cancel()
            val intent = Intent(this, EntradaActivity::class.java).apply {
                putExtra("butacas",           butacasSeleccionadas.joinToString(", "))
                putExtra("cantidad_entradas", butacasSeleccionadas.size)
                putExtra("titulo",            tituloExtra)
                putExtra("duracion_min",      duracionExtra)
                putExtra("clasificacion",     clasificExtra)
                putExtra("sede",              sedeExtra)
                putExtra("hora",              horaExtra)
                putExtra("sala",              salaExtra)
                putExtra("fecha",             fechaExtra)
                putExtra("drawable_res",      drawableResExtra)
                putExtra("id_pelicula",       idPeliculaExtra)
                putExtra("id_funcion",        idFuncionExtra)
            }
            startActivity(intent)
        }
    }

    private fun setupAsientos(bloqueadasBD: Set<String>, compradasBD: Set<String>) {
        // Sumamos: Las de prueba + las temporales + LAS COMPRADAS REALES
        val todasOcupadas = ocupadas + bloqueadasBD + compradasBD
        val maxAsientos = 17
        val celdas = mutableListOf<AsientoCelda>()

        // Encabezado números
        celdas.add(AsientoCelda("", 0, AsientoTipo.ESPACIO))
        for (n in 1..maxAsientos)
            celdas.add(AsientoCelda("", n, AsientoTipo.NUMERO_COL, label = n.toString()))

        // Filas de asientos
        for (fila in filas) {
            val cols        = colsPorFila[fila] ?: emptyList()
            val espaciosIzq = (maxAsientos - cols.size) / 2
            val espaciosDer = maxAsientos - cols.size - espaciosIzq

            celdas.add(AsientoCelda(fila, 0, AsientoTipo.ETIQUETA_FILA, label = fila))
            repeat(espaciosIzq) { celdas.add(AsientoCelda(fila, 0, AsientoTipo.ESPACIO)) }

            for (col in cols) {
                val codigo = "$fila$col"
                val tipo = if (todasOcupadas.contains(codigo)) AsientoTipo.OCUPADA else AsientoTipo.LIBRE
                celdas.add(AsientoCelda(fila, col, tipo))
            }
            repeat(espaciosDer) { celdas.add(AsientoCelda(fila, 0, AsientoTipo.ESPACIO)) }
        }

        seatAdapter = AsientoAdapter(celdas) { fila, col, seleccionada ->
            val codigo = "$fila$col"
            if (seleccionada) butacasSeleccionadas.add(codigo)
            else              butacasSeleccionadas.remove(codigo)
            actualizarPie()
        }
        recyclerAsientos.layoutManager = GridLayoutManager(this, GRID_COLS)
        recyclerAsientos.adapter = seatAdapter
    }

    private fun actualizarPie() {
        tvSeleccionadas.text = if (butacasSeleccionadas.isEmpty()) "Butacas: "
        else "Butacas: ${butacasSeleccionadas.joinToString(", ")}"
        btnContinuar.isEnabled = butacasSeleccionadas.isNotEmpty()
        btnContinuar.backgroundTintList = ColorStateList.valueOf(
            if (butacasSeleccionadas.isNotEmpty()) Color.parseColor("#1A1A2E")
            else Color.parseColor("#AAAAAA")
        )
    }

    private fun iniciarCronometro() {
        timer?.cancel()
        // ✅ CRONÓMETRO EXACTO DE 1 MINUTO (60,000 ms)
        timer = object : CountDownTimer(60000L, 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                tvCronometro.text = "⏱ Expira en: 00:%02d".format(s)
            }
            override fun onFinish() {
                Toast.makeText(this@AsientosActivity, "⏱ Tiempo agotado", Toast.LENGTH_LONG).show()
                finish()
            }
        }.start()
    }

    override fun onDestroy() { super.onDestroy(); timer?.cancel() }

    private fun circleDrawable(colorHex: String, strokeHex: String? = null) =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(colorHex))
            if (strokeHex != null) setStroke(2, Color.parseColor(strokeHex))
        }

    inner class AsientoAdapter(
        private val celdas: List<AsientoCelda>,
        private val onSel: (String, Int, Boolean) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val seleccionadas = mutableSetOf<String>()
        private var tooltipPos = -1

        inner class AsientoVH(v: View) : RecyclerView.ViewHolder(v) {
            val viewAsiento: View = v.findViewById(R.id.viewAsiento)
            val tvAsiento: TextView = v.findViewById(R.id.tvAsiento)
        }
        inner class EtiquetaVH(tv: TextView) : RecyclerView.ViewHolder(tv) { val label = tv }

        override fun getItemViewType(pos: Int) = when (celdas[pos].tipo) {
            AsientoTipo.LIBRE, AsientoTipo.OCUPADA -> VT_ASIENTO
            AsientoTipo.ESPACIO -> VT_ESPACIO
            AsientoTipo.ETIQUETA_FILA -> VT_ETIQUETA
            AsientoTipo.NUMERO_COL -> VT_NUM_COL
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
            return when (vt) {
                VT_ASIENTO -> AsientoVH(LayoutInflater.from(parent.context).inflate(R.layout.item_asiento, parent, false))
                VT_ETIQUETA, VT_NUM_COL -> {
                    val tv = TextView(parent.context).apply {
                        val size = (24 * resources.displayMetrics.density).toInt()
                        layoutParams = RecyclerView.LayoutParams(size, size)
                        gravity = Gravity.CENTER; textSize = 7f; setTextColor(Color.GRAY)
                    }
                    EtiquetaVH(tv)
                }
                else -> object : RecyclerView.ViewHolder(View(parent.context).apply {
                    val size = (24 * resources.displayMetrics.density).toInt()
                    layoutParams = RecyclerView.LayoutParams(size, size)
                }) {}
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            val c = celdas[pos]
            when (holder) {
                is EtiquetaVH -> holder.label.text = c.label
                is AsientoVH -> {
                    val codigo = "${c.fila}${c.col}"
                    holder.tvAsiento.text = if (tooltipPos == pos) codigo else ""

                    when {
                        c.tipo == AsientoTipo.OCUPADA -> {
                            holder.viewAsiento.background = circleDrawable("#BBBBBB")
                            holder.itemView.setOnClickListener(null)
                        }
                        seleccionadas.contains(codigo) -> {
                            holder.viewAsiento.background = circleDrawable("#4CAF50")
                            holder.itemView.setOnClickListener {
                                seleccionadas.remove(codigo); onSel(c.fila, c.col, false); notifyDataSetChanged()
                            }
                        }
                        else -> {
                            holder.viewAsiento.background = circleDrawable("#E8E8E8", "#CCCCCC")
                            holder.itemView.setOnClickListener {
                                seleccionadas.add(codigo); onSel(c.fila, c.col, true); notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
        override fun getItemCount() = celdas.size
    }
}