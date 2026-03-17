package com.idat.movietime

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ComprobanteActivity : AppCompatActivity() {

    private lateinit var tvTitulo:      TextView
    private lateinit var tvTotal:       TextView
    private lateinit var tvMetodo:      TextView
    private lateinit var tvComprobante: TextView
    private lateinit var tvCodigo:      TextView
    private lateinit var ivQR:          ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobante)

        tvTitulo      = findViewById(R.id.tvTitulo)
        tvTotal       = findViewById(R.id.tvTotal)
        tvMetodo      = findViewById(R.id.tvMetodo)
        tvComprobante = findViewById(R.id.tvComprobante)
        tvCodigo      = findViewById(R.id.tvCodigo)
        ivQR          = findViewById(R.id.ivQR)

        val titulo      = intent.getStringExtra("titulo") ?: ""
        val total       = intent.getDoubleExtra("total", 0.0)
        val metodoPago  = intent.getStringExtra("metodo_pago") ?: ""
        val comprobante = intent.getStringExtra("comprobante") ?: ""
        val butacas     = intent.getStringExtra("butacas") ?: ""
        val sede        = intent.getStringExtra("sede") ?: ""
        val fecha       = intent.getStringExtra("fecha") ?: ""
        val hora        = intent.getStringExtra("hora") ?: ""

        val codigoQR = UUID.randomUUID().toString().uppercase().take(12)

        tvTitulo.text      = "✓ Compra exitosa"
        tvTotal.text       = "Total pagado: S/ ${"%.2f".format(total)}"
        tvMetodo.text      = "Método: $metodoPago"
        tvComprobante.text = "Comprobante: $comprobante"
        tvCodigo.text      = "Código: $codigoQR"

        guardarEnHistorial(titulo, total, metodoPago, comprobante, codigoQR, butacas, sede, fecha, hora)
        generarQR(codigoQR)
    }

    private fun guardarEnHistorial(
        titulo: String, total: Double, metodoPago: String,
        comprobante: String, codigoQR: String,
        butacas: String, sede: String, fecha: String, hora: String
    ) {
        val prefs  = getSharedPreferences("movietime_historial", MODE_PRIVATE)
        val jsonStr = prefs.getString("compras", "[]") ?: "[]"
        val array  = JSONArray(jsonStr)

        val nuevaCompra = JSONObject().apply {
            put("titulo",      titulo)
            put("total",       total)
            put("metodoPago",  metodoPago)
            put("comprobante", comprobante)
            put("codigoQR",    codigoQR)
            put("butacas",     butacas)
            put("sede",        sede)
            put("fecha",       fecha)
            put("hora",        hora)
            put("fechaCompra", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
        }

        array.put(nuevaCompra)
        prefs.edit().putString("compras", array.toString()).apply()
    }

    private fun generarQR(codigo: String) {
        try {
            val writer  = MultiFormatWriter()
            val matrix  = writer.encode(codigo, BarcodeFormat.QR_CODE, 400, 400)
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.createBitmap(matrix)
            ivQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}