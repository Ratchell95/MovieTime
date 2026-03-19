package com.idat.movietime

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapters.HistorialAdapter
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.VentaDetalle
import com.idat.movietime.model.VentaDetalle.EntradaItem
import com.idat.movietime.network.SessionManager

class HistorialActivity : AppCompatActivity() {

    private lateinit var recyclerHistorial: RecyclerView
    private lateinit var tvHistorialVacio: TextView
    private var dbHelper: DatabaseHelper? = null
    private var idClienteActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        recyclerHistorial = findViewById(R.id.recyclerHistorial)
        tvHistorialVacio  = findViewById(R.id.tvHistorialVacio)
        recyclerHistorial.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnAtras)?.setOnClickListener { finish() }

        idClienteActual = SessionManager(this).getIdUsuario()

        cargarHistorial()
    }

    private fun cargarHistorial() {
        dbHelper = DatabaseHelper(this)
        val lista: List<VentaDetalle> = obtenerVentasCliente(idClienteActual)

        if (lista.isEmpty()) {
            tvHistorialVacio.visibility  = View.VISIBLE
            recyclerHistorial.visibility = View.GONE
        } else {
            tvHistorialVacio.visibility  = View.GONE
            recyclerHistorial.visibility = View.VISIBLE
            recyclerHistorial.adapter = HistorialAdapter(lista) { venta ->
                val intent = Intent(this, DetalleCompraActivity::class.java)
                intent.putExtra("id_venta", venta.idVenta)
                startActivity(intent)
            }
        }
    }

    private fun obtenerVentasCliente(idCliente: Int): List<VentaDetalle> {
        val resultado = mutableListOf<VentaDetalle>()
        val db: SQLiteDatabase = dbHelper!!.readableDatabase

        val sql =
            "SELECT v.id_venta, v.fecha_venta, v.total, v.subtotal, v.descuento, " +
                    "       v.tipo_comprobante, v.metodo_pago, v.estado, " +
                    "       de.id_detalle_entrada, de.codigo_qr, de.estado_ingreso, de.precio_unitario, " +
                    "       f.fecha_hora, p.titulo, s.nombre AS nombre_sala, b.fila, b.numero " +
                    "FROM ventas v " +
                    "LEFT JOIN detalle_entradas de ON de.id_venta   = v.id_venta " +
                    "LEFT JOIN funciones        f  ON f.id_funcion  = de.id_funcion " +
                    "LEFT JOIN peliculas        p  ON p.id_pelicula = f.id_pelicula " +
                    "LEFT JOIN butacas          b  ON b.id_butaca   = de.id_butaca " +
                    "LEFT JOIN salas            s  ON s.id_sala     = b.id_sala " +
                    "WHERE v.id_cliente = ? " +
                    "ORDER BY v.fecha_venta DESC"

        val cursor = db.rawQuery(sql, arrayOf(idCliente.toString()))

        var ultimoIdVenta = -1
        var ventaActual: VentaDetalle? = null

        while (cursor.moveToNext()) {
            val idVenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_venta"))

            if (idVenta != ultimoIdVenta) {
                ventaActual = VentaDetalle().apply {
                    this.idVenta         = idVenta
                    this.fechaVenta      = cursor.getString(cursor.getColumnIndexOrThrow("fecha_venta"))
                    this.total           = cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
                    this.subtotal        = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal"))
                    this.descuento       = cursor.getDouble(cursor.getColumnIndexOrThrow("descuento"))
                    this.tipoComprobante = cursor.getString(cursor.getColumnIndexOrThrow("tipo_comprobante"))
                    this.metodoPago      = cursor.getString(cursor.getColumnIndexOrThrow("metodo_pago"))
                    this.estadoVenta     = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                    this.entradas        = ArrayList()
                }
                resultado.add(ventaActual)
                ultimoIdVenta = idVenta
            }

            if (!cursor.isNull(cursor.getColumnIndexOrThrow("id_detalle_entrada"))) {
                val entrada = EntradaItem().apply {
                    tituloPelicula   = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                    fechaHoraFuncion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora"))
                    nombreSala       = cursor.getString(cursor.getColumnIndexOrThrow("nombre_sala"))
                    fila             = cursor.getString(cursor.getColumnIndexOrThrow("fila"))
                    numero           = cursor.getInt(cursor.getColumnIndexOrThrow("numero"))
                    precioUnitario   = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_unitario"))
                    codigoQR         = cursor.getString(cursor.getColumnIndexOrThrow("codigo_qr"))
                }
                ventaActual?.entradas?.add(entrada)
            }
        }

        cursor.close()
        db.close()
        return resultado
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper?.close()
    }
}