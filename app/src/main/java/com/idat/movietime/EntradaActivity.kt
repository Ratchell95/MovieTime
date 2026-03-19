package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EntradaActivity : AppCompatActivity() {

    private lateinit var tvCronometro:  TextView
    private lateinit var tvTotal:       TextView
    private lateinit var btnSiguiente:  Button
    private lateinit var tvSubAdulto:   TextView
    private lateinit var tvSubInfantil: TextView
    private lateinit var tvSubMayor:    TextView

    private var cantAdulto   = 0
    private var cantInfantil = 0
    private var cantMayor    = 0

    private val precioAdulto   = 18.0
    private val precioInfantil = 15.0
    private val precioMayor    = 15.0

    private var cantidadButacas = 1
    private var timer: CountDownTimer? = null
    private var descuentoPromo = 0.0

    private var butacas  = ""
    private var titulo   = ""
    private var duracion = 0
    private var clasif   = ""
    private var sede     = ""
    private var hora     = ""
    private var sala     = ""
    private var fecha    = ""
    private var idFuncion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrada)

        butacas         = intent.getStringExtra("butacas")           ?: ""
        cantidadButacas = intent.getIntExtra("cantidad_entradas",     1)
            .let { if (it <= 0) 1 else it }
        titulo          = intent.getStringExtra("titulo")            ?: ""
        duracion        = intent.getIntExtra("duracion_min",          0)
        clasif          = intent.getStringExtra("clasificacion")      ?: ""
        sede            = intent.getStringExtra("sede")              ?: ""
        hora            = intent.getStringExtra("hora")              ?: ""
        sala            = intent.getStringExtra("sala")              ?: ""
        fecha           = intent.getStringExtra("fecha")             ?: ""
        idFuncion       = intent.getIntExtra("id_funcion",            0)

        tvCronometro  = findViewById(R.id.tvCronometro)
        tvTotal       = findViewById(R.id.tvTotal)
        btnSiguiente  = findViewById(R.id.btnSiguiente)
        tvSubAdulto   = findViewById(R.id.tvSubAdulto)
        tvSubInfantil = findViewById(R.id.tvSubInfantil)
        tvSubMayor    = findViewById(R.id.tvSubMayor)

        findViewById<TextView>(R.id.tvTituloEntrada).text    = titulo
        findViewById<TextView>(R.id.tvDuracionEntrada).text  = "${duracion/60} hr ${duracion%60} min | $clasif"
        findViewById<TextView>(R.id.tvSedeEntrada).text      = sede
        findViewById<TextView>(R.id.tvFechaEntrada).text     = fecha
        findViewById<TextView>(R.id.tvHoraEntrada).text      = hora
        findViewById<TextView>(R.id.tvSalaEntrada).text      = sala


        val tvIndicacion = findViewById<TextView>(R.id.tvTotal)
        tvIndicacion?.text = "Asigna ${cantidadButacas} entrada(s) — Total: S/ 0.00"

        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }

        // ── Validar código promo Yape ────────────────────────────────
        findViewById<Button>(R.id.btnValidarPromo)?.setOnClickListener {
            val codigo = findViewById<com.google.android.material.textfield.TextInputEditText>(
                R.id.etCodigoPromo)?.text?.toString()?.trim() ?: ""
            if (codigo.isEmpty()) {
                Toast.makeText(this, "Ingresa un código de promoción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val totalActual = cantAdulto * precioAdulto + cantInfantil * precioInfantil + cantMayor * precioMayor
            val dbHelper = com.idat.movietime.db.DatabaseHelper(this)
            val descuento = dbHelper.aplicarPromocion(codigo, totalActual)
            dbHelper.close()
            if (descuento < 0) {
                Toast.makeText(this, "Código inválido o expirado", Toast.LENGTH_SHORT).show()
            } else {
                descuentoPromo = descuento
                Toast.makeText(this, "Promo aplicada: -S/${"%.2f".format(descuento)}", Toast.LENGTH_SHORT).show()
                actualizarTotal()
            }
        }
        setupContadores()
        iniciarCronometro()
        actualizarTotal()
        btnSiguiente.setOnClickListener {
            val totalAsignado = cantAdulto + cantInfantil + cantMayor
            if (totalAsignado == 0) {
                Toast.makeText(this, "Selecciona al menos una entrada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (totalAsignado != cantidadButacas) {
                Toast.makeText(this,
                    "Debes asignar exactamente $cantidadButacas entrada(s) para tus butacas",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            timer?.cancel()
            val subtotal = cantAdulto * precioAdulto + cantInfantil * precioInfantil + cantMayor * precioMayor
            val total    = maxOf(0.0, subtotal - descuentoPromo)
            startActivity(Intent(this, ConfiteriaActivity::class.java).apply {
                putExtra("butacas",           butacas)
                putExtra("cantidad_entradas", cantidadButacas)
                putExtra("total_entradas",    total)
                putExtra("descuento_promo",   descuentoPromo)
                putExtra("cant_adulto",       cantAdulto)
                putExtra("cant_infantil",     cantInfantil)
                putExtra("cant_mayor",        cantMayor)
                putExtra("titulo",            titulo)
                putExtra("sede",              sede)
                putExtra("hora",              hora)
                putExtra("sala",              sala)
                putExtra("fecha",             fecha)
                putExtra("duracion_min",      duracion)
                putExtra("clasificacion",     clasif)
                putExtra("id_funcion",        idFuncion)
            })
        }
    }

    private fun setupContadores() {
        setupContador(R.id.btnMenosAdulto,   R.id.btnMasAdulto,   R.id.tvCantAdulto,
            getter = { cantAdulto },   setter = { cantAdulto = it })
        setupContador(R.id.btnMenosInfantil, R.id.btnMasInfantil, R.id.tvCantInfantil,
            getter = { cantInfantil }, setter = { cantInfantil = it })
        setupContador(R.id.btnMenosMayor,    R.id.btnMasMayor,    R.id.tvCantMayor,
            getter = { cantMayor },    setter = { cantMayor = it })
    }

    private fun setupContador(
        menosId: Int, masId: Int, cantId: Int,
        getter: () -> Int, setter: (Int) -> Unit
    ) {
        val tvCant = findViewById<TextView>(cantId)
        findViewById<Button>(menosId)?.setOnClickListener {
            if (getter() > 0) {
                setter(getter() - 1)
                tvCant?.text = getter().toString()
                actualizarTotal()
            }
        }
        findViewById<Button>(masId)?.setOnClickListener {
            val totalActual = cantAdulto + cantInfantil + cantMayor
            if (totalActual < cantidadButacas) {
                setter(getter() + 1)
                tvCant?.text = getter().toString()
                actualizarTotal()
            } else {
                Toast.makeText(this, "Máximo $cantidadButacas entrada(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarTotal() {
        val subAdulto   = cantAdulto   * precioAdulto
        val subInfantil = cantInfantil * precioInfantil
        val subMayor    = cantMayor    * precioMayor
        val subtotal    = subAdulto + subInfantil + subMayor
        val total       = maxOf(0.0, subtotal - descuentoPromo)

        tvSubAdulto.text   = "= S/${"%.2f".format(subAdulto)}"
        tvSubInfantil.text = "= S/${"%.2f".format(subInfantil)}"
        tvSubMayor.text    = "= S/${"%.2f".format(subMayor)}"

        tvTotal.text = if (descuentoPromo > 0)
            "Asigna $cantidadButacas entrada(s) — Total: S/ ${"%.2f".format(total)} (-S/${"%.2f".format(descuentoPromo)})"
        else
            "Asigna $cantidadButacas entrada(s) — Total: S/ ${"%.2f".format(total)}"

        val asignadas = cantAdulto + cantInfantil + cantMayor
        btnSiguiente.isEnabled = asignadas == cantidadButacas
        btnSiguiente.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (btnSiguiente.isEnabled) android.graphics.Color.parseColor("#1A1A2E")
            else android.graphics.Color.parseColor("#AAAAAA")
        )
    }

    private fun iniciarCronometro() {
        tvCronometro.visibility = android.view.View.VISIBLE
        timer = object : CountDownTimer(10 * 60 * 1000L, 1000) {
            override fun onTick(ms: Long) {
                val m = ms / 60000; val s = (ms % 60000) / 1000
                tvCronometro.text = "⏱ %02d:%02d".format(m, s)
            }
            override fun onFinish() {
                Toast.makeText(this@EntradaActivity, "⏱ Tiempo agotado — los asientos se liberaron", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@EntradaActivity, PeliculasActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
        }.start()
    }

    override fun onDestroy() { super.onDestroy(); timer?.cancel() }
}