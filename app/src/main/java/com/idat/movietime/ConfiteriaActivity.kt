package com.idat.movietime

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class ProductoItem(val id: Int, val nombre: String, val precio: Double, val emoji: String, var cantidad: Int = 0)

class ConfiteriaActivity : AppCompatActivity() {

    private lateinit var recyclerProductos: RecyclerView
    private lateinit var tvTotal:           TextView
    private lateinit var btnSiguiente:      Button
    private lateinit var adapter:           ProductoAdapter

    // ── Extras recibidos ─────────────────────────────────────────
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

    private val listaPopcorn = mutableListOf(
        ProductoItem(1,"POP CORN\nSALADO GIGANTE",21.0,"🍿"),
        ProductoItem(2,"POP CORN\nSALADO GRANDE",15.0,"🍿"),
        ProductoItem(3,"POP CORN\nSALADO MEDIANO",14.0,"🍿"),
        ProductoItem(4,"POP CORN\nSALADO CHICO",13.0,"🍿"),
        ProductoItem(5,"POP CORN\nDULCE GRANDE",15.0,"🍿"),
        ProductoItem(6,"POP CORN\nDULCE CHICO",13.0,"🍿")
    )
    private val listaCombos = mutableListOf(
        ProductoItem(7,"COMBO\nFAMILIAR",45.0,"🎁"),
        ProductoItem(8,"COMBO\nDÚO",35.0,"🎁"),
        ProductoItem(9,"COMBO\nPERSONAL",22.0,"🎁")
    )
    private val listaSandwich = mutableListOf(
        ProductoItem(10,"HOT DOG\nCLÁSICO",12.0,"🌭"),
        ProductoItem(11,"SANDWICH\nPOLLO",15.0,"🥪"),
        ProductoItem(12,"NACHOS\nCHEESE",14.0,"🧀")
    )

    private var productosActuales = listaPopcorn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confiteria)

        recyclerProductos = findViewById(R.id.recyclerProductos)
        tvTotal           = findViewById(R.id.tvTotal)
        btnSiguiente      = findViewById(R.id.btnSiguiente)

        butacas          = intent.getStringExtra("butacas")           ?: ""
        cantidadEntradas = intent.getIntExtra("cantidad_entradas", 1)
        totalEntradas    = intent.getDoubleExtra("total_entradas",    0.0)
        titulo           = intent.getStringExtra("titulo")            ?: ""
        sede             = intent.getStringExtra("sede")              ?: ""
        hora             = intent.getStringExtra("hora")              ?: ""
        sala             = intent.getStringExtra("sala")              ?: ""
        fecha            = intent.getStringExtra("fecha")             ?: ""
        duracion         = intent.getIntExtra("duracion_min",         0)
        clasif           = intent.getStringExtra("clasificacion")     ?: ""

        findViewById<TextView>(R.id.btnAtras)?.setOnClickListener { finish() }

        adapter = ProductoAdapter(productosActuales) { actualizarTotal() }
        recyclerProductos.layoutManager = GridLayoutManager(this, 3)
        recyclerProductos.adapter = adapter

        setupCategorias()
        actualizarTotal()

        btnSiguiente.setOnClickListener {
            val totalConf = todosProductos().sumOf { it.precio * it.cantidad }
            Intent(this, PagoActivity::class.java).also { i ->
                i.putExtra("butacas",           butacas)
                i.putExtra("cantidad_entradas", cantidadEntradas)
                i.putExtra("total_entradas",    totalEntradas)   // ✅ propagado
                i.putExtra("total_confiteria",  totalConf)
                i.putExtra("titulo",            titulo)
                i.putExtra("sede",              sede)
                i.putExtra("hora",              hora)
                i.putExtra("sala",              sala)            // ✅ propagado
                i.putExtra("fecha",             fecha)
                i.putExtra("duracion_min",      duracion)        // ✅ propagado
                i.putExtra("clasificacion",     clasif)          // ✅ propagado
                startActivity(i)
            }
        }
    }

    private fun todosProductos() = listaPopcorn + listaCombos + listaSandwich

    private fun actualizarTotal() {
        val total = todosProductos().sumOf { it.precio * it.cantidad }
        tvTotal.text = "Total: S/ ${"%.2f".format(total)}"
        btnSiguiente.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (total > 0) Color.parseColor("#1A1A2E") else Color.parseColor("#777777")
        )
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
                    val tv = findViewById<TextView>(catId)
                    tv?.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    tv?.setTextColor(Color.parseColor("#444444"))
                }
                (it as TextView).setBackgroundColor(Color.parseColor("#222222"))
                it.setTextColor(Color.WHITE)
                productosActuales = lista
                adapter.updateLista(lista)
            }
        }
    }

    inner class ProductoAdapter(
        private var items: MutableList<ProductoItem>,
        private val onCambio: () -> Unit
    ) : RecyclerView.Adapter<ProductoAdapter.VH>() {

        fun updateLista(nuevos: MutableList<ProductoItem>) {
            items = nuevos
            notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmoji:    TextView = v.findViewById(R.id.tvEmojiProducto)
            val tvNombre:   TextView = v.findViewById(R.id.tvNombreProducto)
            val tvPrecio:   TextView = v.findViewById(R.id.tvPrecio)
            val tvCantidad: TextView = v.findViewById(R.id.tvCantidad)
            val btnMenos:   TextView = v.findViewById(R.id.btnMenos)
            val btnMas:     TextView = v.findViewById(R.id.btnMas)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.tvEmoji.text    = item.emoji
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