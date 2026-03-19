package com.idat.movietime.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.idat.movietime.model.VentaDetalle;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "db_movietime.db";  // mismo que MovieTimeDatabaseHelper
    private static final int    DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        crearTablasSoporte(db);
        crearTriggers(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) { }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
            crearTablasSoporte(db);
            crearTriggers(db);
        }
    }

    /** Crea tablas que pueden no existir en BDs creadas con versiones anteriores */
    private void crearTablasSoporte(SQLiteDatabase db) {
        // Tabla de bloqueo temporal de butacas por función/día
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS butacas_bloqueadas (" +
                        "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  codigo       TEXT    NOT NULL," +
                        "  fecha        TEXT    NOT NULL," +
                        "  id_funcion   INTEGER NOT NULL DEFAULT 0," +
                        "  bloqueado_en TEXT    DEFAULT (datetime('now'))" +
                        ")"
        );
        // Promo PROMOYAPE: S/5.00 fijo
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS promociones (" +
                        "  id_promocion   INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  nombre         TEXT    NOT NULL," +
                        "  descripcion    TEXT," +
                        "  tipo_descuento TEXT    NOT NULL," +
                        "  valor          REAL    NOT NULL," +
                        "  aplica_a       TEXT    DEFAULT 'Ambos'," +
                        "  fecha_inicio   TEXT    NOT NULL," +
                        "  fecha_fin      TEXT    NOT NULL," +
                        "  estado         TEXT    DEFAULT 'Programada'," +
                        "  fecha_creacion TEXT    DEFAULT (datetime('now'))" +
                        ")"
        );
        db.execSQL(
                "INSERT OR IGNORE INTO promociones " +
                        "(nombre, descripcion, tipo_descuento, valor, aplica_a, fecha_inicio, fecha_fin, estado) " +
                        "VALUES ('PROMOYAPE','Descuento especial Yape','Monto Fijo',5.0,'Ambos','2025-01-01','2027-12-31','Activa')"
        );
    }

    /**
     * Triggers de integridad lógica en ventas:
     *   - id_cliente  → debe tener rol 'Cliente'
     *   - id_empleado → debe tener rol 'Taquillero', 'Confiteria' o 'Administrador'
     *
     * SQLite no admite CHECK con subconsultas, por eso se usan TRIGGERS.
     * Se usan CREATE TRIGGER IF NOT EXISTS para ser idempotentes.
     */
    private void crearTriggers(SQLiteDatabase db) {
        // ── Trigger: id_cliente debe ser rol 'Cliente' ───────────────
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_ventas_cliente " +
                        "BEFORE INSERT ON ventas " +
                        "WHEN NEW.id_cliente IS NOT NULL " +
                        "BEGIN " +
                        "  SELECT RAISE(ABORT, 'id_cliente debe tener rol Cliente') " +
                        "  WHERE (SELECT id_rol FROM usuarios WHERE id_usuario = NEW.id_cliente) != " +
                        "        (SELECT id_rol FROM roles WHERE nombre = 'Cliente'); " +
                        "END"
        );
        // ── Trigger: id_empleado debe ser Taquillero, Confiteria o Admin ──
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_ventas_empleado " +
                        "BEFORE INSERT ON ventas " +
                        "WHEN NEW.id_empleado IS NOT NULL " +
                        "BEGIN " +
                        "  SELECT RAISE(ABORT, 'id_empleado debe tener rol Taquillero, Confiteria o Administrador') " +
                        "  WHERE (SELECT r.nombre FROM roles r " +
                        "         JOIN usuarios u ON u.id_rol = r.id_rol " +
                        "         WHERE u.id_usuario = NEW.id_empleado) " +
                        "        NOT IN ('Taquillero','Confiteria','Administrador'); " +
                        "END"
        );
    }

    // ════════════════════════════════════════════════════════════════
    //  HISTORIAL: lista resumida de ventas del cliente
    // ════════════════════════════════════════════════════════════════

    /**
     * Devuelve todas las ventas de un cliente ordenadas por fecha descendente.
     * Incluye el primer codigoQR de las entradas (para la preview en la card)
     * y contadores de entradas y productos de confitería.
     */
    public List<VentaDetalle> getHistorialCliente(int idCliente) {
        List<VentaDetalle> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Query principal: datos de venta + contadores
        String sql =
                "SELECT " +
                        "  v.id_venta, v.fecha_venta, v.subtotal, v.descuento, v.total, " +
                        "  v.tipo_comprobante, v.metodo_pago, v.canal_venta, v.estado, " +
                        "  (SELECT COUNT(*) FROM detalle_entradas de WHERE de.id_venta = v.id_venta)     AS num_entradas, " +
                        "  (SELECT COUNT(*) FROM detalle_confiteria dc WHERE dc.id_venta = v.id_venta)   AS num_productos " +
                        "FROM ventas v " +
                        "WHERE v.id_cliente = ? " +
                        "ORDER BY v.fecha_venta DESC";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idCliente) });

        while (c.moveToNext()) {
            VentaDetalle venta = new VentaDetalle();
            venta.idVenta        = c.getInt(c.getColumnIndexOrThrow("id_venta"));
            venta.fechaVenta     = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
            venta.subtotal       = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
            venta.descuento      = c.getDouble(c.getColumnIndexOrThrow("descuento"));
            venta.total          = c.getDouble(c.getColumnIndexOrThrow("total"));
            venta.tipoComprobante= c.getString(c.getColumnIndexOrThrow("tipo_comprobante"));
            venta.metodoPago     = c.getString(c.getColumnIndexOrThrow("metodo_pago"));
            venta.canalVenta     = c.getString(c.getColumnIndexOrThrow("canal_venta"));
            venta.estadoVenta    = c.getString(c.getColumnIndexOrThrow("estado"));

            int numEntradas  = c.getInt(c.getColumnIndexOrThrow("num_entradas"));
            int numProductos = c.getInt(c.getColumnIndexOrThrow("num_productos"));

            // Cargar entradas si las hay
            if (numEntradas > 0) {
                venta.entradas = getEntradasDeVenta(db, venta.idVenta);
            }
            // Cargar confitería si la hay
            if (numProductos > 0) {
                venta.productosConfiteria = getConfiteriaDeVenta(db, venta.idVenta);
            }

            lista.add(venta);
        }
        c.close();
        return lista;
    }


    public VentaDetalle getDetalleVenta(int idVenta) {
        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT id_venta, fecha_venta, subtotal, descuento, total, " +
                        "       tipo_comprobante, metodo_pago, canal_venta, estado " +
                        "FROM ventas WHERE id_venta = ?";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idVenta) });
        if (!c.moveToFirst()) { c.close(); return null; }

        VentaDetalle venta = new VentaDetalle();
        venta.idVenta        = c.getInt(c.getColumnIndexOrThrow("id_venta"));
        venta.fechaVenta     = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
        venta.subtotal       = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
        venta.descuento      = c.getDouble(c.getColumnIndexOrThrow("descuento"));
        venta.total          = c.getDouble(c.getColumnIndexOrThrow("total"));
        venta.tipoComprobante= c.getString(c.getColumnIndexOrThrow("tipo_comprobante"));
        venta.metodoPago     = c.getString(c.getColumnIndexOrThrow("metodo_pago"));
        venta.canalVenta     = c.getString(c.getColumnIndexOrThrow("canal_venta"));
        venta.estadoVenta    = c.getString(c.getColumnIndexOrThrow("estado"));
        c.close();

        venta.entradas           = getEntradasDeVenta(db, idVenta);
        venta.productosConfiteria = getConfiteriaDeVenta(db, idVenta);

        return venta;
    }

    private List<VentaDetalle.EntradaItem> getEntradasDeVenta(SQLiteDatabase db, int idVenta) {
        List<VentaDetalle.EntradaItem> lista = new ArrayList<>();

        // JOIN: detalle_entradas → funciones → peliculas, salas, butacas
        String sql =
                "SELECT " +
                        "  de.id_detalle_entrada, de.precio_unitario, de.codigo_qr, " +
                        "  de.estado_ingreso, de.fecha_validacion, " +
                        "  p.titulo            AS titulo_pelicula, " +
                        "  p.clasificacion, " +
                        "  p.formato, " +
                        "  f.fecha_hora        AS fecha_hora_funcion, " +
                        "  f.tipo_funcion, " +
                        "  s.nombre            AS nombre_sala, " +
                        "  s.tipo_sala, " +
                        "  b.fila, " +
                        "  b.numero, " +
                        "  b.tipo              AS tipo_butaca " +
                        "FROM detalle_entradas de " +
                        "JOIN funciones  f ON f.id_funcion  = de.id_funcion " +
                        "JOIN peliculas  p ON p.id_pelicula = f.id_pelicula " +
                        "JOIN salas      s ON s.id_sala     = f.id_sala " +
                        "JOIN butacas    b ON b.id_butaca   = de.id_butaca " +
                        "WHERE de.id_venta = ?";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idVenta) });
        while (c.moveToNext()) {
            VentaDetalle.EntradaItem e = new VentaDetalle.EntradaItem();
            e.idDetalleEntrada  = c.getInt(c.getColumnIndexOrThrow("id_detalle_entrada"));
            e.precioUnitario    = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            e.codigoQR          = c.getString(c.getColumnIndexOrThrow("codigo_qr"));
            e.estadoIngreso     = c.getString(c.getColumnIndexOrThrow("estado_ingreso"));
            e.fechaValidacion   = c.getString(c.getColumnIndexOrThrow("fecha_validacion"));
            e.tituloPelicula    = c.getString(c.getColumnIndexOrThrow("titulo_pelicula"));
            e.clasificacion     = c.getString(c.getColumnIndexOrThrow("clasificacion"));
            e.formato           = c.getString(c.getColumnIndexOrThrow("formato"));
            e.fechaHoraFuncion  = c.getString(c.getColumnIndexOrThrow("fecha_hora_funcion"));
            e.tipoFuncion       = c.getString(c.getColumnIndexOrThrow("tipo_funcion"));
            e.nombreSala        = c.getString(c.getColumnIndexOrThrow("nombre_sala"));
            e.tipoSala          = c.getString(c.getColumnIndexOrThrow("tipo_sala"));
            e.fila              = c.getString(c.getColumnIndexOrThrow("fila"));
            e.numero            = c.getInt(c.getColumnIndexOrThrow("numero"));
            e.tipoButaca        = c.getString(c.getColumnIndexOrThrow("tipo_butaca"));
            lista.add(e);
        }
        c.close();
        return lista;
    }

    private List<VentaDetalle.ConfiteriaItem> getConfiteriaDeVenta(SQLiteDatabase db, int idVenta) {
        List<VentaDetalle.ConfiteriaItem> lista = new ArrayList<>();

        String sql =
                "SELECT " +
                        "  dc.id_producto, pr.nombre AS nombre_producto, " +
                        "  dc.cantidad, dc.precio_unitario, dc.subtotal " +
                        "FROM detalle_confiteria dc " +
                        "JOIN productos pr ON pr.id_producto = dc.id_producto " +
                        "WHERE dc.id_venta = ?";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(idVenta) });
        while (c.moveToNext()) {
            VentaDetalle.ConfiteriaItem item = new VentaDetalle.ConfiteriaItem();
            item.idProducto      = c.getInt(c.getColumnIndexOrThrow("id_producto"));
            item.nombreProducto  = c.getString(c.getColumnIndexOrThrow("nombre_producto"));
            item.cantidad        = c.getInt(c.getColumnIndexOrThrow("cantidad"));
            item.precioUnitario  = c.getDouble(c.getColumnIndexOrThrow("precio_unitario"));
            item.subtotal        = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
            lista.add(item);
        }
        c.close();
        return lista;
    }

    public void bloquearButacas(java.util.List<String> codigos, String fecha, int idFuncion) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String codigo : codigos) {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("codigo",     codigo);
                cv.put("fecha",      fecha);
                cv.put("id_funcion", idFuncion);
                db.insertWithOnConflict("butacas_bloqueadas", null, cv,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    public java.util.Set<String> getButacasBloqueadas(String fecha, int idFuncion) {
        java.util.Set<String> bloqueadas = new java.util.HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        android.database.Cursor c = db.rawQuery(
                "SELECT codigo FROM butacas_bloqueadas WHERE fecha = ? AND id_funcion = ?",
                new String[]{ fecha, String.valueOf(idFuncion) });
        while (c.moveToNext()) bloqueadas.add(c.getString(0));
        c.close();
        return bloqueadas;
    }

    public void limpiarBloqueosCaducados() {
        SQLiteDatabase db = getWritableDatabase();
        String hoy = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        db.execSQL("DELETE FROM butacas_bloqueadas WHERE fecha < ?", new String[]{ hoy });
    }


    public double aplicarPromocion(String codigo, double totalBase) {
        SQLiteDatabase db = getReadableDatabase();
        String hoy = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        String sql =
                "SELECT tipo_descuento, valor, aplica_a " +
                        "FROM promociones " +
                        "WHERE nombre = ? " +
                        "  AND estado = 'Activa' " +
                        "  AND fecha_inicio <= ? " +
                        "  AND fecha_fin    >= ? " +
                        "LIMIT 1";

        Cursor c = db.rawQuery(sql, new String[]{ codigo, hoy, hoy });
        if (!c.moveToFirst()) { c.close(); return -1.0; }

        String tipo  = c.getString(c.getColumnIndexOrThrow("tipo_descuento"));
        double valor = c.getDouble(c.getColumnIndexOrThrow("valor"));
        c.close();

        if ("Porcentaje".equals(tipo)) {
            return Math.round(totalBase * valor / 100.0 * 100.0) / 100.0;
        } else { // Monto Fijo
            return Math.min(valor, totalBase);
        }
    }


    public long insertarVentaCompleta(
            int idCliente,
            int idFuncion,
            java.util.List<Integer> idButacas,
            double precioUnitario,
            java.util.List<String> codigosQR,
            java.util.List<VentaDetalle.ConfiteriaItem> productosConfit,
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

            android.content.ContentValues cvVenta = new android.content.ContentValues();
            if (idCliente != -1) cvVenta.put("id_cliente", idCliente);
            cvVenta.put("canal_venta",      "App");
            cvVenta.put("subtotal",         subtotal);
            cvVenta.put("descuento",        descuento);
            cvVenta.put("total",            total);
            cvVenta.put("tipo_comprobante", tipoComprobante);
            cvVenta.put("metodo_pago",      metodoPago);
            cvVenta.put("estado",           "Pagada");
            if (idPromocion != null) cvVenta.put("id_promocion", idPromocion);

            idVenta = db.insertOrThrow("ventas", null, cvVenta);
            if (idVenta == -1) throw new Exception("Fallo al insertar venta");

            if (idFuncion != -1 && idButacas != null && !idButacas.isEmpty()) {
                for (int i = 0; i < idButacas.size(); i++) {
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("id_venta",       (int) idVenta);
                    cv.put("id_funcion",      idFuncion);
                    cv.put("id_butaca",       idButacas.get(i));
                    cv.put("precio_unitario", precioUnitario);
                    cv.put("codigo_qr",       i < codigosQR.size() ? codigosQR.get(i) : codigosQR.get(0));
                    cv.put("estado_ingreso",  "Pendiente");
                    db.insertOrThrow("detalle_entradas", null, cv);
                }
            }

            // 3. Detalle confitería + descuento de stock
            if (productosConfit != null) {
                for (VentaDetalle.ConfiteriaItem item : productosConfit) {
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("id_venta",        (int) idVenta);
                    cv.put("id_producto",     item.idProducto);
                    cv.put("cantidad",        item.cantidad);
                    cv.put("precio_unitario", item.precioUnitario);
                    cv.put("subtotal",        item.subtotal);
                    db.insertOrThrow("detalle_confiteria", null, cv);
                    db.execSQL(
                            "UPDATE productos SET stock_actual = stock_actual - ? WHERE id_producto = ?",
                            new Object[]{ item.cantidad, item.idProducto }
                    );
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            idVenta = -1;
            android.util.Log.e("DatabaseHelper", "insertarVentaCompleta: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }

        return idVenta;
    }


    public int getIdVentaPorCodigoQR(String codigoQR) {
        SQLiteDatabase db = getReadableDatabase();
        int idVenta = -1;

        String sql = "SELECT id_venta FROM detalle_entradas WHERE codigo_qr = ? LIMIT 1";
        Cursor c = db.rawQuery(sql, new String[]{ codigoQR });

        if (c.moveToFirst()) {
            idVenta = c.getInt(c.getColumnIndexOrThrow("id_venta"));
        }
        c.close();
        return idVenta;
    }

}