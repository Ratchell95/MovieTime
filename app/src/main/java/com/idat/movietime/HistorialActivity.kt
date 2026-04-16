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
import com.idat.movietime.adapter.HistorialAdapter
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.network.SessionManager

class HistorialActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        drawerLayout = findViewById(R.id.drawerLayout)
        setupDrawer()
        cargarHistorial()
    }

    private fun cargarHistorial() {
        val dbHelper = DatabaseHelper(this)
        val idCliente = SessionManager(this).getIdUsuario()
        val listaHistorial = dbHelper.getHistorialCliente(idCliente)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerHistorial)
        val tvVacio = findViewById<TextView>(R.id.tvHistorialVacio)

        if (listaHistorial.isEmpty()) {
            tvVacio?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            tvVacio?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            recyclerView?.layoutManager = LinearLayoutManager(this)
            recyclerView?.adapter = HistorialAdapter(listaHistorial) { venta ->
                val intent = Intent(this, DetalleCompraActivity::class.java)
                intent.putExtra("id_venta", venta.idVenta)
                startActivity(intent)
            }
        }
    }

    private fun setupDrawer() {

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navCartelera)?.setOnClickListener {
            startActivity(Intent(this, PeliculasActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            startActivity(Intent(this, MisEntradasActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
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