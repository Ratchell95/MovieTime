package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.VentaDetalle

class PagoActivity : AppCompatActivity() {

    private lateinit var tvCronometro:    TextView
    private lateinit var btnBoleta:       Button
    private lateinit var btnFactura:      Button
    private lateinit var layoutFactura:   View
    private lateinit var etNumeroDoc:     EditText
    private lateinit var etNombre:        EditText
    private lateinit var etApellido:      EditText
    private lateinit var etEmail:         EditText
    private lateinit var etRuc:           EditText
    private lateinit var etRazonSocial:   EditText
    private lateinit var etDireccion:     EditText
    private lateinit var etTelefono:      EditText
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

    private var tipoComprobante = "Boleta"
    private var metodoPago      = "Tarjeta"
    private var timer:           CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        bindViews()

        val butacas          = intent.getStringExtra("butacas")          ?: ""
        val cantidadEntradas = intent.getIntExtra("cantidad_entradas", 1)
        val titulo           = intent.getStringExtra("titulo")           ?: ""
        val sede             = intent.getStringExtra("sede")             ?: ""
        val sala             = intent.getStringExtra("sala")             ?: sede
        val hora             = intent.getStringExtra("hora")             ?: ""
        val fecha            = intent.getStringExtra("fecha")            ?: ""
        val totalConfiteria  = intent.getDoubleExtra("total_confiteria", 0.0)
        val totalEntradas    = intent.getDoubleExtra("total_entradas", cantidadEntradas * 18.0)
        val totalFinal       = totalEntradas + totalConfiteria

        val idFuncion        = intent.getIntExtra("id_funcion", -1)
        val idButacasStr     = intent.getStringExtra("ids_butacas") ?: ""
        val codigosQRStr     = intent.getStringExtra("codigos_qr")  ?: ""
        val idPromocion      = intent.getIntExtra("id_promocion", -1).takeIf { it != -1 }
        val descuento        = intent.getDoubleExtra("descuento", 0.0)
        val subtotal         = totalFinal + descuento

        val confiteriaStr    = intent.getStringExtra("confiteria_items") ?: ""

