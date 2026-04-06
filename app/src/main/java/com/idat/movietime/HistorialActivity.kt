package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapter.HistorialAdapter
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.SessionManager

class HistorialActivity : AppCompatActivity() {

    private lateinit var recyclerHistorial: RecyclerView
    private lateinit var tvHistorialVacio: TextView
    private lateinit var drawerLayout: DrawerLayout
    // FIX: inicializar como lateinit, igual que DetalleCompraActivity
    private lateinit var dbHelper: DatabaseHelper
    private var idClienteActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // FIX: inicializar en onCreate, nunca dentro de un Thread
        dbHelper = DatabaseHelper(this)

        recyclerHistorial = findViewById(R.id.recyclerHistorial)
        tvHistorialVacio  = findViewById(R.id.tvHistorialVacio)
        drawerLayout      = findViewById(R.id.drawerLayout)
        recyclerHistorial.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawer()
        idClienteActual = SessionManager(this).getIdUsuario()
        cargarHistorial()
    }

    private fun setupDrawer() {
        findViewById<View>(R.id.navCartelera)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, PeliculasActivity::class.java)) }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener    { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, MisEntradasActivity::class.java)) }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener  { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, ConfiteriaActivity::class.java)) }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START) }
        findViewById<View>(R.id.navQR)?.setOnClickListener          { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, QRScannerActivity::class.java)) }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            com.idat.movietime.network.SessionManager(this).cerrarSesion()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun cargarHistorial() {
        Thread {
            val lista = dbHelper.getHistorialCliente(idClienteActual)

            runOnUiThread {
                if (lista.isEmpty()) {
                    tvHistorialVacio.visibility  = View.VISIBLE
                    recyclerHistorial.visibility = View.GONE
                } else {
                    tvHistorialVacio.visibility  = View.GONE
                    recyclerHistorial.visibility = View.VISIBLE

                    recyclerHistorial.adapter = HistorialAdapter(lista) { venta ->
                        // FIX: id_venta es el único extra OBLIGATORIO.
                        // DetalleCompraActivity lo usa para consultar la BD completa.
                        // Los extras adicionales son solo para mostrar algo mientras carga.
                        val intent = Intent(this, DetalleCompraActivity::class.java)

                        // ── OBLIGATORIO ──────────────────────────────────────
                        intent.putExtra("id_venta", venta.idVenta)

                        // ── Datos de pago (fallback mientras carga la BD) ────
                        intent.putExtra("metodo_pago",      venta.metodoPago      ?: "")
                        intent.putExtra("tipo_comprobante", venta.tipoComprobante ?: "Boleta")
                        intent.putExtra("gran_total",       venta.total)
                        intent.putExtra("total_entradas",   venta.subtotal)
                        intent.putExtra("descuento",        venta.descuento)

                        startActivity(intent)
                    }
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}