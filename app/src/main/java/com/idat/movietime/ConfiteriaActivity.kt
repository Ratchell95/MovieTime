package com.idat.movietime

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.idat.movietime.network.RetrofitClient


// Este modelo leerá la información que viene de tu Spring Boot
data class ProductoItem(
    val id:        Int,
    val nombre:    String,
    var precio:    Double,
    val imagenRes: Int,
    var cantidad:  Int = 0
)
class ConfiteriaActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: RecyclerView
    private lateinit var tvTotal:           TextView
    private lateinit var btnSiguiente:      Button
    private lateinit var adapter:           ProductoAdapter
    private lateinit var drawerLayout:      DrawerLayout

    private var butacas          = ""
    private var cantidadEntradas = 1
    private var totalEntradas    = 0.0
    private var titulo           = ""
    private var sede             = ""
    private var hora             = ""
    private var sala             = ""
    private var fecha            = ""
    private var duracion         = 0
    private var clasif           = ""
    private var idFuncion        = 0

    private val listaPopcorn = mutableListOf(
        ProductoItem(1, "POP CORN\nSALADO GIGANTE", 21.0, R.drawable.popcorn_sl_g),
        ProductoItem(2, "POP CORN\nSALADO GRANDE",  15.0, R.drawable.popcorn_sl_gr),
        ProductoItem(3, "POP CORN\nSALADO MEDIANO", 14.0, R.drawable.popcorn_sl_m),
        ProductoItem(4, "POP CORN\nSALADO CHICO",   13.0, R.drawable.popcorn_sl_ch),
        ProductoItem(5, "POP CORN\nDULCE GRANDE",   15.0, R.drawable.popcorn_sl_gr),
        ProductoItem(6, "POP CORN\nDULCE CHICO",    13.0, R.drawable.popcorn_sl_ch)
    )
    private val listaCombos = mutableListOf(
        ProductoItem(7,  "COMBO\nFAMILIAR", 45.0, R.drawable.combo1),
        ProductoItem(8,  "COMBO\nDÚO",      35.0, R.drawable.combo2),
        ProductoItem(9,  "COMBO\nHOT DOG",  28.0, R.drawable.combo3),
        ProductoItem(10, "COMBO\nPERSONAL DULCE", 22.0, R.drawable.combo4)
    )
    private val listaSandwich = mutableListOf(
        ProductoItem(11, "HOT DOG\nCLÁSICO", 12.0, R.drawable.hotdog),
        ProductoItem(12, "NACHOS\nCHEESE",   14.0, R.drawable.nachos),
        ProductoItem(13, "SANDWICH\nPOLLO",  15.0, R.drawable.sandwich)
    )

    private var productosActuales = listaPopcorn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confiteria)

        drawerLayout      = findViewById(R.id.drawerLayout)
        recyclerProductos = findViewById(R.id.recyclerProductos)
        tvTotal           = findViewById(R.id.tvTotal)
        btnSiguiente      = findViewById(R.id.btnSiguiente)

        butacas          = intent.getStringExtra("butacas")        ?: ""
        cantidadEntradas = intent.getIntExtra("cantidad_entradas", 1)
        totalEntradas    = intent.getDoubleExtra("total_entradas", 0.0)
        titulo           = intent.getStringExtra("titulo")         ?: ""
        sede             = intent.getStringExtra("sede")           ?: ""
        hora             = intent.getStringExtra("hora")           ?: ""
        sala             = intent.getStringExtra("sala")           ?: ""
        fecha            = intent.getStringExtra("fecha")          ?: ""
        duracion         = intent.getIntExtra("duracion_min",      0)
        clasif           = intent.getStringExtra("clasificacion")  ?: ""
        idFuncion        = intent.getIntExtra("id_funcion",        0)

        // btnAtras (flujo de compra)
        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }

        adapter = ProductoAdapter(productosActuales) { actualizarTotal() }
        recyclerProductos.layoutManager = GridLayoutManager(this, 3)
        recyclerProductos.adapter = adapter

        setupCategorias()
        setupDrawer()
        actualizarTotal()
        descargarPreciosReales()
        btnSiguiente.setOnClickListener {
            val totalConf = todosProductos().sumOf { it.precio * it.cantidad }
            val granTotal = totalEntradas + totalConf
            startActivity(Intent(this, PagoActivity::class.java).apply {
                putExtra("butacas",           butacas)
                putExtra("cantidad_entradas", cantidadEntradas)
                putExtra("total_entradas",    totalEntradas)
                putExtra("total_confiteria",  totalConf)
                putExtra("gran_total",        granTotal)   // ← total real para PagoActivity
                putExtra("titulo",            titulo)
                putExtra("sede",              sede)
                putExtra("hora",              hora)
                putExtra("sala",              sala)
                putExtra("fecha",             fecha)
                putExtra("duracion_min",      duracion)
                putExtra("clasificacion",     clasif)
                putExtra("id_funcion",        idFuncion)
            })
        }
    }
    private fun todosProductos() = listaPopcorn + listaCombos + listaSandwich

    private fun actualizarTotal() {
        val totalConf = todosProductos().sumOf { it.precio * it.cantidad }
        val granTotal = totalEntradas + totalConf
        tvTotal.text = "Total: S/ ${"%.2f".format(granTotal)}"
        // Confitería es opcional → btnSiguiente siempre habilitado
        btnSiguiente.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor("#1A1A2E")
        )
        btnSiguiente.isEnabled = true
    }

    private fun setupCategorias() {
        val catMap = mapOf(
            R.id.catPopcorn  to listaPopcorn,
            R.id.catCombos   to listaCombos,
            R.id.catSandwich to listaSandwich
        )
        catMap.forEach { (id, lista) ->
            findViewById<TextView>(id)?.setOnClickListener {
                catMap.keys.forEach { catId ->
                    findViewById<TextView>(catId)?.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    findViewById<TextView>(catId)?.setTextColor(Color.parseColor("#444444"))
                }
                (it as TextView).setBackgroundColor(Color.parseColor("#222222"))
                it.setTextColor(Color.WHITE)
                productosActuales = lista
                adapter.updateLista(lista)
            }
        }
    }

    private fun setupDrawer() {
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navCartelera)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, PeliculasActivity::class.java)) }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener    { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener  { drawerLayout.closeDrawer(GravityCompat.START) }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navQR)?.setOnClickListener         { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, QRScannerActivity::class.java)) }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)

            com.idat.movietime.network.SessionManager(this).cerrarSesion()

            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    // ──────────────────────────────────────────────────────────────
    // LA MAGIA: DESCARGAR PRECIOS EN TIEMPO REAL DESDE MYSQL
    // ──────────────────────────────────────────────────────────────
    private fun descargarPreciosReales() {
        lifecycleScope.launch {
            try {
                // 1. Usamos la ruta que YA EXISTÍA en tu MovieTimeApi.kt
                val response = RetrofitClient.api.getProductosActivos()

                if (response.isSuccessful && response.body()?.success == true) {

                    // Kotlin entiende esto perfectamente porque es un Map nativo
                    val listaMapas = response.body()?.data

                    if (listaMapas != null) {
                        for (mapa in listaMapas) {
                            // Extraemos los datos (Retrofit convierte los JSON numbers a Double)
                            val idDouble = (mapa["idProducto"] ?: mapa["id_producto"]) as? Double
                            val precioNuevo = mapa["precio"] as? Double

                            if (idDouble != null && precioNuevo != null) {
                                val id = idDouble.toInt()

                                // Buscamos en tus listas locales y actualizamos
                                val itemLocal = todosProductos().find { it.id == id }
                                if (itemLocal != null) {
                                    itemLocal.precio = precioNuevo
                                }
                            }
                        }

                        // 3. Refrescamos la pantalla
                        adapter.notifyDataSetChanged()
                        actualizarTotal()
                    }
                }
            } catch (e: Exception) {
                // Si falla el internet, se mantienen los precios de emergencia
                e.printStackTrace()
            }
        }
    }

    inner class ProductoAdapter(
        private var items: MutableList<ProductoItem>,
        private val onCambio: () -> Unit
    ) : RecyclerView.Adapter<ProductoAdapter.VH>() {

        fun updateLista(nuevos: MutableList<ProductoItem>) { items = nuevos; notifyDataSetChanged() }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivProducto: ImageView = v.findViewById(R.id.ivProducto)
            val tvNombre:   TextView  = v.findViewById(R.id.tvNombreProducto)
            val tvPrecio:   TextView  = v.findViewById(R.id.tvPrecio)
            val tvCantidad: TextView  = v.findViewById(R.id.tvCantidad)
            val btnMenos:   TextView  = v.findViewById(R.id.btnMenos)
            val btnMas:     TextView  = v.findViewById(R.id.btnMas)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.ivProducto.setImageResource(item.imagenRes)
            h.tvNombre.text   = item.nombre
            h.tvPrecio.text   = "S/${item.precio.toInt()}.00"
            h.tvCantidad.text = item.cantidad.toString()
            h.btnMenos.setOnClickListener {
                if (item.cantidad > 0) { item.cantidad--; h.tvCantidad.text = item.cantidad.toString(); onCambio() }
            }
            h.btnMas.setOnClickListener {
                item.cantidad++; h.tvCantidad.text = item.cantidad.toString(); onCambio()
            }
        }
        override fun getItemCount() = items.size
    }
}