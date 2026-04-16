package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.SessionManager
import java.util.*

class PagoActivity : AppCompatActivity() {

    private lateinit var tvCronometro:    TextView
    private lateinit var btnBoleta:       Button
    private lateinit var btnFactura:      Button
    private lateinit var layoutFactura:   View
    private lateinit var rbTarjeta:       RadioButton
    private lateinit var rbYape:          RadioButton
    private lateinit var layoutTarjeta:   View
    private lateinit var layoutYape:      View
    private lateinit var etNumTarjeta:    EditText
    private lateinit var etVencimiento:   EditText
    private lateinit var etCvv:           EditText
    private lateinit var etNombreTarjeta: EditText
    private lateinit var etCelularYape:   EditText
    private lateinit var switchTerminos:  SwitchCompat
    private lateinit var tvTotal:         TextView
    private lateinit var btnPagar:        Button

    private var timer: CountDownTimer? = null

    private var tipoComprobante = "Boleta"
    private var metodoPago      = "Tarjeta"
    private var butacas         = ""
    private var tituloPelicula  = ""
    private var idPelicula      = 0
    private var granTotal       = 0.0
    private var subtotal        = 0.0
    private var descuento       = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        vincularVistas()
        setupComprobante()
        setupMetodoPago()
        recuperarDatosIntent()
        iniciarCronometro()
        configurarSaltoAutomaticoPin()

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }

        btnPagar.setOnClickListener {
            procesarPagoLocal()
        }
    }

    private fun vincularVistas() {
        tvCronometro    = findViewById(R.id.tvCronometro)
        btnBoleta       = findViewById(R.id.btnBoleta)
        btnFactura      = findViewById(R.id.btnFactura)
        layoutFactura   = findViewById(R.id.layoutFactura)
        rbTarjeta       = findViewById(R.id.rbTarjeta)
        rbYape          = findViewById(R.id.rbYape)
        layoutTarjeta   = findViewById(R.id.layoutTarjeta)
        layoutYape      = findViewById(R.id.layoutYape)
        switchTerminos  = findViewById(R.id.switchTerminos)
        tvTotal         = findViewById(R.id.tvTotal)
        btnPagar        = findViewById(R.id.btnPagar)

        etNumTarjeta    = findViewById(R.id.etNumTarjeta)
        etCelularYape   = findViewById(R.id.etCelularYape)
    }

    private fun recuperarDatosIntent() {
        butacas        = intent.getStringExtra("butacas") ?: ""
        tituloPelicula = intent.getStringExtra("titulo") ?: "Película Desconocida"
        idPelicula     = intent.getIntExtra("id_pelicula", 0)
        granTotal      = intent.getDoubleExtra("gran_total", 0.0)
        subtotal       = intent.getDoubleExtra("total_entradas", 0.0)
        descuento      = intent.getDoubleExtra("descuento", 0.0)

        tvTotal.text = "Pagar S/ ${"%.2f".format(granTotal)}"
        btnPagar.text = "Pagar S/ ${"%.2f".format(granTotal)}"
    }

    private fun setupComprobante() {
        btnBoleta.setOnClickListener {
            tipoComprobante = "Boleta"
            layoutFactura.visibility = View.GONE
            btnBoleta.alpha = 1.0f
            btnFactura.alpha = 0.5f
        }
        btnFactura.setOnClickListener {
            tipoComprobante = "Factura"
            layoutFactura.visibility = View.VISIBLE
            btnFactura.alpha = 1.0f
            btnBoleta.alpha = 0.5f
        }
    }

    private fun setupMetodoPago() {
        rbTarjeta.setOnClickListener {
            metodoPago = "Tarjeta"
            layoutTarjeta.visibility = View.VISIBLE
            layoutYape.visibility = View.GONE
        }
        rbYape.setOnClickListener {
            metodoPago = "Yape"
            layoutYape.visibility = View.VISIBLE
            layoutTarjeta.visibility = View.GONE
        }
    }

    private fun iniciarCronometro() {
        timer = object : CountDownTimer(600000L, 1000) {
            override fun onTick(ms: Long) {
                val m = ms / 60000; val s = (ms % 60000) / 1000
                tvCronometro.text = "⏱ %02d:%02d".format(m, s)
            }
            override fun onFinish() {
                Toast.makeText(this@PagoActivity, "Tiempo agotado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }

    private fun procesarPagoLocal() {
        if (!switchTerminos.isChecked) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
            return
        }
        if (metodoPago == "Tarjeta" && etNumTarjeta.text.isBlank()) {
            Toast.makeText(this, "Ingresa el número de tu tarjeta", Toast.LENGTH_SHORT).show()
            return
        }
        if (metodoPago == "Yape" && etCelularYape.text.isBlank()) {
            Toast.makeText(this, "Ingresa el celular asociado a Yape", Toast.LENGTH_SHORT).show()
            return
        }

        btnPagar.isEnabled = false
        btnPagar.text = "Procesando pago..."

        val dbHelper = DatabaseHelper(this)
        var idClienteActual = SessionManager(this).getIdUsuario()
        val idFuncion = intent.getIntExtra("id_funcion", 1)

        val dbW = dbHelper.writableDatabase

        try {
            dbW.execSQL("DROP TRIGGER IF EXISTS trg_validar_cliente_venta")
        } catch (e: Exception) {
            // Se ignora si no existe
        }

        var idClienteSeguro = -1
        val c = dbW.rawQuery("SELECT id_usuario FROM usuarios WHERE id_rol = 5 LIMIT 1", null)
        if (c.moveToFirst()) {
            idClienteSeguro = c.getInt(0)
        } else {
            val cv = android.content.ContentValues().apply {
                put("nombres", "Cliente App")
                put("email", "cliente@app.com")
                put("id_rol", 5)
                put("estado", "Activo")
            }
            idClienteSeguro = dbW.insert("usuarios", null, cv).toInt()
        }
        c.close()

        if (idClienteActual <= 0) {
            idClienteActual = idClienteSeguro
        }
        val db = dbHelper.readableDatabase
        val idsButacas = mutableListOf<Int>()
        val codigosQR = mutableListOf<String>()
        val listaButacas = butacas.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val qrBase = "QR-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

        for (butacaString in listaButacas) {
            val fila = butacaString.take(1)
            val numero = butacaString.drop(1).toIntOrNull() ?: 0

            val cursor = db.rawQuery(
                "SELECT b.id_butaca FROM butacas b INNER JOIN funciones f ON b.id_sala = f.id_sala WHERE f.id_funcion = ? AND b.fila = ? AND b.numero = ?",
                arrayOf(idFuncion.toString(), fila, numero.toString())
            )
            if (cursor.moveToFirst()) {
                idsButacas.add(cursor.getInt(0))
                codigosQR.add("$qrBase-$butacaString")
            }
            cursor.close()
        }

        val precioUnitario = if (listaButacas.isNotEmpty()) subtotal / listaButacas.size else subtotal

        var idVenta = dbHelper.insertarVentaCompleta(
            idClienteActual,
            idFuncion,
            idsButacas,
            precioUnitario,
            codigosQR,
            null,
            subtotal,
            descuento,
            granTotal,
            tipoComprobante,
            metodoPago,
            null
        )


        if (idVenta == -1L && idClienteActual != idClienteSeguro) {
            idVenta = dbHelper.insertarVentaCompleta(
                idClienteSeguro,
                idFuncion,
                idsButacas,
                precioUnitario,
                codigosQR,
                null,
                subtotal,
                descuento,
                granTotal,
                tipoComprobante,
                metodoPago,
                null
            )
        }

        if (idVenta != -1L) {
            timer?.cancel()
            Toast.makeText(this, "¡Compra Exitosa!", Toast.LENGTH_LONG).show()
            val intentExito = Intent(this, DetalleCompraActivity::class.java).apply {
                putExtra("id_venta",          idVenta.toInt())
                putExtra("titulo",            intent.getStringExtra("titulo")         ?: "")
                putExtra("sala",              intent.getStringExtra("sala")           ?: "")
                putExtra("butacas",           intent.getStringExtra("butacas")        ?: "")
                putExtra("fecha",             intent.getStringExtra("fecha")          ?: "")
                putExtra("hora",              intent.getStringExtra("hora")           ?: "")
                putExtra("metodo_pago",       metodoPago)
                putExtra("tipo_comprobante",  tipoComprobante)
                putExtra("gran_total",        granTotal)
                putExtra("total_entradas",    subtotal)
                putExtra("descuento",         descuento)
            }
            startActivity(intentExito)
            finish()
        } else {
            Toast.makeText(this, "Error crítico de BD. Comuníquese con soporte.", Toast.LENGTH_LONG).show()
            btnPagar.isEnabled = true
            btnPagar.text = "Pagar S/ ${"%.2f".format(granTotal)}"
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    private fun configurarSaltoAutomaticoPin() {
        val pins = arrayOf(
            findViewById<EditText>(R.id.pin1),
            findViewById<EditText>(R.id.pin2),
            findViewById<EditText>(R.id.pin3),
            findViewById<EditText>(R.id.pin4),
            findViewById<EditText>(R.id.pin5),
            findViewById<EditText>(R.id.pin6)
        )

        for (i in pins.indices) {
            pins[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < pins.size - 1) {
                        pins[i + 1].requestFocus()
                    }
                }
            })

            pins[i].setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (pins[i].text.isEmpty() && i > 0) {
                        pins[i - 1].requestFocus()
                        pins[i - 1].setText("")
                    }
                }
                false
            }
        }
    }

}