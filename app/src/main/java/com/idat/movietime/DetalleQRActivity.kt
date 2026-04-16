package com.idat.movietime

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.SessionManager

class DetalleQRActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_qr)

        dbHelper = DatabaseHelper(this)

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvToolbarTitulo)?.text = "Control de Ingreso"


        val tvEstadoBadge     = findViewById<TextView>(R.id.tvEstadoBadge)
        val tvPeliculaDetalle = findViewById<TextView>(R.id.tvPeliculaDetalle)
        val tvSalaDetalle     = findViewById<TextView>(R.id.tvSalaDetalle)
        val tvButacaDetalle   = findViewById<TextView>(R.id.tvButacaDetalle)
        val tvFechaDetalle    = findViewById<TextView>(R.id.tvFechaDetalle)
        val tvEstadoDetalle   = findViewById<TextView>(R.id.tvEstadoDetalle)
        val tvCodigoDetalle   = findViewById<TextView>(R.id.tvCodigoDetalle)

        val cardConfiteriaQR  = findViewById<CardView>(R.id.cardConfiteriaQR)
        val containerConfiteriaQR = findViewById<LinearLayout>(R.id.containerConfiteriaQR)
        val cardPagoQR        = findViewById<CardView>(R.id.cardPagoQR)
        val tvMetodoPagoQR    = findViewById<TextView>(R.id.tvMetodoPagoQR)
        val tvTotalQR         = findViewById<TextView>(R.id.tvTotalQR)

        val btnValidarEntrada = findViewById<Button>(R.id.btnValidarEntrada)
        val btnEscanearOtro   = findViewById<Button>(R.id.btnEscanearOtro)

        btnEscanearOtro.setOnClickListener { finish() }

        val esInvalido = intent.getBooleanExtra("qr_invalido", false)
        val codigoQR   = intent.getStringExtra("codigo_qr") ?: ""


        if (esInvalido) {
            tvEstadoBadge.text   = "Inválido"
            tvEstadoDetalle.text = "CÓDIGO QR INVÁLIDO"
            tvEstadoDetalle.setTextColor(Color.parseColor("#F44336"))

            tvPeliculaDetalle.text = "Desconocido"
            tvSalaDetalle.text     = "—"
            tvButacaDetalle.text   = "—"
            tvFechaDetalle.text    = "—"
            tvCodigoDetalle.text   = codigoQR

            btnValidarEntrada.visibility = View.GONE
            return
        }

        val pelicula = intent.getStringExtra("pelicula") ?: "—"
        val sala     = intent.getStringExtra("sala") ?: "—"
        val butacas  = intent.getStringExtra("butaca") ?: "—"
        val fecha    = intent.getStringExtra("fecha") ?: "—"
        val idVenta  = intent.getIntExtra("id_venta", -1)

        tvPeliculaDetalle.text = pelicula
        tvSalaDetalle.text     = sala
        tvButacaDetalle.text   = butacas
        tvFechaDetalle.text    = fecha
        tvCodigoDetalle.text   = codigoQR


        val estadoReal = dbHelper.getEstadoIngreso(codigoQR)
        val detalleVenta = if (idVenta > 0) dbHelper.getDetalleVenta(idVenta) else null


        if (detalleVenta != null) {
            cardPagoQR.visibility = View.VISIBLE
            tvMetodoPagoQR.text = "${detalleVenta.metodoPago ?: "Yape"} (${detalleVenta.tipoComprobante ?: "Boleta"})"
            tvTotalQR.text = "S/ %.2f".format(detalleVenta.total)

            if (detalleVenta.tieneConfiteria()) {
                cardConfiteriaQR.visibility = View.VISIBLE
                containerConfiteriaQR.removeAllViews()
                for (item in detalleVenta.productosConfiteria) {
                    val tvItem = TextView(this).apply {
                        text = "${item.cantidad}x ${item.nombreProducto}"
                        setTextColor(Color.parseColor("#CCCCCC"))
                        textSize = 14f
                        setPadding(0, 0, 0, 8)
                    }
                    containerConfiteriaQR.addView(tvItem)
                }
            }
        }


        when (estadoReal) {
            "Pendiente" -> {
                tvEstadoBadge.text   = "Pendiente"
                tvEstadoDetalle.text = "LISTO PARA INGRESAR"
                tvEstadoDetalle.setTextColor(Color.parseColor("#4CAF50"))
                btnValidarEntrada.visibility = View.VISIBLE
            }
            "Validado" -> {
                tvEstadoBadge.text   = "Validado"
                tvEstadoDetalle.text = " ENTRADA YA UTILIZADA"
                tvEstadoDetalle.setTextColor(Color.parseColor("#F44336"))
                btnValidarEntrada.visibility = View.GONE
            }
            else -> {
                tvEstadoBadge.text   = "Error"
                tvEstadoDetalle.text = "TICKET NO ENCONTRADO"
                tvEstadoDetalle.setTextColor(Color.parseColor("#F44336"))
                btnValidarEntrada.visibility = View.GONE
            }
        }


        btnValidarEntrada.setOnClickListener {
            val idUsuarioControl = SessionManager(this).getIdUsuario()
            val exito = dbHelper.validarEntrada(codigoQR, idUsuarioControl)

            if (exito) {
                Toast.makeText(this, "Ingreso registrado exitosamente", Toast.LENGTH_LONG).show()

                tvEstadoBadge.text   = "Validado"
                tvEstadoDetalle.text = " ENTRADA YA UTILIZADA"
                tvEstadoDetalle.setTextColor(Color.parseColor("#F44336"))
                btnValidarEntrada.visibility = View.GONE
            } else {
                Toast.makeText(this, "Error al validar la entrada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}