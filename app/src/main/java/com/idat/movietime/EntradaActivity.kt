package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class EntradaActivity : AppCompatActivity() {

    private lateinit var tvCronometro: TextView
    private lateinit var tvTotal:      TextView
    private lateinit var btnSiguiente: Button


    private var cantAdulto   = 0
    private var cantInfantil = 0
    private var cantMayor    = 0

    private val precioAdulto   = 18.0
    private val precioInfantil = 15.0
    private val precioMayor    = 15.0

    private var cantidadButacas = 0
    private var timer: CountDownTimer? = null

    private var butacas   = ""
    private var titulo    = ""
    private var duracion  = 0
    private var clasif    = ""
    private var sede      = ""
    private var hora      = ""
    private var sala      = ""
    private var fecha     = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrada)

        butacas          = intent.getStringExtra("butacas")           ?: ""
        cantidadButacas  = intent.getIntExtra("cantidad_entradas", 1)
        titulo           = intent.getStringExtra("titulo")            ?: ""
        duracion         = intent.getIntExtra("duracion_min", 0)
        clasif           = intent.getStringExtra("clasificacion")     ?: ""
        sede             = intent.getStringExtra("sede")              ?: ""
        hora             = intent.getStringExtra("hora")              ?: ""
        sala             = intent.getStringExtra("sala")              ?: ""
        fecha            = intent.getStringExtra("fecha")             ?: ""

        tvCronometro = findViewById(R.id.tvCronometro)
        tvTotal      = findViewById(R.id.tvTotal)
        btnSiguiente = findViewById(R.id.btnSiguiente)

        findViewById<TextView>(R.id.tvTituloEntrada).text    = titulo
        findViewById<TextView>(R.id.tvDuracionEntrada).text  = "${duracion/60} hr ${duracion%60} min | $clasif"
        findViewById<TextView>(R.id.tvSedeEntrada).text      = sede
        findViewById<TextView>(R.id.tvFechaEntrada).text     = fecha
        findViewById<TextView>(R.id.tvHoraEntrada).text      = hora
        findViewById<TextView>(R.id.tvSalaEntrada).text      = sala

        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }

        setupContadores()
        iniciarCronometro()
        actualizarTotal()

        btnSiguiente.setOnClickListener {
            val total = cantAdulto * precioAdulto + cantInfantil * precioInfantil + cantMayor * precioMayor
            if (cantAdulto + cantInfantil + cantMayor == 0) {
                Toast.makeText(this, "Selecciona al menos una entrada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantAdulto + cantInfantil + cantMayor != cantidadButacas) {
                Toast.makeText(this, "Debes asignar exactamente ${cantidadButacas} entradas para tus butacas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            timer?.cancel()
            Intent(this, ConfiteriaActivity::class.java).also { i ->
                i.putExtra("butacas",           butacas)
                i.putExtra("cantidad_entradas", cantidadButacas)
                i.putExtra("total_entradas",    total)
                i.putExtra("cant_adulto",       cantAdulto)
                i.putExtra("cant_infantil",     cantInfantil)
                i.putExtra("cant_mayor",        cantMayor)
                i.putExtra("titulo",            titulo)
                i.putExtra("sede",              sede)
                i.putExtra("hora",              hora)
                i.putExtra("sala",              sala)
                i.putExtra("fecha",             fecha)
                startActivity(i)
            }
        }
    }

    private fun setupContadores() {
        // ADULTO
        setupContador(
            R.id.btnMenosAdulto, R.id.btnMasAdulto, R.id.tvCantAdulto,
            getter = { cantAdulto }, setter = { cantAdulto = it }
        )
        // INFANTIL
        setupContador(
            R.id.btnMenosInfantil, R.id.btnMasInfantil, R.id.tvCantInfantil,
            getter = { cantInfantil }, setter = { cantInfantil = it }
        )
        // ADULTO MAYOR
        setupContador(
            R.id.btnMenosMayor, R.id.btnMasMayor, R.id.tvCantMayor,
            getter = { cantMayor }, setter = { cantMayor = it }
        )
    }

    private fun setupContador(
        menosId: Int, masId: Int, cantId: Int,
        getter: () -> Int, setter: (Int) -> Unit
    ) {
        val tvCant = findViewById<TextView>(cantId)
        findViewById<View>(menosId).setOnClickListener {
            if (getter() > 0) {
                setter(getter() - 1)
                tvCant.text = getter().toString()
                actualizarTotal()
            }
        }
        findViewById<View>(masId).setOnClickListener {
            val totalActual = cantAdulto + cantInfantil + cantMayor
            if (totalActual < cantidadButacas) {
                setter(getter() + 1)
                tvCant.text = getter().toString()
                actualizarTotal()
            } else {
                Toast.makeText(this, "Máximo ${cantidadButacas} entradas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarTotal() {
        val total = cantAdulto * precioAdulto + cantInfantil * precioInfantil + cantMayor * precioMayor
        tvTotal.text = "Total: S/ ${"%.2f".format(total)}"
        btnSiguiente.isEnabled = (cantAdulto + cantInfantil + cantMayor) == cantidadButacas
        btnSiguiente.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (btnSiguiente.isEnabled) android.graphics.Color.parseColor("#1A1A2E")
            else android.graphics.Color.parseColor("#AAAAAA")
        )
    }

    private fun iniciarCronometro() {
        timer = object : CountDownTimer(10 * 60 * 1000L, 1000) {
            override fun onTick(ms: Long) {
                val m = ms / 60000; val s = (ms % 60000) / 1000
                tvCronometro.text = "⏱ %02d:%02d".format(m, s)
            }
            override fun onFinish() {
                Toast.makeText(this@EntradaActivity, "Tiempo agotado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }

    override fun onDestroy() { super.onDestroy(); timer?.cancel() }
}
