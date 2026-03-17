package com.idat.movietime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

data class CompraHistorial(
    val titulo:      String,
    val total:       Double,
    val metodoPago:  String,
    val comprobante: String,
    val codigoQR:    String,
    val butacas:     String,
    val sede:        String,
    val fecha:       String,
    val hora:        String,
    val fechaCompra: String
)

class HistorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        findViewById<TextView>(R.id.btnAtrasHistorial)?.setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.recyclerHistorial)
        val tvVacio  = findViewById<TextView>(R.id.tvHistorialVacio)
        val compras  = cargarCompras()

        if (compras.isEmpty()) {
            tvVacio.visibility  = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvVacio.visibility  = View.GONE
            recycler.visibility = View.VISIBLE
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = HistorialAdapter(compras)
        }
    }

    private fun cargarCompras(): List<CompraHistorial> {
        val prefs   = getSharedPreferences("movietime_historial", MODE_PRIVATE)
        val jsonStr = prefs.getString("compras", "[]") ?: "[]"
        val array   = try { JSONArray(jsonStr) } catch (e: Exception) { JSONArray() }
        val lista   = mutableListOf<CompraHistorial>()

        for (i in array.length() - 1 downTo 0) {
            val obj = array.getJSONObject(i)
            lista.add(CompraHistorial(
                titulo      = obj.optString("titulo"),
                total       = obj.optDouble("total", 0.0),
                metodoPago  = obj.optString("metodoPago"),
                comprobante = obj.optString("comprobante"),
                codigoQR    = obj.optString("codigoQR"),
                butacas     = obj.optString("butacas"),
                sede        = obj.optString("sede"),
                fecha       = obj.optString("fecha"),
                hora        = obj.optString("hora"),
                fechaCompra = obj.optString("fechaCompra")
            ))
        }
        return lista
    }

    inner class HistorialAdapter(private val items: List<CompraHistorial>) :
        RecyclerView.Adapter<HistorialAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitulo:      TextView = v.findViewById(R.id.tvHistTitulo)
            val tvSede:        TextView = v.findViewById(R.id.tvHistSede)
            val tvFechaHora:   TextView = v.findViewById(R.id.tvHistFechaHora)
            val tvButacas:     TextView = v.findViewById(R.id.tvHistButacas)
            val tvTotal:       TextView = v.findViewById(R.id.tvHistTotal)
            val tvMetodo:      TextView = v.findViewById(R.id.tvHistMetodo)
            val tvCodigo:      TextView = v.findViewById(R.id.tvHistCodigo)
            val tvFechaCompra: TextView = v.findViewById(R.id.tvHistFechaCompra)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val c = items[pos]
            h.tvTitulo.text      = c.titulo
            h.tvSede.text        = c.sede
            h.tvFechaHora.text   = "${c.fecha}  ${c.hora}"
            h.tvButacas.text     = "Butacas: ${c.butacas}"
            h.tvTotal.text       = "S/ ${"%.2f".format(c.total)}"
            h.tvMetodo.text      = "${c.metodoPago} · ${c.comprobante}"
            h.tvCodigo.text      = "Cód: ${c.codigoQR}"
            h.tvFechaCompra.text = "Comprado el ${c.fechaCompra}"
        }

        override fun getItemCount() = items.size
    }
}