        tvTotal.text = "Total:  S/ ${"%.2f".format(totalFinal)}"

        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }
        iniciarCronometro()
        setupComprobante()
        setupMetodoPago()

        btnPagar.setOnClickListener {
            procesarPago(
                butacas, cantidadEntradas, titulo, sede, sala, hora, fecha,
                totalFinal, subtotal, descuento,
                idFuncion, idButacasStr, codigosQRStr,
                confiteriaStr, idPromocion
            )
        }
    }

    private fun procesarPago(
        butacas: String, cantidad: Int, titulo: String,
        sede: String, sala: String, hora: String, fecha: String,
        total: Double, subtotal: Double, descuento: Double,
        idFuncion: Int, idButacasStr: String, codigosQRStr: String,
        confiteriaStr: String, idPromocion: Int?
    ) {
        if (!switchTerminos.isChecked) {
            Toast.makeText(this, "Acepta los Términos y Condiciones para continuar", Toast.LENGTH_SHORT).show()
            return
        }
        if (etNombre.text.isNullOrBlank())   { etNombre.error   = "Requerido"; return }
        if (etApellido.text.isNullOrBlank()) { etApellido.error = "Requerido"; return }
        if (etEmail.text.isNullOrBlank())    { etEmail.error    = "Requerido"; return }

        if (tipoComprobante == "Factura") {
            if (etRuc.text.isNullOrBlank())         { etRuc.error         = "Requerido"; return }
            if (etRazonSocial.text.isNullOrBlank()) { etRazonSocial.error = "Requerido"; return }
        }

        when (metodoPago) {
            "Tarjeta" -> {
                if (etNumTarjeta.text.isNullOrBlank())    { etNumTarjeta.error    = "Requerido"; return }
                if (etVencimiento.text.isNullOrBlank())   { etVencimiento.error   = "Requerido"; return }
                if (etCvv.text.isNullOrBlank())           { etCvv.error           = "Requerido"; return }
                if (etNombreTarjeta.text.isNullOrBlank()) { etNombreTarjeta.error = "Requerido"; return }
            }
            "Yape" -> {
                if (etCelularYape.text.isNullOrBlank()) { etCelularYape.error = "Requerido"; return }
            }
        }

        timer?.cancel()

        val codigoQRFinal = if (codigosQRStr.isNotEmpty()) {
            codigosQRStr.split("|").firstOrNull() ?: generarCodigoQR()
        } else {
            generarCodigoQR()
        }

        val idVenta = guardarEnSQLite(
            total, subtotal, descuento,
            idFuncion, idButacasStr, codigoQRFinal,
            confiteriaStr, idPromocion,
            cantidad, butacas
        )

        Intent(this, ComprobanteActivity::class.java).also {
            it.putExtra("titulo",            titulo)
            it.putExtra("total",             total)
            it.putExtra("gran_total",        total)       // ambas keys por seguridad
            it.putExtra("metodo_pago",       metodoPago)
            it.putExtra("tipo_comprobante",  tipoComprobante)  // FIX: key correcta
            it.putExtra("butacas",           butacas)
            it.putExtra("sede",              sede)
            it.putExtra("sala",              sala)             // FIX: agregar sala
            it.putExtra("hora",              hora)
            it.putExtra("fecha",             fecha)
            it.putExtra("nombre",            "${etNombre.text} ${etApellido.text}")
            it.putExtra("email",             etEmail.text.toString())
            it.putExtra("codigo_qr",         codigoQRFinal)
            it.putExtra("id_venta",          idVenta.toInt())
            startActivity(it)
            finish()
        }
    }

    private fun guardarEnSQLite(
        total: Double, subtotal: Double, descuento: Double,
        idFuncion: Int, idButacasStr: String, codigoQR: String,
        confiteriaStr: String, idPromocion: Int?,
        cantidad: Int, butacas: String
    ): Long {
        val idCliente = com.idat.movietime.network.SessionManager(this).getIdUsuario()

        val db = DatabaseHelper(this)

        val idButacas = if (idButacasStr.isNotEmpty()) {
            idButacasStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }

        val codigosQR = if (idButacas.isNotEmpty()) {
            idButacas.mapIndexed { i, _ ->
                if (i == 0) codigoQR else generarCodigoQR()
            }
        } else {
            listOf(codigoQR)
        }

        val productosConfit = mutableListOf<VentaDetalle.ConfiteriaItem>()
        if (confiteriaStr.isNotEmpty()) {
            confiteriaStr.split(",").forEach { parte ->
                val campos = parte.split(":")
                if (campos.size >= 4) {
                    val item = VentaDetalle.ConfiteriaItem()
                    item.idProducto     = campos[0].trim().toIntOrNull() ?: 0
                    item.cantidad       = campos[1].trim().toIntOrNull() ?: 1
                    item.precioUnitario = campos[2].trim().toDoubleOrNull() ?: 0.0
                    item.subtotal       = campos[3].trim().toDoubleOrNull() ?: 0.0
                    if (item.idProducto > 0) productosConfit.add(item)
                }
            }
        }

        val idVenta = db.insertarVentaCompleta(
            idCliente,
            idFuncion,
            idButacas,
            if (idButacas.isEmpty()) total / maxOf(cantidad, 1) else total / maxOf(idButacas.size, 1),
            codigosQR,
            productosConfit,
            subtotal,
            descuento,
            total,
            tipoComprobante,
            metodoPago,
            idPromocion
        )

        db.close()
        return idVenta
    }

    private fun generarCodigoQR(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "MT-" + (1..8).map { chars.random() }.joinToString("")
    }

    private fun bindViews() {
        tvCronometro    = findViewById(R.id.tvCronometro)
        btnBoleta       = findViewById(R.id.btnBoleta)
        btnFactura      = findViewById(R.id.btnFactura)
        layoutFactura   = findViewById(R.id.layoutFactura)
        etNumeroDoc     = findViewById(R.id.etNumeroDoc)
        etNombre        = findViewById(R.id.etNombre)
        etApellido      = findViewById(R.id.etApellido)
        etEmail         = findViewById(R.id.etEmail)
        etRuc           = findViewById(R.id.etRuc)
        etRazonSocial   = findViewById(R.id.etRazonSocial)
        etDireccion     = findViewById(R.id.etDireccion)
        etTelefono      = findViewById(R.id.etTelefono)
        rbTarjeta       = findViewById(R.id.rbTarjeta)
        rbYape          = findViewById(R.id.rbYape)
        layoutTarjeta   = findViewById(R.id.layoutTarjeta)
        layoutYape      = findViewById(R.id.layoutYape)
        etNumTarjeta    = findViewById(R.id.etNumTarjeta)
        etVencimiento   = findViewById(R.id.etVencimiento)
        etCvv           = findViewById(R.id.etCvv)
        etNombreTarjeta = findViewById(R.id.etNombreTarjeta)
        etCelularYape   = findViewById(R.id.etCelularYape)
        switchTerminos  = findViewById(R.id.switchTerminos)
        tvTotal         = findViewById(R.id.tvTotal)
        btnPagar        = findViewById(R.id.btnPagar)
    }

    private fun setupComprobante() {
        btnBoleta.setOnClickListener  { seleccionarBoleta() }
        btnFactura.setOnClickListener { seleccionarFactura() }
    }

    private fun seleccionarBoleta() {
        tipoComprobante = "Boleta"
        btnBoleta.backgroundTintList  = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#222222"))
        btnBoleta.setTextColor(android.graphics.Color.WHITE)
        btnFactura.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EEEEEE"))
        btnFactura.setTextColor(android.graphics.Color.parseColor("#888888"))
        layoutFactura.visibility = View.GONE
    }

    private fun seleccionarFactura() {
        tipoComprobante = "Factura"
        btnFactura.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#222222"))
        btnFactura.setTextColor(android.graphics.Color.WHITE)
        btnBoleta.backgroundTintList  = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EEEEEE"))
        btnBoleta.setTextColor(android.graphics.Color.parseColor("#888888"))
        layoutFactura.visibility = View.VISIBLE
    }

    private fun setupMetodoPago() {
        seleccionarTarjeta()
        rbTarjeta.setOnClickListener { seleccionarTarjeta() }
        rbYape.setOnClickListener    { seleccionarYape() }
        findViewById<View>(R.id.rgMetodoPago)?.setOnClickListener  { seleccionarTarjeta() }
        findViewById<View>(R.id.rgMetodoPago2)?.setOnClickListener { seleccionarYape() }

        etVencimiento.addTextChangedListener(object : TextWatcher {
            var editing = false
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editing) return
                editing = true
                val digits = s.toString().filter { it.isDigit() }
                val formatted = when {
                    digits.length >= 4 -> "${digits.substring(0,2)}/${digits.substring(2,4)}"
                    digits.length >= 3 -> "${digits.substring(0,2)}/${digits.substring(2)}"
                    digits.length == 2 -> digits
                    else               -> digits
                }
                etVencimiento.setText(formatted)
                etVencimiento.setSelection(formatted.length)
                editing = false
            }
        })
        setupPinYape()
    }

    private fun seleccionarTarjeta() {
        metodoPago           = "Tarjeta"
        rbTarjeta.isChecked  = true
        rbYape.isChecked     = false
        layoutTarjeta.visibility = View.VISIBLE
        layoutYape.visibility    = View.GONE
    }

    private fun seleccionarYape() {
        metodoPago           = "Yape"
        rbYape.isChecked     = true
        rbTarjeta.isChecked  = false
        layoutYape.visibility    = View.VISIBLE
        layoutTarjeta.visibility = View.GONE
    }

    private fun setupPinYape() {
        val pins = listOf<EditText>(
            findViewById(R.id.pin1), findViewById(R.id.pin2),
            findViewById(R.id.pin3), findViewById(R.id.pin4),
            findViewById(R.id.pin5), findViewById(R.id.pin6)
        )
        pins.forEachIndexed { i, et ->
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty() && i < pins.size - 1) pins[i + 1].requestFocus()
                }
            })
        }
    }

    private fun iniciarCronometro() {
        timer = object : CountDownTimer(10 * 60 * 1000L, 1000) {
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

    override fun onDestroy() { super.onDestroy(); timer?.cancel() }
}