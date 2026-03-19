package com.idat.movietime

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.idat.movietime.db.DatabaseHelper

class DetalleQRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_qr)

        findViewById<TextView>(R.id.tvToolbarTitulo)?.text = "Detalle de entrada"
        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }

        val tvDetalle   = findViewById<TextView>(R.id.tvDetalleQR)
        val tvBadge     = findViewById<TextView>(R.id.tvEstadoBadge)
        val btnEscanear = findViewById<Button>(R.id.btnEscanearOtro)

        val invalido  = intent.getBooleanExtra("qr_invalido", false)
        val codigoQR  = intent.getStringExtra("codigo_qr")  ?: ""
        val idVenta   = intent.getIntExtra("id_venta", -1)
        val pelicula  = intent.getStringExtra("pelicula")   ?: ""
        val sala      = intent.getStringExtra("sala")       ?: ""
        val butaca    = intent.getStringExtra("butaca")     ?: ""
        val fecha     = intent.getStringExtra("fecha")      ?: ""
        val estado    = intent.getStringExtra("estado")     ?: "Pendiente"

        if (invalido) {

            tvBadge.text = "❌  QR no válido"
            tvBadge.setBackgroundResource(R.drawable.bg_badge_red)
            tvDetalle.text = "Este código QR no pertenece a MovieTime.\n\nContenido:\n$codigoQR"

        } else if (idVenta != -1) {

            val db    = DatabaseHelper(this)
            val venta = db.getDetalleVenta(idVenta)
            db.close()

            if (venta != null) {

                if (venta.estadoVenta == "Anulada") {
                    tvBadge.text = "🚫  Anulada"
                    tvBadge.setBackgroundResource(R.drawable.bg_badge_red)
                } else {
                    tvBadge.text = "✅  Entrada válida"
                    tvBadge.setBackgroundResource(R.drawable.bg_badge_green)
                }

                val sb = StringBuilder()

                // Entradas
                if (venta.entradas.isNotEmpty()) {
                    val e = venta.entradas[0]
                    sb.appendLine("🎬  ${e.tituloPelicula}")
                    sb.appendLine("🎭  ${e.nombreSala}  (${e.tipoSala})")
                    sb.appendLine("💺  Butaca ${e.fila}${e.numero}  ·  ${e.tipoButaca}")
                    sb.appendLine("📅  ${e.fechaHoraFuncion}")
                    sb.appendLine("🎞️  ${e.formato}  ·  ${e.clasificacion}")
                    if (venta.entradas.size > 1) {
                        val todas = venta.entradas.joinToString(", ") { "${it.fila}${it.numero}" }
                        sb.appendLine("💺  Todas las butacas: $todas")
                    }
                    sb.appendLine()
                }

                // Confitería
                if (venta.productosConfiteria.isNotEmpty()) {
                    sb.appendLine("🍿  Confitería:")
                    for (p in venta.productosConfiteria) {
                        sb.appendLine("    ${p.cantidad}x ${p.nombreProducto}   S/ ${"%.2f".format(p.subtotal)}")
                    }
                    sb.appendLine()
                }

                // Pago
                sb.appendLine("💳  ${venta.metodoPago}  ·  ${venta.tipoComprobante}")
                sb.appendLine("💰  Total:  S/ ${"%.2f".format(venta.total)}")
                if (venta.descuento > 0)
                    sb.appendLine("🏷️  Descuento aplicado:  S/ ${"%.2f".format(venta.descuento)}")
                sb.appendLine()
                sb.appendLine("🔑  Cód: $codigoQR")

                tvDetalle.text = sb.toString().trimEnd()

            } else {
                mostrarDatosQR(tvBadge, tvDetalle, pelicula, sala, butaca, fecha, estado, codigoQR)
            }

        } else {

            mostrarDatosQR(tvBadge, tvDetalle, pelicula, sala, butaca, fecha, estado, codigoQR)
        }

        btnEscanear.setOnClickListener {
            finish()
        }
    }

    private fun mostrarDatosQR(
        tvBadge: TextView, tvDetalle: TextView,
        pelicula: String, sala: String, butaca: String,
        fecha: String, estado: String, codigoQR: String
    ) {
        val emoji = if (estado == "Validado") "✅" else "🎫"
        tvBadge.text = "$emoji  $estado"
        tvBadge.setBackgroundResource(
            if (estado == "Validado") R.drawable.bg_badge_green
            else R.drawable.bg_badge_red
        )

        tvDetalle.text = buildString {
            if (pelicula.isNotEmpty()) appendLine("🎬  $pelicula")
            if (sala.isNotEmpty())     appendLine("🎭  $sala")
            if (butaca.isNotEmpty())   appendLine("💺  Butaca $butaca")
            if (fecha.isNotEmpty())    appendLine("📅  $fecha")
            appendLine()
            appendLine("📌  Estado: $estado")
            appendLine()
            appendLine("🔑  Cód: $codigoQR")
        }.trimEnd()
    }
}
