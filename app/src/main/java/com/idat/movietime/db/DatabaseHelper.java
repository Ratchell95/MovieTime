package com.idat.movietime.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.idat.movietime.model.Pelicula;
import com.idat.movietime.model.VentaDetalle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "db_movietime.db";
    private static final int DB_VERSION = 30;
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String TABLE_PELICULAS = "peliculas";
    public static final String TABLE_HISTORIAL_ACCESOS = "historial_accesos";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = OFF;");
        db.execSQL("CREATE TABLE IF NOT EXISTS roles (id_rol INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT UNIQUE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (id_usuario INTEGER PRIMARY KEY AUTOINCREMENT, nombres TEXT, apellidos TEXT, email TEXT UNIQUE, documento TEXT UNIQUE, password_hash TEXT, id_rol INTEGER, estado TEXT, rol TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS peliculas (id_pelicula INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT UNIQUE, duracion_min INTEGER, genero TEXT, formato TEXT, imagen_url TEXT, estado TEXT, clasificacion TEXT, sinopsis TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS salas (id_sala INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, tipo_sala TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS funciones (id_funcion INTEGER PRIMARY KEY AUTOINCREMENT, id_pelicula INTEGER, id_sala INTEGER, fecha_hora TEXT, precio_base REAL, aforo_disponible INTEGER, tipo_funcion TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS butacas (id_butaca INTEGER PRIMARY KEY AUTOINCREMENT, id_sala INTEGER, fila TEXT, numero INTEGER, tipo TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ventas (id_venta INTEGER PRIMARY KEY AUTOINCREMENT, id_cliente INTEGER, id_funcion INTEGER, subtotal REAL, descuento REAL, total REAL, tipo_comprobante TEXT, metodo_pago TEXT, canal_venta TEXT, id_promocion INTEGER, fecha_venta TEXT, estado TEXT, titulo_fb TEXT, sala_fb TEXT, butacas_fb TEXT, fecha_fb TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS detalle_entradas (id_detalle_entrada INTEGER PRIMARY KEY AUTOINCREMENT, id_venta INTEGER, id_funcion INTEGER, id_butaca INTEGER, precio_unitario REAL, codigo_qr TEXT, estado_ingreso TEXT, fecha_validacion TEXT, id_validado_por INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS detalle_confiteria (id_detalle_confiteria INTEGER PRIMARY KEY AUTOINCREMENT, id_venta INTEGER, id_producto INTEGER, cantidad INTEGER, precio_unitario REAL, subtotal REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS productos (id_producto INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT UNIQUE, stock_actual INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS historial_accesos (id_acceso INTEGER PRIMARY KEY AUTOINCREMENT, id_usuario INTEGER, dispositivo TEXT, resultado TEXT)");

        insertarDatosSemilla(db);
    }

    private void insertarDatosSemilla(SQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO roles (nombre) VALUES ('Administrador'), ('Control'), ('Cliente')");
        db.execSQL("INSERT OR IGNORE INTO peliculas (id_pelicula, titulo, duracion_min, genero, formato, estado) VALUES " +
                "(1, 'Iron Lung: Océano de Sangre', 110, 'Terror', '2D', 'Activa'), " +
                "(2, 'Espía Entre Animales', 95, 'Animación', '2D', 'Activa'), " +
                "(3, 'Avengers: Doomsday', 150, 'Acción', 'XD', 'Activa'), " +
                "(4, 'Minecraft: La Película', 105, 'Aventura', '3D', 'Activa')");
        db.execSQL("INSERT OR IGNORE INTO salas (id_sala, nombre, tipo_sala) VALUES (1, 'Sala 1', '2D'), (2, 'Sala 2', 'XD'), (3, 'Sala 3', '3D')");
        db.execSQL("INSERT OR IGNORE INTO funciones (id_funcion, id_pelicula, id_sala, fecha_hora, precio_base, aforo_disponible) VALUES " +
                "(1, 4, 1, '2026-04-16 18:00:00', 18.0, 50), " +
                "(2, 4, 1, '2026-04-16 21:00:00', 18.0, 50), " +
                "(3, 3, 2, '2026-04-16 20:00:00', 25.0, 100)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS historial_accesos");
        db.execSQL("DROP TABLE IF EXISTS detalle_entradas");
        db.execSQL("DROP TABLE IF EXISTS detalle_confiteria");
        db.execSQL("DROP TABLE IF EXISTS ventas");
        db.execSQL("DROP TABLE IF EXISTS funciones");
        db.execSQL("DROP TABLE IF EXISTS peliculas");
        db.execSQL("DROP TABLE IF EXISTS salas");
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS roles");
        onCreate(db);
    }


    public double aplicarPromocion(String codigo, double totalActual) {
        if ("PROMOYAPE".equalsIgnoreCase(codigo.trim())) {
            return 5.00; // Descuento de S/5.00
        }
        return -1.0; // Código inválido
    }

    public long insertarVentaCompleta(int idCliente, int idFuncion, List<Integer> idButacas, double precioUnitario, List<String> codigosQR, List<VentaDetalle.ConfiteriaItem> productosConfit, double subtotal, double descuento, double total, String tipoComprobante, String metodoPago, Integer idPromocion) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long idVenta = -1;
        try {
            ContentValues cvVenta = new ContentValues();
            cvVenta.put("id_cliente", idCliente);
            cvVenta.put("id_funcion", idFuncion > 0 ? idFuncion : 1);
            cvVenta.put("canal_venta", "App");
            cvVenta.put("subtotal", subtotal);
            cvVenta.put("descuento", descuento);
            cvVenta.put("total", total);
            cvVenta.put("tipo_comprobante", tipoComprobante);
            cvVenta.put("metodo_pago", metodoPago);
            cvVenta.put("estado", "Pagada");
            cvVenta.put("fecha_venta", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            idVenta = db.insertOrThrow("ventas", null, cvVenta);

            if (idButacas == null || idButacas.isEmpty()) {
                idButacas = new ArrayList<>(); idButacas.add(0);
            }
            if (codigosQR == null || codigosQR.isEmpty()) {
                codigosQR = new ArrayList<>(); codigosQR.add("MT-" + System.currentTimeMillis());
            }

            for (int i = 0; i < idButacas.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("id_venta", idVenta);
                cv.put("id_funcion", idFuncion);
                cv.put("id_butaca", idButacas.get(i));
                cv.put("precio_unitario", precioUnitario);
                cv.put("codigo_qr", i < codigosQR.size() ? codigosQR.get(i) : codigosQR.get(0));
                cv.put("estado_ingreso", "Pendiente");
                db.insertOrThrow("detalle_entradas", null, cv);
            }

            if (productosConfit != null) {
                for (VentaDetalle.ConfiteriaItem item : productosConfit) {
                    ContentValues cv = new ContentValues();
                    cv.put("id_venta", idVenta);
                    cv.put("id_producto", item.idProducto);
                    cv.put("cantidad", item.cantidad);
                    cv.put("precio_unitario", item.precioUnitario);
                    cv.put("subtotal", item.subtotal);
                    db.insertOrThrow("detalle_confiteria", null, cv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return idVenta;
    }

    public void actualizarFallbackVenta(int idVenta, String titulo, String sala, String butacas, String fechaHora) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("titulo_fb", titulo);
        cv.put("sala_fb", sala);
        cv.put("butacas_fb", butacas);
        cv.put("fecha_fb", fechaHora);
        db.update("ventas", cv, "id_venta = ?", new String[]{String.valueOf(idVenta)});
    }

    public List<VentaDetalle> getHistorialCliente(int idCliente) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT v.*, de.codigo_qr, de.estado_ingreso, " +
                "COALESCE(NULLIF(v.titulo_fb, ''), p.titulo, 'Cine') AS titulo_real, " +
                "COALESCE(NULLIF(v.sala_fb, ''), s.nombre, 'Sala') AS sala_real, " +
                "COALESCE(NULLIF(v.butacas_fb, ''), '') AS butacas_real, " +
                "COALESCE(NULLIF(v.fecha_fb, ''), f.fecha_hora, '') AS fecha_real " +
                "FROM ventas v " +
                "LEFT JOIN funciones f ON v.id_funcion = f.id_funcion " +
                "LEFT JOIN peliculas p ON f.id_pelicula = p.id_pelicula " +
                "LEFT JOIN salas s ON f.id_sala = s.id_sala " +
                "LEFT JOIN detalle_entradas de ON v.id_venta = de.id_venta " +
                "WHERE v.id_cliente = ? ORDER BY v.id_venta DESC";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(idCliente)});
        while (c.moveToNext()) {
            VentaDetalle v = new VentaDetalle();
            v.idVenta = c.getInt(c.getColumnIndexOrThrow("id_venta"));
            v.total = c.getDouble(c.getColumnIndexOrThrow("total"));
            v.fechaVenta = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
            v.estadoVenta = c.getString(c.getColumnIndexOrThrow("estado"));
            v.tituloPeliculaAux = c.getString(c.getColumnIndexOrThrow("titulo_real"));

            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.tituloPelicula = v.tituloPeliculaAux;
            e.nombreSala = c.getString(c.getColumnIndexOrThrow("sala_real"));
            e.fila = c.getString(c.getColumnIndexOrThrow("butacas_real"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_real"));
            e.codigoQR = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));

            boolean existe = false;
            for (VentaDetalle vEx : lista) {
                if (vEx.idVenta == v.idVenta) {
                    vEx.entradas.add(e);
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                v.entradas.add(e);
                lista.add(v);
            }
        }
        c.close();
        return lista;
    }


    public List<VentaDetalle> getMisEntradas(int idCliente) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();


        String sql =
                "SELECT v.*, de.codigo_qr, de.estado_ingreso, " +
                        "COALESCE(NULLIF(v.titulo_fb, ''), p.titulo, 'Cine') AS titulo_real, " +
                        "COALESCE(NULLIF(v.sala_fb,   ''), s.nombre, 'Sala') AS sala_real, " +
                        "COALESCE(NULLIF(v.butacas_fb,''), '')                AS butacas_real, " +
                        "COALESCE(NULLIF(v.fecha_fb,  ''), f.fecha_hora, '')  AS fecha_real " +
                        "FROM ventas v " +
                        "INNER JOIN detalle_entradas de ON v.id_venta = de.id_venta " +  // INNER = solo con entradas
                        "LEFT JOIN funciones f   ON v.id_funcion    = f.id_funcion " +
                        "LEFT JOIN peliculas p   ON f.id_pelicula   = p.id_pelicula " +
                        "LEFT JOIN salas     s   ON f.id_sala        = s.id_sala " +
                        "WHERE v.id_cliente = ? " +
                        "ORDER BY v.id_venta DESC";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idCliente) });
        while (c.moveToNext()) {
            VentaDetalle v = new VentaDetalle();
            v.idVenta         = c.getInt(c.getColumnIndexOrThrow("id_venta"));
            v.total           = c.getDouble(c.getColumnIndexOrThrow("total"));
            v.fechaVenta      = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
            v.estadoVenta     = c.getString(c.getColumnIndexOrThrow("estado"));
            v.metodoPago      = c.getString(c.getColumnIndexOrThrow("metodo_pago"));
            v.tipoComprobante = c.getString(c.getColumnIndexOrThrow("tipo_comprobante"));
            v.tituloPeliculaAux = c.getString(c.getColumnIndexOrThrow("titulo_real"));

            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.tituloPelicula  = v.tituloPeliculaAux;
            e.nombreSala      = c.getString(c.getColumnIndexOrThrow("sala_real"));
            e.fila            = c.getString(c.getColumnIndexOrThrow("butacas_real"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_real"));
            e.codigoQR        = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso   = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));


            boolean existe = false;
            for (VentaDetalle vEx : lista) {
                if (vEx.idVenta == v.idVenta) {
                    vEx.entradas.add(e);
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                v.entradas.add(e);
                lista.add(v);
            }
        }
        c.close();
        return lista;
    }
    public VentaDetalle getDetalleVenta(int idVenta) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT v.*, de.codigo_qr, de.estado_ingreso, " +
                "COALESCE(NULLIF(v.titulo_fb, ''), p.titulo, 'Cine') AS titulo_real, " +
                "COALESCE(NULLIF(v.sala_fb, ''), s.nombre, 'Sala') AS sala_real, " +
                "COALESCE(NULLIF(v.butacas_fb, ''), '') AS butacas_real, " +
                "COALESCE(NULLIF(v.fecha_fb, ''), f.fecha_hora, '') AS fecha_real " +
                "FROM ventas v " +
                "LEFT JOIN funciones f ON v.id_funcion = f.id_funcion " +
                "LEFT JOIN peliculas p ON f.id_pelicula = p.id_pelicula " +
                "LEFT JOIN salas s ON f.id_sala = s.id_sala " +
                "LEFT JOIN detalle_entradas de ON v.id_venta = de.id_venta " +
                "WHERE v.id_venta = ?";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(idVenta)});
        VentaDetalle v = null;
        while (c.moveToNext()) {
            if (v == null) {
                v = new VentaDetalle();
                v.idVenta = idVenta;
                v.total = c.getDouble(c.getColumnIndexOrThrow("total"));
                v.subtotal = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
                v.descuento = c.getDouble(c.getColumnIndexOrThrow("descuento"));
                v.metodoPago = c.getString(c.getColumnIndexOrThrow("metodo_pago"));
                v.tipoComprobante = c.getString(c.getColumnIndexOrThrow("tipo_comprobante"));
                v.fechaVenta = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
                v.estadoVenta = c.getString(c.getColumnIndexOrThrow("estado"));
                v.tituloPeliculaAux = c.getString(c.getColumnIndexOrThrow("titulo_real"));
            }
            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.tituloPelicula = v.tituloPeliculaAux;
            e.nombreSala = c.getString(c.getColumnIndexOrThrow("sala_real"));
            e.fila = c.getString(c.getColumnIndexOrThrow("butacas_real"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_real"));
            e.codigoQR = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));
            v.entradas.add(e);
        }
        c.close();

        if (v != null) {
            Cursor cc = db.rawQuery("SELECT dc.id_producto, COALESCE(pr.nombre, 'Producto') AS nombre_producto, dc.cantidad, dc.precio_unitario, dc.subtotal " +
                    "FROM detalle_confiteria dc LEFT JOIN productos pr ON pr.id_producto = dc.id_producto WHERE dc.id_venta = ?", new String[]{String.valueOf(idVenta)});
            while (cc.moveToNext()) {
                VentaDetalle.ConfiteriaItem item = new VentaDetalle.ConfiteriaItem();
                item.idProducto = cc.getInt(cc.getColumnIndexOrThrow("id_producto"));
                item.nombreProducto = cc.getString(cc.getColumnIndexOrThrow("nombre_producto"));
                item.cantidad = cc.getInt(cc.getColumnIndexOrThrow("cantidad"));
                item.precioUnitario = cc.getDouble(cc.getColumnIndexOrThrow("precio_unitario"));
                item.subtotal = cc.getDouble(cc.getColumnIndexOrThrow("subtotal"));
                v.productosConfiteria.add(item);
            }
            cc.close();
        }
        return v;
    }

    public String getEstadoIngreso(String qr) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT estado_ingreso FROM detalle_entradas WHERE codigo_qr = ?", new String[]{qr});
        String res = c.moveToFirst() ? c.getString(0) : "Pendiente";
        c.close();
        return res;
    }

    public boolean validarEntrada(String qr, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado_ingreso", "Validado");
        return db.update("detalle_entradas", cv, "codigo_qr = ?", new String[]{qr}) > 0;
    }

    public Set<String> getButacasOcupadasPermanentes(int idFuncion) {
        Set<String> ocupadas = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT butacas_fb FROM ventas WHERE id_funcion = ? AND estado != 'Anulada'", new String[]{String.valueOf(idFuncion)});
        while (c.moveToNext()) {
            String b = c.getString(0);
            if (b != null) for (String s : b.split(",")) ocupadas.add(s.trim());
        }
        c.close();
        return ocupadas;
    }

    public Set<String> getButacasBloqueadasActivas(int idFuncion) { return new HashSet<>(); }
    public void bloquearButacasTemporales(List<String> b, int id) {}
    public void limpiarBloqueosCaducados() {}

    public int getIdVentaPorCodigoQR(String qr) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id_venta FROM detalle_entradas WHERE codigo_qr = ?", new String[]{qr});
        int res = c.moveToFirst() ? c.getInt(0) : -1;
        c.close();
        return res;
    }
}