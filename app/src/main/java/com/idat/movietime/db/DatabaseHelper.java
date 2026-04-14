package com.idat.movietime.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
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

    private static final String TAG                      = "DatabaseHelper";
    private static final String DB_NAME                  = "db_movietime.db";
    private static final int    DB_VERSION               = 7;

    // Nombres de tablas
    private static final String TABLE_BUTACAS_BLOQUEADAS = "butacas_bloqueadas";

    public static final String TABLE_USUARIOS            = "usuarios";
    public static final String TABLE_PELICULAS           = "peliculas";
    public static final String TABLE_HISTORIAL_ACCESOS   = "historial_accesos";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = OFF;");
        crearTablas(db);
        crearTriggers(db);
        insertarDatosIniciales(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) db.execSQL("PRAGMA foreign_keys = OFF;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS detalle_entradas");
        db.execSQL("DROP TABLE IF EXISTS detalle_confiteria");
        db.execSQL("DROP TABLE IF EXISTS ventas");
        db.execSQL("DROP TABLE IF EXISTS funciones");
        db.execSQL("DROP TABLE IF EXISTS butacas");
        db.execSQL("DROP TABLE IF EXISTS salas");
        db.execSQL("DROP TABLE IF EXISTS peliculas");
        db.execSQL("DROP TABLE IF EXISTS productos");
        db.execSQL("DROP TABLE IF EXISTS promociones");
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS roles");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUTACAS_BLOQUEADAS);
        onCreate(db);
    }

    // ──────────────────────────────────────────────────────────────
    //  CREACIÓN DE TABLAS
    // ──────────────────────────────────────────────────────────────

    private void crearTablas(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS roles (" +
                "id_rol INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (" +
                "id_usuario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombres TEXT, apellidos TEXT, email TEXT UNIQUE, " +
                "documento TEXT, password_hash TEXT, id_rol INTEGER, " +
                "estado TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS peliculas (" +
                "id_pelicula INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "titulo TEXT, duracion_min INTEGER, genero TEXT, formato TEXT, " +
                "imagen_url TEXT, estado TEXT, clasificacion TEXT, sinopsis TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS salas (" +
                "id_sala INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, tipo_sala TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS butacas (" +
                "id_butaca INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_sala INTEGER, fila TEXT, numero INTEGER, tipo TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS funciones (" +
                "id_funcion INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_pelicula INTEGER, id_sala INTEGER, fecha_hora TEXT, " +
                "precio_base REAL, aforo_disponible INTEGER, tipo_funcion TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS promociones (" +
                "id_promocion INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, tipo_descuento TEXT, valor REAL, " +
                "estado TEXT, fecha_inicio TEXT, fecha_fin TEXT)");

        db.execSQL("INSERT INTO promociones (nombre, tipo_descuento, valor, estado, fecha_inicio, fecha_fin) VALUES ('PROMOYAPE', 'Porcentaje', 10.0, 'Activa', '2024-01-01', '2099-12-31')");

        db.execSQL("CREATE TABLE IF NOT EXISTS productos (" +
                "id_producto INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, stock_actual INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS ventas (" +
                "id_venta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_cliente INTEGER, id_funcion INTEGER, " +
                "subtotal REAL, descuento REAL, total REAL, " +
                "tipo_comprobante TEXT, metodo_pago TEXT, canal_venta TEXT, " +
                "id_promocion INTEGER, fecha_venta TEXT, estado TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS detalle_entradas (" +
                "id_detalle_entrada INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_venta INTEGER, id_funcion INTEGER, id_butaca INTEGER, " +
                "precio_unitario REAL, codigo_qr TEXT, " +
                "estado_ingreso TEXT, fecha_validacion TEXT, id_validado_por INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS detalle_confiteria (" +
                "id_detalle_confiteria INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_venta INTEGER, id_producto INTEGER, " +
                "cantidad INTEGER, precio_unitario REAL, subtotal REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS butacas_bloqueadas " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, codigo TEXT, id_funcion INTEGER, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

    }
    private void crearTriggers(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_validar_cliente_venta " +
                        "BEFORE INSERT ON ventas " +
                        "WHEN NEW.id_cliente IS NOT NULL " +
                        "BEGIN " +
                        "  SELECT RAISE(ABORT, 'El rol Control no tiene permisos para vender') " +
                        "  WHERE (SELECT id_rol FROM usuarios WHERE id_usuario = NEW.id_cliente) = " +
                        "        (SELECT id_rol FROM roles WHERE nombre = 'Control'); " +
                        "END");
    }
    // ──────────────────────────────────────────────────────────────
    //  DATOS INICIALES
    // ──────────────────────────────────────────────────────────────

    private void insertarDatosIniciales(SQLiteDatabase db) {
        // Roles
        String[] roles = {"Administrador", "Taquillero", "Confiteria", "Control", "Cliente"};
        for (String rol : roles) {
            ContentValues cv = new ContentValues();
            cv.put("nombre", rol);
            db.insertWithOnConflict("roles", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // Películas
        Object[][] peliculas = {
                {"Iron Lung: Océano de Sangre", 108, "Terror",               "2D", "", "Activa",   "R",     "Basado en el videojuego indie de terror."},
                {"Espía Entre Animales",        95,  "Animación / Comedia",  "3D", "", "Activa",   "G",     "Aventura animada para toda la familia."},
                {"Avengers: Doomsday",          150, "Acción / Superhéroes", "XD", "", "Activa",   "PG-13", "El universo Marvel enfrenta su mayor amenaza."},
                {"Minecraft: La Película",      110, "Aventura / Familia",   "2D", "", "Activa",   "PG",    "Cuatro inadaptados en el Overworld."},
                {"Superman: Legacy",            130, "Acción",               "XD", "", "Inactiva", "PG-13", "El regreso del Hombre de Acero."},
                {"Jurassic World: Renacimiento",125, "Aventura",             "3D", "", "Inactiva", "PG-13", "Nueva expedición a una isla remota."},
                {"La Traviata",                 180, "Ópera",                "2D", "", "Activa",   "TP",    "Ópera de Giuseppe Verdi en el cine."},
                {"Carmen",                      165, "Ópera",                "2D", "", "Activa",   "TP",    "La ópera más famosa de Georges Bizet."}
        };
        for (Object[] p : peliculas) {
            ContentValues cv = new ContentValues();
            cv.put("titulo",        (String)  p[0]);
            cv.put("duracion_min",  (Integer) p[1]);
            cv.put("genero",        (String)  p[2]);
            cv.put("formato",       (String)  p[3]);
            cv.put("imagen_url",    (String)  p[4]);
            cv.put("estado",        (String)  p[5]);
            cv.put("clasificacion", (String)  p[6]);
            cv.put("sinopsis",      (String)  p[7]);
            db.insertWithOnConflict("peliculas", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  PELÍCULAS
    // ──────────────────────────────────────────────────────────────

    // ──────────────────────────────────────────────────────────────
    //  PELÍCULAS (Orden corregido para coincidir con Pelicula.kt)
    // ──────────────────────────────────────────────────────────────

    public List<Pelicula> getPeliculasPorEstado(String estado) {
        List<Pelicula> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id_pelicula, titulo, duracion_min, clasificacion, " +
                        "       genero, formato, sinopsis, imagen_url, estado " +
                        "FROM peliculas WHERE estado = ?",
                new String[]{ estado });
        while (c.moveToNext()) {
            lista.add(new Pelicula(
                    c.getInt(c.getColumnIndexOrThrow("id_pelicula")),      // 1. id
                    c.getString(c.getColumnIndexOrThrow("titulo")),        // 2. titulo
                    c.getInt(c.getColumnIndexOrThrow("duracion_min")),     // 3. duracionMin
                    c.getString(c.getColumnIndexOrThrow("clasificacion")), // 4. clasificacion
                    c.getString(c.getColumnIndexOrThrow("genero")),        // 5. genero
                    c.getString(c.getColumnIndexOrThrow("formato")),       // 6. formato
                    c.getString(c.getColumnIndexOrThrow("sinopsis")),      // 7. sinopsis
                    c.getString(c.getColumnIndexOrThrow("imagen_url")),    // 8. imagenUrl
                    c.getString(c.getColumnIndexOrThrow("estado")),        // 9. estado
                    0,  // 10. anio (Dato local)
                    "", // 11. posterUrl (Dato local)
                    0   // 12. drawableRes (Dato local)
            ));
        }
        c.close();
        return lista;
    }

    // ──────────────────────────────────────────────────────────────
    //  MIS ENTRADAS
    //  Una fila = una butaca. Solo entradas futuras y pendientes.
    // ──────────────────────────────────────────────────────────────

    public List<VentaDetalle> getMisEntradas(int idCliente) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT " +
                        "  v.id_venta, v.fecha_venta, v.subtotal, v.descuento, v.total, " +
                        "  v.tipo_comprobante, v.metodo_pago, v.canal_venta, v.estado, " +
                        "  de.id_detalle_entrada, de.precio_unitario, de.codigo_qr, " +
                        "  de.estado_ingreso, de.fecha_validacion, " +
                        "  p.titulo         AS titulo_pelicula, " +
                        "  p.clasificacion, p.formato, " +
                        "  f.fecha_hora     AS fecha_hora_funcion, " +
                        "  f.tipo_funcion, " +
                        "  s.nombre         AS nombre_sala, " +
                        "  s.tipo_sala, " +
                        "  b.fila, b.numero, " +
                        "  b.tipo           AS tipo_butaca " +
                        "FROM detalle_entradas de " +
                        "JOIN ventas    v ON v.id_venta    = de.id_venta " +
                        "JOIN funciones f ON f.id_funcion  = de.id_funcion " +
                        "JOIN peliculas p ON p.id_pelicula = f.id_pelicula " +
                        "JOIN salas     s ON s.id_sala     = f.id_sala " +
                        "JOIN butacas   b ON b.id_butaca   = de.id_butaca " +
                        "WHERE v.id_cliente      = ? " +
                        "  AND v.estado         != 'Anulada' " +
                        "  AND de.estado_ingreso = 'Pendiente' " +
                        "  AND f.fecha_hora      > datetime('now') " +
                        "ORDER BY f.fecha_hora ASC";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idCliente) });
        while (c.moveToNext()) {
            VentaDetalle venta = mapearVentaBase(c);

            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.idDetalleEntrada = c.getInt(c.getColumnIndexOrThrow("id_detalle_entrada"));
            e.precioUnitario   = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            e.codigoQR         = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso    = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));
            e.fechaValidacion  = c.getString(c.getColumnIndexOrThrow("fecha_validacion"));
            e.tituloPelicula   = c.getString(c.getColumnIndexOrThrow("titulo_pelicula"));
            e.clasificacion    = c.getString(c.getColumnIndexOrThrow("clasificacion"));
            e.formato          = c.getString(c.getColumnIndexOrThrow("formato"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_hora_funcion"));
            e.tipoFuncion      = c.getString(c.getColumnIndexOrThrow("tipo_funcion"));
            e.nombreSala       = c.getString(c.getColumnIndexOrThrow("nombre_sala"));
            e.tipoSala         = c.getString(c.getColumnIndexOrThrow("tipo_sala"));
            e.fila             = c.getString(c.getColumnIndexOrThrow("fila"));
            e.numero           = c.getInt(c.getColumnIndexOrThrow("numero"));
            e.tipoButaca       = c.getString(c.getColumnIndexOrThrow("tipo_butaca"));

            venta.entradas.add(e);
            lista.add(venta);
        }
        c.close();
        return lista;
    }

    // ──────────────────────────────────────────────────────────────
    //  HISTORIAL DE COMPRAS
    //  Todas las compras del cliente sin filtro de fecha ni estado.
    //  FIX: columnas explícitas en lugar de v.* para evitar crash
    //       "no such column" y ambigüedad con JOIN.
    //  FIX: JOIN a funciones y peliculas para traer título e imagen.
    // ──────────────────────────────────────────────────────────────

    public List<VentaDetalle> getHistorialCliente(int idCliente) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT v.id_venta, v.fecha_venta, v.subtotal, v.descuento, v.total, " +
                        "       v.tipo_comprobante, v.metodo_pago, v.canal_venta, v.estado, " +
                        "       p.titulo AS titulo_pelicula, p.imagen_url " +
                        "FROM ventas v " +
                        "LEFT JOIN funciones f ON v.id_funcion = f.id_funcion " +
                        "LEFT JOIN peliculas p ON f.id_pelicula = p.id_pelicula " +
                        "WHERE v.id_cliente = ? " +
                        "ORDER BY v.id_venta DESC";

        try {
            Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idCliente) });
            while (c.moveToNext()) {
                VentaDetalle venta = mapearVentaBase(c);
                venta.tituloPeliculaAux   = c.getString(c.getColumnIndexOrThrow("titulo_pelicula"));
                venta.imagenUrlAux        = c.getString(c.getColumnIndexOrThrow("imagen_url"));
                venta.entradas            = getEntradasDeVenta(db, venta.idVenta);
                venta.productosConfiteria = getConfiteriaDeVenta(db, venta.idVenta);
                lista.add(venta);
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "getHistorialCliente: " + e.getMessage());
        }
        return lista;
    }

    // ──────────────────────────────────────────────────────────────
    //  DETALLE COMPLETO DE UNA VENTA
    // ──────────────────────────────────────────────────────────────

    public VentaDetalle getDetalleVenta(int idVenta) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id_venta, fecha_venta, subtotal, descuento, total, " +
                        "       tipo_comprobante, metodo_pago, canal_venta, estado " +
                        "FROM ventas WHERE id_venta = ?",
                new String[]{ String.valueOf(idVenta) });

        if (!c.moveToFirst()) { c.close(); return null; }

        VentaDetalle venta = mapearVentaBase(c);
        c.close();

        venta.entradas            = getEntradasDeVenta(db, idVenta);
        venta.productosConfiteria = getConfiteriaDeVenta(db, idVenta);
        return venta;
    }


    private VentaDetalle mapearVentaBase(Cursor c) {
        VentaDetalle v = new VentaDetalle();
        v.idVenta         = c.getInt(c.getColumnIndexOrThrow("id_venta"));
        v.fechaVenta      = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
        v.subtotal        = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
        v.descuento       = c.getDouble(c.getColumnIndexOrThrow("descuento"));
        v.total           = c.getDouble(c.getColumnIndexOrThrow("total"));
        v.tipoComprobante = c.getString(c.getColumnIndexOrThrow("tipo_comprobante"));
        v.metodoPago      = c.getString(c.getColumnIndexOrThrow("metodo_pago"));
        v.canalVenta      = c.getString(c.getColumnIndexOrThrow("canal_venta"));
        v.estadoVenta     = c.getString(c.getColumnIndexOrThrow("estado"));
        return v;
    }

    private List<VentaDetalle.EntradaItem> getEntradasDeVenta(SQLiteDatabase db, int idVenta) {
        List<VentaDetalle.EntradaItem> lista = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT " +
                        "  de.id_detalle_entrada, de.precio_unitario, de.codigo_qr, " +
                        "  de.estado_ingreso, de.fecha_validacion, " +
                        "  p.titulo         AS titulo_pelicula, " +
                        "  p.clasificacion, p.formato, " +
                        "  f.fecha_hora     AS fecha_hora_funcion, " +
                        "  f.tipo_funcion, " +
                        "  s.nombre         AS nombre_sala, " +
                        "  s.tipo_sala, " +
                        "  b.fila, b.numero, " +
                        "  b.tipo           AS tipo_butaca " +
                        "FROM detalle_entradas de " +
                        "JOIN funciones f ON f.id_funcion  = de.id_funcion " +
                        "JOIN peliculas p ON p.id_pelicula = f.id_pelicula " +
                        "JOIN salas     s ON s.id_sala     = f.id_sala " +
                        "JOIN butacas   b ON b.id_butaca   = de.id_butaca " +
                        "WHERE de.id_venta = ?",
                new String[]{ String.valueOf(idVenta) });

        while (c.moveToNext()) {
            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.idDetalleEntrada = c.getInt(c.getColumnIndexOrThrow("id_detalle_entrada"));
            e.precioUnitario   = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            e.codigoQR         = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso    = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));
            e.fechaValidacion  = c.getString(c.getColumnIndexOrThrow("fecha_validacion"));
            e.tituloPelicula   = c.getString(c.getColumnIndexOrThrow("titulo_pelicula"));
            e.clasificacion    = c.getString(c.getColumnIndexOrThrow("clasificacion"));
            e.formato          = c.getString(c.getColumnIndexOrThrow("formato"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_hora_funcion"));
            e.tipoFuncion      = c.getString(c.getColumnIndexOrThrow("tipo_funcion"));
            e.nombreSala       = c.getString(c.getColumnIndexOrThrow("nombre_sala"));
            e.tipoSala         = c.getString(c.getColumnIndexOrThrow("tipo_sala"));
            e.fila             = c.getString(c.getColumnIndexOrThrow("fila"));
            e.numero           = c.getInt(c.getColumnIndexOrThrow("numero"));
            e.tipoButaca       = c.getString(c.getColumnIndexOrThrow("tipo_butaca"));
            lista.add(e);
        }
        c.close();
        return lista;
    }

    private List<VentaDetalle.ConfiteriaItem> getConfiteriaDeVenta(SQLiteDatabase db, int idVenta) {
        List<VentaDetalle.ConfiteriaItem> lista = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT dc.id_producto, pr.nombre AS nombre_producto, " +
                        "       dc.cantidad, dc.precio_unitario, dc.subtotal " +
                        "FROM detalle_confiteria dc " +
                        "JOIN productos pr ON pr.id_producto = dc.id_producto " +
                        "WHERE dc.id_venta = ?",
                new String[]{ String.valueOf(idVenta) });

        while (c.moveToNext()) {
            VentaDetalle.ConfiteriaItem item = new VentaDetalle.ConfiteriaItem();
            item.idProducto     = c.getInt(c.getColumnIndexOrThrow("id_producto"));
            item.nombreProducto = c.getString(c.getColumnIndexOrThrow("nombre_producto"));
            item.cantidad       = c.getInt(c.getColumnIndexOrThrow("cantidad"));
            item.precioUnitario = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            item.subtotal       = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
            lista.add(item);
        }
        c.close();
        return lista;
    }
    // ──────────────────────────────────────────────────────────────
    public void bloquearButacasPermanentes(String butacasSeparadasPorComa, int idFuncion) {
        if (butacasSeparadasPorComa == null || butacasSeparadasPorComa.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        String[] codigos = butacasSeparadasPorComa.split(",");
        db.beginTransaction();
        try {
            for (String cod : codigos) {
                ContentValues cv = new ContentValues();
                cv.put("codigo", cod.trim());
                cv.put("id_funcion", idFuncion);
                // Ponemos una fecha lejana (año 2099) para que el limpiador temporal no la borre jamás
                cv.put("timestamp", "2099-12-31 23:59:59");
                db.insert("butacas_bloqueadas", null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error al bloquear butacas permanentes", e);
        } finally {
            db.endTransaction();
        }
    }
    // ──────────────────────────────────────────────────────────────
    //  BUTACAS BLOQUEADAS
    // ──────────────────────────────────────────────────────────────

    public void bloquearButacasTemporales(List<String> codigos, int idFuncion) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String cod : codigos) {
                ContentValues cv = new ContentValues();
                cv.put("codigo", cod);
                cv.put("id_funcion", idFuncion);
                db.insert(TABLE_BUTACAS_BLOQUEADAS, null, cv);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    // ──────────────────────────────────────────────────────────────
    //  BUTACAS VENDIDAS PERMANENTES
    // ──────────────────────────────────────────────────────────────
    public Set<String> getButacasOcupadasPermanentes(int idFuncion) {
        Set<String> ocupadas = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT b.fila, b.numero FROM detalle_entradas de " +
                "JOIN butacas b ON de.id_butaca = b.id_butaca " +
                "JOIN ventas v ON de.id_venta = v.id_venta " +
                "WHERE de.id_funcion = ? AND v.estado != 'Anulada'";
        try {
            Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idFuncion) });
            while (c.moveToNext()) {
                ocupadas.add(c.getString(0) + c.getInt(1)); // Junta "K" + 8 = "K8"
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getButacasOcupadasPermanentes: " + e.getMessage());
        }
        return ocupadas;
    }
    public Set<String> getButacasBloqueadasActivas(int idFuncion) {
        Set<String> bloqueadas = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();

        // Eliminamos bloqueos que tengan más de 1 minuto
        db.execSQL("DELETE FROM " + TABLE_BUTACAS_BLOQUEADAS +
                " WHERE timestamp <= datetime('now', '-1 minute')");

        Cursor c = db.rawQuery("SELECT codigo FROM " + TABLE_BUTACAS_BLOQUEADAS + " WHERE id_funcion = ?",
                new String[]{String.valueOf(idFuncion)});
        while (c.moveToNext()) {
            bloqueadas.add(c.getString(0));
        }
        c.close();
        return bloqueadas;
    }

    public void limpiarBloqueosCaducados() {
        SQLiteDatabase db = getWritableDatabase();
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.execSQL("DELETE FROM " + TABLE_BUTACAS_BLOQUEADAS + " WHERE fecha < ?",
                new String[]{ hoy });
    }

    // ──────────────────────────────────────────────────────────────
    //  PROMOCIONES
    // ──────────────────────────────────────────────────────────────

    public double aplicarPromocion(String codigo, double totalBase) {
        SQLiteDatabase db = getReadableDatabase();
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Cursor c = db.rawQuery(
                "SELECT tipo_descuento, valor FROM promociones " +
                        "WHERE nombre = ? AND estado = 'Activa' " +
                        "  AND fecha_inicio <= ? AND fecha_fin >= ? LIMIT 1",
                new String[]{ codigo, hoy, hoy });

        if (!c.moveToFirst()) { c.close(); return -1.0; }

        String tipo  = c.getString(c.getColumnIndexOrThrow("tipo_descuento"));
        double valor = c.getDouble(c.getColumnIndexOrThrow("valor"));
        c.close();

        if ("Porcentaje".equals(tipo)) {
            return Math.round(totalBase * valor / 100.0 * 100.0) / 100.0;
        } else {
            return Math.min(valor, totalBase);
        }
    }
    public List<VentaDetalle> getEntradasPorTipo(int idCliente, boolean esPasada) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String filtroTiempo = esPasada
                ? "(f.fecha_hora <= datetime('now') OR de.estado_ingreso = 'Validado' OR de.estado_ingreso = 'Anulado')"
                : "(f.fecha_hora > datetime('now') AND de.estado_ingreso = 'Pendiente')";
        String orden = esPasada ? "DESC" : "ASC";

        String sql =
                "SELECT v.id_venta, v.fecha_venta, v.subtotal, v.descuento, v.total, " +
                        "v.tipo_comprobante, v.metodo_pago, v.canal_venta, v.estado, " +
                        "de.id_detalle_entrada, de.precio_unitario, de.codigo_qr, " +
                        "de.estado_ingreso, de.fecha_validacion, " +
                        "p.id_pelicula, p.titulo AS titulo_pelicula, p.imagen_url, p.clasificacion, p.formato, " + // ✅ TRAEMOS LA IMAGEN
                        "f.fecha_hora AS fecha_hora_funcion, f.tipo_funcion, " +
                        "s.nombre AS nombre_sala, s.tipo_sala, " +
                        "b.fila, b.numero, b.tipo AS tipo_butaca " +
                        "FROM detalle_entradas de " +
                        "JOIN ventas v ON v.id_venta = de.id_venta " +
                        "JOIN funciones f ON f.id_funcion = de.id_funcion " +
                        "JOIN peliculas p ON p.id_pelicula = f.id_pelicula " +
                        "JOIN salas s ON s.id_sala = f.id_sala " +
                        "JOIN butacas b ON b.id_butaca = de.id_butaca " +
                        "WHERE v.id_cliente = ? AND v.estado != 'Anulada' AND " + filtroTiempo + " " +
                        "ORDER BY f.fecha_hora " + orden;

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idCliente) });
        while (c.moveToNext()) {
            VentaDetalle venta = mapearVentaBase(c);
            venta.idPeliculaAux = c.getInt(c.getColumnIndexOrThrow("id_pelicula"));
            venta.imagenUrlAux  = c.getString(c.getColumnIndexOrThrow("imagen_url"));

            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.idDetalleEntrada = c.getInt(c.getColumnIndexOrThrow("id_detalle_entrada"));
            e.precioUnitario   = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            e.codigoQR         = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso    = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));
            e.fechaValidacion  = c.getString(c.getColumnIndexOrThrow("fecha_validacion"));
            e.tituloPelicula   = c.getString(c.getColumnIndexOrThrow("titulo_pelicula"));
            e.clasificacion    = c.getString(c.getColumnIndexOrThrow("clasificacion"));
            e.formato          = c.getString(c.getColumnIndexOrThrow("formato"));
            e.fechaHoraFuncion = c.getString(c.getColumnIndexOrThrow("fecha_hora_funcion"));
            e.tipoFuncion      = c.getString(c.getColumnIndexOrThrow("tipo_funcion"));
            e.nombreSala       = c.getString(c.getColumnIndexOrThrow("nombre_sala"));
            e.tipoSala         = c.getString(c.getColumnIndexOrThrow("tipo_sala"));
            e.fila             = c.getString(c.getColumnIndexOrThrow("fila"));
            e.numero           = c.getInt(c.getColumnIndexOrThrow("numero"));
            e.tipoButaca       = c.getString(c.getColumnIndexOrThrow("tipo_butaca"));

            // Agrupar butacas si es la misma venta
            boolean existe = false;
            for (VentaDetalle vExistente : lista) {
                if (vExistente.idVenta == venta.idVenta) {
                    vExistente.entradas.add(e);
                    existe = true; break;
                }
            }
            if (!existe) {
                venta.entradas.add(e);
                lista.add(venta);
            }
        }
        c.close();
        return lista;
    }
    // ──────────────────────────────────────────────────────────────
    //  VENTAS — insertarVentaCompleta
    //  FIX: idCliente > 0 en lugar de != -1
    //  FIX: idFuncion > 0 con protección en cabecera y detalle
    //  FIX: transacción atómica (venta + entradas + confitería)
    // ──────────────────────────────────────────────────────────────

    public long insertarVentaCompleta(
            int idCliente,
            int idFuncion,
            List<Integer> idButacas,
            double precioUnitario,
            List<String> codigosQR,
            List<VentaDetalle.ConfiteriaItem> productosConfit,
            double subtotal,
            double descuento,
            double total,
            String tipoComprobante,
            String metodoPago,
            Integer idPromocion
    ) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long idVenta = -1;

        try {
            // ── 1. Cabecera de venta ─────────────────────────────────
            ContentValues cvVenta = new ContentValues();
            if (idCliente > 0) cvVenta.put("id_cliente", idCliente);
            if (idFuncion > 0) cvVenta.put("id_funcion", idFuncion);
            cvVenta.put("canal_venta",      "App");
            cvVenta.put("subtotal",         subtotal);
            cvVenta.put("descuento",        descuento);
            cvVenta.put("total",            total);
            cvVenta.put("tipo_comprobante", tipoComprobante);
            cvVenta.put("metodo_pago",      metodoPago);
            cvVenta.put("estado",           "Pagada");
            cvVenta.put("fecha_venta",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            if (idPromocion != null) cvVenta.put("id_promocion", idPromocion);

            idVenta = db.insertOrThrow("ventas", null, cvVenta);

            // ── 2. Detalle de entradas (una fila por butaca) ─────────
            if (idFuncion > 0 && idButacas != null && !idButacas.isEmpty()) {
                for (int i = 0; i < idButacas.size(); i++) {
                    ContentValues cv = new ContentValues();
                    cv.put("id_venta",        idVenta);
                    cv.put("id_funcion",      idFuncion);
                    cv.put("id_butaca",       idButacas.get(i));
                    cv.put("precio_unitario", precioUnitario);
                    cv.put("codigo_qr",       i < codigosQR.size() ? codigosQR.get(i) : codigosQR.get(0));
                    cv.put("estado_ingreso",  "Pendiente");
                    db.insertOrThrow("detalle_entradas", null, cv);
                }
            }

            // ── 3. Detalle confitería y descuento de stock ───────────
            if (productosConfit != null) {
                for (VentaDetalle.ConfiteriaItem item : productosConfit) {
                    ContentValues cv = new ContentValues();
                    cv.put("id_venta",        idVenta);
                    cv.put("id_producto",     item.idProducto);
                    cv.put("cantidad",        item.cantidad);
                    cv.put("precio_unitario", item.precioUnitario);
                    cv.put("subtotal",        item.subtotal);
                    db.insertOrThrow("detalle_confiteria", null, cv);

                    db.execSQL(
                            "UPDATE productos SET stock_actual = stock_actual - ? WHERE id_producto = ?",
                            new Object[]{ item.cantidad, item.idProducto });
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            idVenta = -1;
            Log.e(TAG, "insertarVentaCompleta: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }

        return idVenta;
    }

    // ──────────────────────────────────────────────────────────────
    //  VALIDACIÓN DE ENTRADAS QR
    //  Solo actualiza si estado_ingreso = 'Pendiente' → evita doble ingreso.
    // ──────────────────────────────────────────────────────────────

    public boolean validarEntrada(String codigoQR, int idValidadoPor) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado_ingreso",   "Validado");
        cv.put("fecha_validacion",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        if (idValidadoPor > 0) cv.put("id_validado_por", idValidadoPor);

        int rows = db.update("detalle_entradas", cv,
                "codigo_qr = ? AND estado_ingreso = 'Pendiente'",
                new String[]{ codigoQR });
        return rows > 0;
    }

    public String getEstadoIngreso(String codigoQR) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT estado_ingreso FROM detalle_entradas WHERE codigo_qr = ? LIMIT 1",
                new String[]{ codigoQR });
        String estado = null;
        if (c.moveToFirst()) estado = c.getString(0);
        c.close();
        return estado;
    }

    public int getIdVentaPorCodigoQR(String codigoQR) {
        SQLiteDatabase db = getReadableDatabase();
        int idVenta = -1;
        Cursor c = db.rawQuery(
                "SELECT id_venta FROM detalle_entradas WHERE codigo_qr = ? LIMIT 1",
                new String[]{ codigoQR });
        if (c.moveToFirst()) idVenta = c.getInt(c.getColumnIndexOrThrow("id_venta"));
        c.close();
        return idVenta;
    }
}