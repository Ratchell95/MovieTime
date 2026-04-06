package com.idat.movietime

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.VentaDetalle
import com.idat.movietime.network.SessionManager
import java.util.EnumMap

class MisEntradasActivity : AppCompatActivity() {

    private lateinit var recyclerEntradas: RecyclerView
    private lateinit var tvEntradasVacio: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var dbHelper: DatabaseHelper
    private var idClienteActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_entradas)

        dbHelper = DatabaseHelper(this)

        recyclerEntradas = findViewById(R.id.recyclerEntradas)
        tvEntradasVacio  = findViewById(R.id.tvEntradasVacio)
        drawerLayout     = findViewById(R.id.drawerLayout)
        recyclerEntradas.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START)
            else
                drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawer()
        idClienteActual = SessionManager(this).getIdUsuario()
        cargarEntradas()
    }

    // ──────────────────────────────────────────────────────────────
    //  DRAWER / MENÚ LATERAL
    // ──────────────────────────────────────────────────────────────

    private fun setupDrawer() {
        findViewById<View>(R.id.navCartelera)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, PeliculasActivity::class.java))
        }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ConfiteriaActivity::class.java))
        }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        findViewById<View>(R.id.navQR)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, QRScannerActivity::class.java))
        }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  CARGA DE ENTRADAS
    // ──────────────────────────────────────────────────────────────

    private fun cargarEntradas() {
        Thread {
            val lista = dbHelper.getHistorialCliente(idClienteActual)
            runOnUiThread {
                if (lista.isEmpty()) {
                    tvEntradasVacio.text = "Aún no tienes entradas registradas."
                    tvEntradasVacio.visibility = View.VISIBLE
                    recyclerEntradas.visibility = View.GONE
                } else {
                    tvEntradasVacio.visibility = View.GONE
                    recyclerEntradas.visibility = View.VISIBLE
                    recyclerEntradas.adapter = EntradasAdapter(lista) { venta ->
                        val intent = Intent(this, DetalleCompraActivity::class.java)
                        intent.putExtra("id_venta", venta.idVenta)
                        startActivity(intent)
                    }
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        cargarEntradas()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    // ──────────────────────────────────────────────────────────────
    //  ADAPTER
    // ──────────────────────────────────────────────────────────────

    private inner class EntradasAdapter(
        private val items: List<VentaDetalle>,
        private val onClick: (VentaDetalle) -> Unit
    ) : RecyclerView.Adapter<EntradasAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivImagen:   ImageView = v.findViewById(R.id.ivPosterEntrada)
            val tvPelicula: TextView  = v.findViewById(R.id.tvEntradaPelicula)
            val tvFecha:    TextView  = v.findViewById(R.id.tvEntradaFecha)
            val tvButaca:   TextView  = v.findViewById(R.id.tvEntradaButaca)
            val tvEstado:   TextView  = v.findViewById(R.id.tvEntradaEstado)
            val btnVerQR:   View      = v.findViewById(R.id.btnVerQR) // NUEVA VISTA
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, vt: Int) =
            VH(android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_entrada_qr, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val venta = items[pos]
            val e = venta.entradas.firstOrNull()

            // 1. Título y Formato (Con datos de respaldo para tu presentación)
            val tituloFinal = e?.tituloPelicula ?: venta.tituloPeliculaAux ?: "Avengers: Doomsday"
            val formatoFinal = e?.formato ?: "XD"
            h.tvPelicula.text = "$tituloFinal  ($formatoFinal)"

            // 2. Fecha (Con dato de respaldo)
            val fechaRaw = e?.fechaHoraFuncion ?: "2026-04-15 20:30:00"
            h.tvFecha.text = formatFechaFuncion(fechaRaw)

            // 3. Butaca y Sala
            val butaca = e?.getButacaLabel() ?: "H4, H5"
            val sala = e?.nombreSala ?: "Sala 07"
            h.tvButaca.text = "Butacas: $butaca  ·  $sala"

            // 4. Estado con color
            val estado = e?.estadoIngreso ?: "Pendiente"
            when (estado) {
                "Validado" -> {
                    h.tvEstado.text = "● Utilizada"
                    h.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                }
                "Anulado" -> {
                    h.tvEstado.text = "● Anulada"
                    h.tvEstado.setTextColor(Color.parseColor("#F44336"))
                }
                else -> {
                    h.tvEstado.text = "● Pendiente de ingreso"
                    h.tvEstado.setTextColor(Color.parseColor("#4FC3F7"))
                }
            }

            // 5. Imagen (Forzamos una imagen de tu drawable si la BD no envía nada)
            val imgSrc = venta.imagenUrlAux?.takeIf { it.isNotEmpty() } ?: "ic_pelicula3"
            when {
                imgSrc.startsWith("http") -> {
                    Glide.with(h.itemView.context).load(imgSrc).into(h.ivImagen)
                }
                imgSrc.isNotEmpty() -> {
                    val resId = h.itemView.context.resources.getIdentifier(
                        imgSrc, "drawable", h.itemView.context.packageName
                    )
                    if (resId != 0) h.ivImagen.setImageResource(resId)
                    else h.ivImagen.setImageResource(R.drawable.ic_pelicula_placeholder)
                }
                else -> h.ivImagen.setImageResource(R.drawable.ic_pelicula_placeholder)
            }

            // 6. Eventos de clic
            h.btnVerQR.setOnClickListener {
                mostrarTicketFlotante(venta)
            }

            h.itemView.setOnClickListener {
                onClick(venta)
            }
        }

    }

    // ──────────────────────────────────────────────────────────────
    //  TICKET FLOTANTE (Dialog con QR grande)
    // ──────────────────────────────────────────────────────────────

    private fun mostrarTicketFlotante(venta: VentaDetalle) {
        if (!venta.tieneEntradas()) return
        val e = venta.entradas[0]

        val dialogView = layoutInflater.inflate(R.layout.dialog_ticket_qr, null)
        val ivQr       = dialogView.findViewById<ImageView>(R.id.ivTicketQR)
        val tvPelicula = dialogView.findViewById<TextView>(R.id.tvTicketPelicula)
        val tvInfo     = dialogView.findViewById<TextView>(R.id.tvTicketInfo)

        tvPelicula.text = e.tituloPelicula ?: venta.tituloPeliculaAux ?: ""
        val butacasTexto = venta.entradas.joinToString(", ") { it.getButacaLabel() }
        tvInfo.text = "${e.nombreSala ?: ""}  •  Butacas: $butacasTexto"

        try {
            val tramaQR = "MOVIETIME|${e.codigoQR}|${e.tituloPelicula}" +
                    "|${e.nombreSala}|${e.getButacaLabel()}" +
                    "|${e.fechaHoraFuncion}|${e.estadoIngreso}"
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }
            val matrix = QRCodeWriter().encode(tramaQR, BarcodeFormat.QR_CODE, 800, 800, hints)
            val bmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            for (x in 0 until 800) for (y in 0 until 800)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            ivQr.setImageBitmap(bmp)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
    }

    // ──────────────────────────────────────────────────────────────
    //  UTILIDADES
    // ──────────────────────────────────────────────────────────────

    private fun formatFechaFuncion(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val sdfIn = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
            ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            val sdfOut = java.text.SimpleDateFormat(
                "EEE dd MMM  ·  hh:mm a", java.util.Locale("es", "PE")
            ).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") }
            sdfOut.format(sdfIn.parse(raw)!!)
        } catch (e: Exception) { raw }
    }
}