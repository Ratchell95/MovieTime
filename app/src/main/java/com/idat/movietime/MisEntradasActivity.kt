package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapter.EntradasAdapter
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.SessionManager

class MisEntradasActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_entradas)

        drawerLayout = findViewById(R.id.drawerLayout)
        setupDrawer()
        cargarEntradas()
    }

    private fun cargarEntradas() {
        val dbHelper  = DatabaseHelper(this)
        val idCliente = SessionManager(this).getIdUsuario()


        val listaEntradas = dbHelper.getMisEntradas(idCliente)
            .filter { it.tieneEntradas() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerEntradas)
        val tvVacio      = findViewById<TextView>(R.id.tvEntradasVacio)

        if (listaEntradas.isEmpty()) {
            tvVacio?.visibility    = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            tvVacio?.visibility    = View.GONE
            recyclerView?.visibility = View.VISIBLE
            recyclerView?.layoutManager = LinearLayoutManager(this)
            recyclerView?.adapter = EntradasAdapter(listaEntradas) { venta ->
                val intent = Intent(this, DetalleCompraActivity::class.java)
                intent.putExtra("id_venta", venta.idVenta)
                startActivity(intent)
            }
        }
    }

    private fun setupDrawer() {
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navCartelera)?.setOnClickListener {
            startActivity(Intent(this, PeliculasActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener {
            startActivity(Intent(this, ConfiteriaActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navQR)?.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            SessionManager(this).cerrarSesion()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}