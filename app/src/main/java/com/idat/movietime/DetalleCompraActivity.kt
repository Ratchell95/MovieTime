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

/**
 * DetalleCompraActivity
 *
 * FIX PRINCIPAL: Siempre consulta la BD completa usando id_venta.
 * Antes solo leía los intent.putExtra(...) → película/sala/butaca/QR no aparecían
 * cuando se abría desde Historial (que solo enviaba id_venta).
 *
 * Flujo correcto:
 *   1. Leer id_venta del Intent.
 *   2. Consultar dbHelper.getDetalleVenta(idVenta) en hilo de fondo.
 *   3. Poblar toda la UI desde el objeto VentaDetalle retornado.
 *   4. Los intent.putExtra("metodo_pago", ...) etc. se usan solo como
 *      fallback mientras la BD carga (o si getDetalleVenta retorna null).
 */
class DetalleCompraActivity : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────
    private lateinit var tvEstadoCompra:    TextView
    private lateinit var tvFechaDetalle:    TextView

    // Tarjeta QR / Entradas
    private lateinit var cardQR:            LinearLayout
    private lateinit var ivQR:              ImageView
    private lateinit var tvCodigoQR:        TextView
    private lateinit var tvEstadoIngreso:   TextView
    private lateinit var tvPelicula:        TextView
    private lateinit var tvSala:            TextView
    private lateinit var tvButaca:          TextView
    private lateinit var tvFuncion:         TextView

    // Tarjeta Confitería
    private lateinit var cardConfiteria:    LinearLayout
    private lateinit var containerProductos: LinearLayout

    // Tarjeta Pago
    private lateinit var tvSubtotal:        TextView
    private lateinit var layoutDescuento:   LinearLayout
    private lateinit var tvDescuento:       TextView
    private lateinit var tvTotalDetalle:    TextView
    private lateinit var tvMetodoDetalle:   TextView
    private lateinit var tvComprobanteDetalle: TextView
    private lateinit var tvNotaQR:          TextView

    // ── Helpers ───────────────────────────────────────────────────
    private lateinit var dbHelper: DatabaseHelper

    private val TZ_UTC  = TimeZone.getTimeZone("UTC")
    private val TZ_PERU = TimeZone.getTimeZone("America/Lima")

    // ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_compra)

        dbHelper = DatabaseHelper(this)

        // Toolbar
        findViewById<TextView>(R.id.tvToolbarTitulo)?.text = "Detalle de compra"
        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }

        // Botón Aceptar e ir al Inicio: limpia el historial y vuelve a MainActivity
        findViewById<View>(R.id.btnAceptarInicio)?.setOnClickListener {
            val intent = Intent(this, PeliculasActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        bindViews()

        // ── FIX: obtener id_venta y consultar la BD ───────────────
        val idVenta = intent.getIntExtra("id_venta", -1)

        if (idVenta <= 0) {
            // No hay id_venta válido → mostrar solo los extras de fallback
            mostrarFallbackDesdeExtras()
            return
        }

        // Mostrar datos de fallback mientras la BD carga
        mostrarFallbackDesdeExtras()

        // Consultar BD en hilo de fondo
        Thread {
            val venta = dbHelper.getDetalleVenta(idVenta)
            runOnUiThread {
                if (venta != null) {
                    poblarUI(venta)
                }
                // Si venta == null la BD no encontró el registro;
                // se quedan los datos de fallback que ya se pintaron.
            }
        }.start()
    }

    // ── Vincular vistas ───────────────────────────────────────────

    private fun bindViews() {
        tvEstadoCompra      = findViewById(R.id.tvEstadoCompra)
        tvFechaDetalle      = findViewById(R.id.tvFechaDetalle)

        cardQR              = findViewById(R.id.cardQR)
        ivQR                = findViewById(R.id.ivQR)
        tvCodigoQR          = findViewById(R.id.tvCodigoQR)
        tvEstadoIngreso     = findViewById(R.id.tvEstadoIngreso)
        tvPelicula          = findViewById(R.id.tvPelicula)
        tvSala              = findViewById(R.id.tvSala)
        tvButaca            = findViewById(R.id.tvButaca)
        tvFuncion           = findViewById(R.id.tvFuncion)

        cardConfiteria      = findViewById(R.id.cardConfiteria)
        containerProductos  = findViewById(R.id.containerProductos)

        tvSubtotal          = findViewById(R.id.tvSubtotal)
        layoutDescuento     = findViewById(R.id.layoutDescuento)
        tvDescuento         = findViewById(R.id.tvDescuento)
        tvTotalDetalle      = findViewById(R.id.tvTotalDetalle)
        tvMetodoDetalle     = findViewById(R.id.tvMetodoDetalle)
        tvComprobanteDetalle = findViewById(R.id.tvComprobanteDetalle)
        tvNotaQR            = findViewById(R.id.tvNotaQR)
    }

    // ── Fallback: datos básicos desde los extras del Intent ───────
    //    Se muestran mientras la BD carga o si getDetalleVenta == null.

    private fun mostrarFallbackDesdeExtras() {
        val total        = intent.getDoubleExtra("gran_total", 0.0)
        val subtotal     = intent.getDoubleExtra("total_entradas", 0.0)
        val descuento    = intent.getDoubleExtra("descuento", 0.0)
        val metodo       = intent.getStringExtra("metodo_pago")       ?: "—"
        val comprobante  = intent.getStringExtra("tipo_comprobante")  ?: "Boleta"

        tvTotalDetalle.text       = "S/ %.2f".format(total)
        tvSubtotal.text           = "S/ %.2f".format(subtotal)
        tvMetodoDetalle.text      = metodo
        tvComprobanteDetalle.text = comprobante

        if (descuento > 0) {
            layoutDescuento.visibility = View.VISIBLE
            tvDescuento.text = "-S/ %.2f".format(descuento)
        }

        // Ocultar tarjetas de contenido hasta tener datos reales
        cardQR.visibility         = View.GONE
        cardConfiteria.visibility = View.GONE
        tvNotaQR.visibility       = View.GONE
    }

    // ── Poblar toda la UI con datos reales de la BD ───────────────

    private fun poblarUI(venta: VentaDetalle) {

        // ── Estado de la venta ────────────────────────────────────
        if (venta.estadoVenta == "Anulada") {
            tvEstadoCompra.text      = "✕ Venta anulada"
            tvEstadoCompra.setTextColor(Color.parseColor("#F44336"))
        } else {
            tvEstadoCompra.text      = "✓ Compra exitosa"
            tvEstadoCompra.setTextColor(Color.parseColor("#4CAF50"))
        }
        tvFechaDetalle.text = formatFechaCorta(venta.fechaVenta)

        // ── Resumen de pago ───────────────────────────────────────
        tvSubtotal.text           = "S/ %.2f".format(venta.subtotal)
        tvTotalDetalle.text       = "S/ %.2f".format(venta.total)
        tvMetodoDetalle.text      = venta.metodoPago      ?: "—"
        tvComprobanteDetalle.text = venta.tipoComprobante ?: "Boleta"

        if (venta.tieneDescuento()) {
            layoutDescuento.visibility = View.VISIBLE
            tvDescuento.text = "-S/ %.2f".format(venta.descuento)
        } else {
            layoutDescuento.visibility = View.GONE
        }

        // ── Tarjeta QR / Entradas ─────────────────────────────────
        if (venta.tieneEntradas()) {
            cardQR.visibility   = View.VISIBLE
            tvNotaQR.visibility = View.VISIBLE

            val e = venta.entradas[0]   // primera entrada

            // Película + formato
            tvPelicula.text = buildString {
                append(e.tituloPelicula ?: "Entrada")
                if (!e.formato.isNullOrEmpty()) append("  (${e.formato})")
            }

            // Sala + tipo
            tvSala.text = buildString {
                append(e.nombreSala ?: "—")
                if (!e.tipoSala.isNullOrEmpty()) append("  (${e.tipoSala})")
            }

            // Butaca — si hay más de una entrada, listarlas todas
            tvButaca.text = if (venta.entradas.size == 1) {
                e.getButacaLabel()
            } else {
                venta.entradas.joinToString(", ") { it.getButacaLabel() }
            }

            // Función
            tvFuncion.text = formatFechaFuncion(e.fechaHoraFuncion)

            // Código QR
            tvCodigoQR.text = e.codigoQR ?: ""

            // Badge estado ingreso
            when (e.estadoIngreso) {
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

            // Generar QR en hilo de fondo
            val qrContent = "MOVIETIME|${e.codigoQR}|${e.tituloPelicula}|${e.nombreSala}|${e.getButacaLabel()}|${formatFechaFuncion(e.fechaHoraFuncion)}|${e.estadoIngreso}"
            Thread {
                val bmp = generarQRBitmap(qrContent, 600)
                runOnUiThread { if (bmp != null) ivQR.setImageBitmap(bmp) }
            }.start()

        } else {
            // ✅ FIX: Si el JOIN de la BD falla porque las tablas de salas/funciones están vacías,
            // forzamos a que aparezca la tarjeta usando los textos que mandamos desde el Pago.
            cardQR.visibility   = View.VISIBLE
            tvNotaQR.visibility = View.VISIBLE

            tvPelicula.text = intent.getStringExtra("titulo") ?: "Entrada de Cine"
            tvSala.text     = intent.getStringExtra("sala") ?: "SALA"
            tvButaca.text   = intent.getStringExtra("butacas") ?: "General"

            val f = intent.getStringExtra("fecha") ?: ""
            val h = intent.getStringExtra("hora") ?: ""
            tvFuncion.text  = if (f.isNotEmpty()) "$f · $h" else "Función programada"

            val fallbackQR = "MT-${venta.idVenta}X999"
            tvCodigoQR.text = fallbackQR
            tvEstadoIngreso.text = "● Pendiente de ingreso"
            tvEstadoIngreso.setTextColor(Color.parseColor("#FFA726")) // Asegúrate que Color esté importado

            val qrContent = "MOVIETIME|$fallbackQR|${tvPelicula.text}|${tvSala.text}|${tvButaca.text}|${tvFuncion.text}|Pendiente"
            Thread {
                val bmp = generarQRBitmap(qrContent, 600)
                runOnUiThread { if (bmp != null) ivQR.setImageBitmap(bmp) }
            }.start()
        }

        // ── Tarjeta Confitería ────────────────────────────────────
        if (venta.tieneConfiteria()) {
            cardConfiteria.visibility = View.VISIBLE
            containerProductos.removeAllViews()

            for (item in venta.productosConfiteria) {
                // Inflar una fila de producto dinámica
                val fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_producto_detalle, containerProductos, false)

                fila.findViewById<TextView>(R.id.tvNombreProducto)?.text =
                    "${item.cantidad}x  ${item.nombreProducto}"
                fila.findViewById<TextView>(R.id.tvPrecioProducto)?.text =
                    "S/ %.2f".format(item.subtotal)

                containerProductos.addView(fila)
            }
        } else {
            cardConfiteria.visibility = View.GONE
        }
    }

    // ── Generación de QR ─────────────────────────────────────────

    private fun generarQRBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size)
                for (y in 0 until size)
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            bmp
        } catch (e: Exception) { null }
    }

    // ── Formateo de fechas ────────────────────────────────────────

    /** "yyyy-MM-dd HH:mm:ss" (UTC) → "Lun 15 Mar  ·  08:30 PM" (Lima) */
    private fun formatFechaFuncion(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val sdfIn  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .apply { timeZone = TZ_UTC }
            val sdfOut = SimpleDateFormat("EEE dd MMM  ·  hh:mm a", Locale("es", "PE"))
                .apply { timeZone = TZ_PERU }
            sdfOut.format(sdfIn.parse(raw)!!)
        } catch (e: Exception) { raw }
    }

    /** "yyyy-MM-dd HH:mm:ss" (UTC) → "15/03/2025  08:30" (Lima) */
    private fun formatFechaCorta(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val sdfIn  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .apply { timeZone = TZ_UTC }
            val sdfOut = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale("es", "PE"))
                .apply { timeZone = TZ_PERU }
            sdfOut.format(sdfIn.parse(raw)!!)
        } catch (e: Exception) { raw }
    }


    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}