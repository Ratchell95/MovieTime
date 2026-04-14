package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.VentaDetalle
import com.idat.movietime.model.VentaRequest
import com.idat.movietime.model.ApiResponse
import com.idat.movietime.model.VentaResumenResponse
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PagoActivity : AppCompatActivity() {

    private lateinit var tvCronometro:    TextView
    private lateinit var btnBoleta:       Button
    private lateinit var btnFactura:      Button
    private lateinit var layoutFactura:   View
    private lateinit var etNombre:        EditText
    private lateinit var etApellido:      EditText
    private lateinit var etEmail:         EditText
    private lateinit var etRuc:           EditText
    private lateinit var etRazonSocial:   EditText
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

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }
        iniciarCronometro()
        setupComprobante()
        setupMetodoPago()

        btnPagar.setOnClickListener {
            procesarPago(
                butacas, cantidadEntradas, totalFinal, subtotal, descuento,
                idFuncion, idButacasStr, codigosQRStr, confiteriaStr, idPromocion
            )
        }
    }

    private fun procesarPago(
        butacas: String, cantidad: Int, total: Double, subtotal: Double,
        descuento: Double, idFuncion: Int, idButacasStr: String,
        codigosQRStr: String, confiteriaStr: String, idPromocion: Int?
    ) {
        if (!switchTerminos.isChecked) {
            Toast.makeText(this, "Acepta los Términos y Condiciones", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Bloquear botón para que el usuario no presione 2 veces
        btnPagar.isEnabled = false
        btnPagar.text = "PROCESANDO..."

        // 2. Preparar el objeto Request para Spring Boot
        val sessionID = SessionManager(this).getIdUsuario()
        val idClienteFinal = if (sessionID > 0) sessionID else null

        // 2.1 Parsear las entradas
        val listaEntradas = mutableListOf<VentaRequest.EntradaItem>()
        if (idButacasStr.isNotEmpty() && idFuncion > 0) {
            val butacasIds = idButacasStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            for (bId in butacasIds) {
                listaEntradas.add(VentaRequest.EntradaItem(idFuncion, bId))
            }
        } else {
            // Si son entradas no numeradas, armamos datos genéricos (Asegúrate que MySQL lo permita)
            for (i in 1..cantidad) {
                listaEntradas.add(VentaRequest.EntradaItem(idFuncion, i))
            }
        }

        // 2.2 Parsear la confitería
        val listaConfiteria = mutableListOf<VentaRequest.ConfiteriaItem>()
        if (confiteriaStr.isNotEmpty()) {
            confiteriaStr.split(",").forEach { parte ->
                val campos = parte.split(":")
                if (campos.size >= 2) {
                    val idProd = campos[0].trim().toIntOrNull() ?: 0
                    val cant = campos[1].trim().toIntOrNull() ?: 1
                    if (idProd > 0) {
                        listaConfiteria.add(VentaRequest.ConfiteriaItem(idProd, cant))
                    }
                }
            }
        }

        // 2.3 Ensamblar la petición final
        val request = VentaRequest(
            idCliente = idClienteFinal,
            idPromocion = idPromocion,
            canalVenta = "App",
            tipoComprobante = tipoComprobante,
            metodoPago = metodoPago,
            entradas = listaEntradas,
            confiteria = listaConfiteria
        )

        // 3. ENVIAR A MYSQL VÍA RETROFIT Y SPRING BOOT
        RetrofitClient.api.registrarVenta(request).enqueue(object : Callback<ApiResponse<VentaResumenResponse>> {

            override fun onResponse(call: Call<ApiResponse<VentaResumenResponse>>, response: Response<ApiResponse<VentaResumenResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {

                    // ¡ÉXITO EN EL SERVIDOR! Ahora guardamos localmente para la UI y el QR
                    timer?.cancel()
                    finalizarCompraLocalYMostrarQR(
                        total, subtotal, descuento, idFuncion, idButacasStr,
                        codigosQRStr, confiteriaStr, idPromocion, cantidad, butacas
                    )

                } else {
                    // Falló en el backend (ej: butaca ya ocupada o error 400)
                    btnPagar.isEnabled = true
                    btnPagar.text = "PAGAR"
                    Toast.makeText(this@PagoActivity, "Error del servidor: Revisa los datos de compra", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<VentaResumenResponse>>, t: Throwable) {
                // No hay conexión con tu localhost/servidor
                btnPagar.isEnabled = true
                btnPagar.text = "PAGAR"
                Toast.makeText(this@PagoActivity, "Sin conexión al servidor MySQL. Revisa tu red.", Toast.LENGTH_LONG).show()
            }
        })
    }


    // Este método se llama SOLO si el servidor Spring Boot responde OK
    private fun finalizarCompraLocalYMostrarQR(
        total: Double, subtotal: Double, descuento: Double, idFuncion: Int,
        idButacasStr: String, codigosQRStr: String, confiteriaStr: String,
        idPromocion: Int?, cantidad: Int, butacas: String
    ) {
        val codigoQRFinal = if (codigosQRStr.isNotEmpty()) {
            codigosQRStr.split("|").firstOrNull() ?: generarCodigoQR()
        } else {
            generarCodigoQR()
        }

        // Guardado local (Tu código original)
        val idVentaLocal = guardarEnSQLite(
            total, subtotal, descuento, idFuncion, idButacasStr, codigoQRFinal,
            confiteriaStr, idPromocion, cantidad, butacas
        )

        if (idVentaLocal > 0) {
            DatabaseHelper(this).bloquearButacasPermanentes(butacas, idFuncion)

            val intentDetalle = Intent(this, DetalleCompraActivity::class.java).apply {
                putExtra("id_venta", idVentaLocal.toInt())
                putExtra("metodo_pago", metodoPago)
                putExtra("tipo_comprobante", tipoComprobante)
                putExtra("gran_total", total)
                putExtra("titulo",  intent.getStringExtra("titulo") ?: "")
                putExtra("butacas", butacas)
                putExtra("sala",    intent.getStringExtra("sala")  ?: "SALA 05")
                putExtra("fecha",   intent.getStringExtra("fecha") ?: "")
                putExtra("hora",    intent.getStringExtra("hora")  ?: "")
            }
            startActivity(intentDetalle)
            finish()
        } else {
            Toast.makeText(this, "Se guardó en la nube pero falló localmente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarEnSQLite(
        total: Double, subtotal: Double, descuento: Double,
        idFuncion: Int, idButacasStr: String, codigoQR: String,
        confiteriaStr: String, idPromocion: Int?,
        cantidad: Int, butacas: String
    ): Long {
        val db = DatabaseHelper(this)
        val sessionID = SessionManager(this).getIdUsuario()
        val idClienteFinal = if (sessionID > 0) sessionID else -1 // <-- Corregido para Trigger

        val funcId = if (idFuncion > 0) idFuncion else 1

        var idButacas: List<Int> = emptyList()
        var codigosQR: List<String> = emptyList()

        if (idButacasStr.isNotEmpty()) {
            idButacas = idButacasStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            idButacas = (1..cantidad).map { it }
        }

        codigosQR = idButacas.mapIndexed { i, _ -> if (i == 0) codigoQR else generarCodigoQR() }

        val productosConfit = mutableListOf<VentaDetalle.ConfiteriaItem>()
        if (confiteriaStr.isNotEmpty()) {
            confiteriaStr.split(",").forEach { parte ->
                val campos = parte.split(":")
                if (campos.size >= 4) {
                    val item = VentaDetalle.ConfiteriaItem()
                    item.idProducto = campos[0].trim().toIntOrNull() ?: 0
                    item.cantidad = campos[1].trim().toIntOrNull() ?: 1
                    item.precioUnitario = campos[2].trim().toDoubleOrNull() ?: 0.0
                    item.subtotal = campos[3].trim().toDoubleOrNull() ?: 0.0
                    if (item.idProducto > 0) productosConfit.add(item)
                }
            }
        }

        return db.insertarVentaCompleta(
            idClienteFinal, funcId, idButacas, total / maxOf(idButacas.size, 1),
            codigosQR, productosConfit, subtotal, descuento, total,
            tipoComprobante, metodoPago, idPromocion
        )
    }

    private fun generarCodigoQR() = "MT-" + (1..8).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

    private fun bindViews() {
        tvCronometro    = findViewById(R.id.tvCronometro)
        btnBoleta       = findViewById(R.id.btnBoleta)
        btnFactura      = findViewById(R.id.btnFactura)
        layoutFactura   = findViewById(R.id.layoutFactura)
        etNombre        = findViewById(R.id.etNombre)
        etApellido      = findViewById(R.id.etApellido)
        etEmail         = findViewById(R.id.etEmail)
        etRuc           = findViewById(R.id.etRuc)
        etRazonSocial   = findViewById(R.id.etRazonSocial)
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
        btnBoleta.setOnClickListener  {
            tipoComprobante = "Boleta"
            layoutFactura.visibility = View.GONE
        }
        btnFactura.setOnClickListener {
            tipoComprobante = "Factura"
            layoutFactura.visibility = View.VISIBLE
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
                tvCronometro.text = "⏱ %02d:%02d".format(ms / 60000, (ms % 60000) / 1000)
            }
            override fun onFinish() { finish() }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}