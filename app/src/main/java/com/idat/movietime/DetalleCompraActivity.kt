package com.idat.movietime

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.VentaDetalle
import java.text.SimpleDateFormat
import java.util.EnumMap
import java.util.Locale
import java.util.TimeZone

class DetalleCompraActivity : AppCompatActivity() {

    private lateinit var tvEstadoCompra: TextView
    private lateinit var tvFechaDetalle: TextView
    private lateinit var cardQR: LinearLayout
    private lateinit var ivQR: ImageView
    private lateinit var tvCodigoQR: TextView
    private lateinit var tvEstadoIngreso: TextView
    private lateinit var tvPelicula: TextView
    private lateinit var tvSala: TextView
    private lateinit var tvButaca: TextView
    private lateinit var tvFuncion: TextView
    private lateinit var cardConfiteria: LinearLayout
    private lateinit var containerProductos: LinearLayout
    private lateinit var tvSubtotal: TextView
    private lateinit var layoutDescuento: LinearLayout
    private lateinit var tvDescuento: TextView
    private lateinit var tvTotalDetalle: TextView
    private lateinit var tvMetodoDetalle: TextView
    private lateinit var tvComprobanteDetalle: TextView
    private lateinit var tvNotaQR: TextView

    private lateinit var dbHelper: DatabaseHelper
    private val TZ_UTC = TimeZone.getTimeZone("UTC")
    private val TZ_PERU = TimeZone.getTimeZone("America/Lima")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_compra)

        dbHelper = DatabaseHelper(this)

        findViewById<TextView>(R.id.tvToolbarTitulo)?.text = "Detalle de compra"
        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }

        findViewById<View>(R.id.btnAceptarInicio)?.setOnClickListener {
            val intent = Intent(this, PeliculasActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }

        bindViews()

        val idVenta = intent.getIntExtra("id_venta", -1)
        val tituloIntent = intent.getStringExtra("titulo") ?: ""
        val salaIntent = intent.getStringExtra("sala") ?: ""
        val butacasIntent = intent.getStringExtra("butacas") ?: ""
        val fechaIntent = intent.getStringExtra("fecha") ?: ""
        val horaIntent = intent.getStringExtra("hora") ?: ""
        val fechaCompleta = if (fechaIntent.isNotEmpty() && horaIntent.isNotEmpty()) "$fechaIntent $horaIntent" else fechaIntent

        if (idVenta > 0 && tituloIntent.isNotEmpty()) {
            dbHelper.actualizarFallbackVenta(idVenta, tituloIntent, salaIntent, butacasIntent, fechaCompleta)
        }

        if (idVenta <= 0) {
            mostrarFallbackDesdeExtras()
            return
        }

        mostrarFallbackDesdeExtras()

        Thread {
            val venta = dbHelper.getDetalleVenta(idVenta)
            runOnUiThread {
                if (venta != null) {
                    poblarUI(venta)
                }
            }
        }.start()
    }

    private fun bindViews() {
        tvEstadoCompra = findViewById(R.id.tvEstadoCompra)
        tvFechaDetalle = findViewById(R.id.tvFechaDetalle)
        cardQR = findViewById(R.id.cardQR)
        ivQR = findViewById(R.id.ivQR)
        tvCodigoQR = findViewById(R.id.tvCodigoQR)
        tvEstadoIngreso = findViewById(R.id.tvEstadoIngreso)
        tvPelicula = findViewById(R.id.tvPelicula)
        tvSala = findViewById(R.id.tvSala)
        tvButaca = findViewById(R.id.tvButaca)
        tvFuncion = findViewById(R.id.tvFuncion)
        cardConfiteria = findViewById(R.id.cardConfiteria)
        containerProductos = findViewById(R.id.containerProductos)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        layoutDescuento = findViewById(R.id.layoutDescuento)
        tvDescuento = findViewById(R.id.tvDescuento)
        tvTotalDetalle = findViewById(R.id.tvTotalDetalle)
        tvMetodoDetalle = findViewById(R.id.tvMetodoDetalle)
        tvComprobanteDetalle = findViewById(R.id.tvComprobanteDetalle)
        tvNotaQR = findViewById(R.id.tvNotaQR)
    }

    private fun mostrarFallbackDesdeExtras() {
        val total = intent.getDoubleExtra("gran_total", 0.0)
        val subtotal = intent.getDoubleExtra("total_entradas", 0.0)
        val descuento = intent.getDoubleExtra("descuento", 0.0)

        tvTotalDetalle.text = "S/ %.2f".format(total)
        tvSubtotal.text = "S/ %.2f".format(subtotal)
        tvMetodoDetalle.text = intent.getStringExtra("metodo_pago") ?: "—"
        tvComprobanteDetalle.text = intent.getStringExtra("tipo_comprobante") ?: "Boleta"

        if (descuento > 0) {
            layoutDescuento.visibility = View.VISIBLE
            tvDescuento.text = "-S/ %.2f".format(descuento)
        } else {
            layoutDescuento.visibility = View.GONE
        }

        cardQR.visibility = View.GONE
        cardConfiteria.visibility = View.GONE
        tvNotaQR.visibility = View.GONE
    }

    private fun poblarUI(venta: VentaDetalle) {
        if (venta.estadoVenta == "Anulada") {
            tvEstadoCompra.text = "✕ Venta anulada"
            tvEstadoCompra.setTextColor(Color.parseColor("#F44336"))
        } else {
            tvEstadoCompra.text = "✓ Compra exitosa"
            tvEstadoCompra.setTextColor(Color.parseColor("#4CAF50"))
        }
        tvFechaDetalle.text = formatFechaCorta(venta.fechaVenta)

        tvSubtotal.text = "S/ %.2f".format(venta.subtotal)
        tvTotalDetalle.text = "S/ %.2f".format(venta.total)
        tvMetodoDetalle.text = venta.metodoPago ?: "—"
        tvComprobanteDetalle.text = venta.tipoComprobante ?: "Boleta"

        if (venta.tieneDescuento()) {
            layoutDescuento.visibility = View.VISIBLE
            tvDescuento.text = "-S/ %.2f".format(venta.descuento)
        } else {
            layoutDescuento.visibility = View.GONE
        }

        cardQR.visibility = View.VISIBLE
        tvNotaQR.visibility = View.VISIBLE

        val e = venta.entradas.firstOrNull()

        val tituloReal = e?.tituloPelicula?.takeIf { it.isNotEmpty() && !it.contains("Cine", true) }
            ?: venta.tituloPeliculaAux?.takeIf { it.isNotEmpty() && !it.contains("Cine", true) }
            ?: intent.getStringExtra("titulo")
            ?: "Entrada"

        val salaNombre = e?.nombreSala?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("sala")
            ?: "—"

        val butacasReal = e?.fila?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("butacas")
            ?: "—"

        val fechaHora = e?.fechaHoraFuncion?.takeIf { it.isNotEmpty() }
            ?: intent.getStringExtra("fecha")
            ?: "—"

        tvPelicula.text = tituloReal
        tvSala.text = salaNombre
        tvButaca.text = butacasReal

        val fechaFormateada = formatFechaFuncion(fechaHora)
        tvFuncion.text = fechaFormateada.takeIf { it.isNotEmpty() } ?: fechaHora

        val codigoQRStr = e?.codigoQR ?: "MT-${venta.idVenta}X"
        tvCodigoQR.text = codigoQRStr

        val estado = e?.estadoIngreso ?: "Pendiente"
        when (estado) {
            "Validado" -> {
                tvEstadoIngreso.text = "● Entrada utilizada"
                tvEstadoIngreso.setTextColor(Color.parseColor("#4CAF50"))
            }
            "Anulado" -> {
                tvEstadoIngreso.text = "● Entrada anulada"
                tvEstadoIngreso.setTextColor(Color.parseColor("#F44336"))
            }
            else -> {
                tvEstadoIngreso.text = "● Pendiente de ingreso"
                tvEstadoIngreso.setTextColor(Color.parseColor("#FFA726"))
            }
        }

        val qrContent = "MOVIETIME|$codigoQRStr|$tituloReal|$salaNombre|$butacasReal|$fechaHora|$estado"
        Thread {
            val bmp = generarQRBitmap(qrContent, 600)
            runOnUiThread { if (bmp != null) ivQR.setImageBitmap(bmp) }
        }.start()

        if (venta.tieneConfiteria()) {
            cardConfiteria.visibility = View.VISIBLE
            containerProductos.removeAllViews()
            for (item in venta.productosConfiteria) {
                val fila = LayoutInflater.from(this).inflate(R.layout.item_producto_detalle, containerProductos, false)
                fila.findViewById<TextView>(R.id.tvNombreProducto)?.text = "${item.cantidad}x  ${item.nombreProducto}"
                fila.findViewById<TextView>(R.id.tvPrecioProducto)?.text = "S/ %.2f".format(item.subtotal)
                containerProductos.addView(fila)
            }
        } else {
            cardConfiteria.visibility = View.GONE
        }
    }

    private fun generarQRBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size)
                for (y in 0 until size)
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            bmp
        } catch (e: Exception) { null }
    }

    private fun formatFechaFuncion(raw: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TZ_UTC }
            val sdfOut = SimpleDateFormat("EEE dd MMM  ·  hh:mm a", Locale("es", "PE")).apply { timeZone = TZ_PERU }
            sdfOut.format(sdfIn.parse(raw)!!)
        } catch (e: Exception) { raw }
    }
    private fun formatFechaCorta(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = TZ_UTC }
            val sdfOut = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale("es", "PE")).apply { timeZone = TZ_PERU }
            sdfOut.format(sdfIn.parse(raw)!!)
        } catch (e: Exception) { raw }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}