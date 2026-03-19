package com.idat.movietime

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

class ComprobanteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobante)

        val titulo      = intent.getStringExtra("titulo")           ?: ""
        val sede        = intent.getStringExtra("sede")             ?: ""
        val sala        = intent.getStringExtra("sala")             ?: sede  // FIX: leer sala
        val hora        = intent.getStringExtra("hora")             ?: ""
        val fecha       = intent.getStringExtra("fecha")            ?: ""
        val butacas     = intent.getStringExtra("butacas")          ?: ""
        val metodo      = intent.getStringExtra("metodo_pago")      ?: ""
        val codigoQR    = intent.getStringExtra("codigo_qr")        ?: generarCodigoUnico()

        // FIX: leer "tipo_comprobante" (key correcta enviada por PagoActivity)
        val comprobante = intent.getStringExtra("tipo_comprobante")
            ?: intent.getStringExtra("comprobante")
            ?: "Boleta"

        // Intentar todas las keys posibles para el total
        var granTotal = intent.getDoubleExtra("gran_total", 0.0)
        if (granTotal == 0.0) granTotal = intent.getDoubleExtra("total", 0.0)
        if (granTotal == 0.0) {
            val totalEnt  = intent.getDoubleExtra("total_entradas",   0.0)
            val totalConf = intent.getDoubleExtra("total_confiteria", 0.0)
            granTotal = totalEnt + totalConf
        }

        findViewById<TextView>(R.id.tvTitulo)?.text      = "✓ Compra exitosa"
        findViewById<TextView>(R.id.tvCodigo)?.text      = "Código: $codigoQR"
        findViewById<TextView>(R.id.tvTotal)?.text       = "Total pagado: S/ ${"%.2f".format(granTotal)}"
        findViewById<TextView>(R.id.tvMetodo)?.text      = "Método: $metodo"
        findViewById<TextView>(R.id.tvComprobante)?.text = "Comprobante: $comprobante"

        val contenidoQR = "MOVIETIME|$codigoQR|$titulo|$sala|$butacas|$fecha $hora|Pendiente"
        generarQR(contenidoQR)

        findViewById<android.widget.Button>(R.id.btnInicio)?.setOnClickListener {
            startActivity(Intent(this, PeliculasActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun generarQR(content: String) {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 2)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 600, 600, hints)
            val w = matrix.width; val h = matrix.height
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            for (x in 0 until w) for (y in 0 until h)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            findViewById<ImageView>(R.id.ivQR)?.setImageBitmap(bmp)
        } catch (e: Exception) {
            findViewById<ImageView>(R.id.ivQR)?.setImageResource(android.R.drawable.ic_dialog_alert)
        }
    }

    private fun generarCodigoUnico(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "MT-" + (1..9).map { chars.random() }.joinToString("")
    }
}