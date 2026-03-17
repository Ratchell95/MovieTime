package com.idat.movietime.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MovieTimeDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "db_movietime.db"
        const val DATABASE_VERSION = 1


        const val TABLE_ROLES                 = "roles"
        const val TABLE_USUARIOS              = "usuarios"
        const val TABLE_PELICULAS             = "peliculas"
        const val TABLE_SALAS                 = "salas"
        const val TABLE_FUNCIONES             = "funciones"
        const val TABLE_BUTACAS               = "butacas"
        const val TABLE_PROMOCIONES           = "promociones"
        const val TABLE_CATEGORIAS            = "categorias"
        const val TABLE_PRODUCTOS             = "productos"
        const val TABLE_MOVIMIENTOS_INVENTARIO = "movimientos_inventario"
        const val TABLE_VENTAS                = "ventas"
        const val TABLE_DETALLE_ENTRADAS      = "detalle_entradas"
        const val TABLE_DETALLE_CONFITERIA    = "detalle_confiteria"
        const val TABLE_REPORTES              = "reportes"
        const val TABLE_HISTORIAL_ACCESOS     = "historial_accesos"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS roles (
                id_rol      INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre      TEXT    NOT NULL UNIQUE,
                descripcion TEXT,
                estado      INTEGER DEFAULT 1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id_usuario          INTEGER PRIMARY KEY AUTOINCREMENT,
                nombres             TEXT    NOT NULL,
                apellidos           TEXT    NOT NULL,
                email               TEXT    NOT NULL UNIQUE,
                password_hash       TEXT    NOT NULL,
                telefono            TEXT,
                id_rol              INTEGER NOT NULL,
                estado              TEXT    DEFAULT 'Activo',
                fecha_creacion      TEXT    DEFAULT (datetime('now')),
                fecha_actualizacion TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
                CHECK (estado IN ('Activo','Inactivo'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS peliculas (
                id_pelicula   INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo        TEXT    NOT NULL,
                duracion_min  INTEGER NOT NULL,
                clasificacion TEXT,
                genero        TEXT,
                formato       TEXT    NOT NULL,
                sinopsis      TEXT,
                imagen_url    TEXT,
                estado        TEXT    DEFAULT 'Activa',
                fecha_creacion TEXT   DEFAULT (datetime('now')),
                CHECK (formato IN ('2D','3D','XD')),
                CHECK (estado  IN ('Activa','Inactiva'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS salas (
                id_sala         INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre          TEXT    NOT NULL,
                capacidad_total INTEGER NOT NULL,
                tipo_sala       TEXT    DEFAULT 'Estandar',
                estado          TEXT    DEFAULT 'Operativa',
                CHECK (tipo_sala IN ('Estandar','XD','VIP')),
                CHECK (estado    IN ('Operativa','Mantenimiento'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS funciones (
                id_funcion       INTEGER PRIMARY KEY AUTOINCREMENT,
                id_pelicula      INTEGER NOT NULL,
                id_sala          INTEGER NOT NULL,
                fecha_hora       TEXT    NOT NULL,
                precio_base      REAL    NOT NULL,
                aforo_disponible INTEGER NOT NULL,
                tipo_funcion     TEXT    DEFAULT 'Publica',
                organizador      TEXT,
                estado           TEXT    DEFAULT 'Programada',
                fecha_creacion   TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (id_pelicula) REFERENCES peliculas(id_pelicula),
                FOREIGN KEY (id_sala)     REFERENCES salas(id_sala),
                CHECK (tipo_funcion IN ('Publica','Privada')),
                CHECK (estado IN ('Programada','Activa','Finalizada','Cancelada'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS butacas (
                id_butaca INTEGER PRIMARY KEY AUTOINCREMENT,
                id_sala   INTEGER NOT NULL,
                fila      TEXT    NOT NULL,
                numero    INTEGER NOT NULL,
                tipo      TEXT    DEFAULT 'Estandar',
                estado    TEXT    DEFAULT 'Disponible',
                FOREIGN KEY (id_sala) REFERENCES salas(id_sala),
                UNIQUE (id_sala, fila, numero),
                CHECK (tipo   IN ('Estandar','Preferencial','Discapacitado')),
                CHECK (estado IN ('Disponible','Mantenimiento'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS promociones (
                id_promocion   INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre         TEXT    NOT NULL,
                descripcion    TEXT,
                tipo_descuento TEXT    NOT NULL,
                valor          REAL    NOT NULL,
                aplica_a       TEXT    DEFAULT 'Ambos',
                fecha_inicio   TEXT    NOT NULL,
                fecha_fin      TEXT    NOT NULL,
                estado         TEXT    DEFAULT 'Programada',
                fecha_creacion TEXT    DEFAULT (datetime('now')),
                CHECK (tipo_descuento IN ('Porcentaje','Monto Fijo')),
                CHECK (aplica_a       IN ('Entradas','Confiteria','Ambos')),
                CHECK (estado         IN ('Programada','Activa','Inactiva'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categorias (
                id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre       TEXT    NOT NULL UNIQUE,
                descripcion  TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS productos (
                id_producto         INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre              TEXT    NOT NULL,
                descripcion         TEXT,
                precio              REAL    NOT NULL,
                stock_actual        INTEGER NOT NULL,
                stock_minimo        INTEGER DEFAULT 5,
                id_categoria        INTEGER,
                estado              TEXT    DEFAULT 'Activo',
                fecha_creacion      TEXT    DEFAULT (datetime('now')),
                fecha_actualizacion TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
                CHECK (estado IN ('Activo','Inactivo'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS movimientos_inventario (
                id_movimiento    INTEGER PRIMARY KEY AUTOINCREMENT,
                id_producto      INTEGER NOT NULL,
                id_usuario       INTEGER NOT NULL,
                tipo_movimiento  TEXT    NOT NULL,
                cantidad         INTEGER NOT NULL,
                stock_anterior   INTEGER NOT NULL,
                stock_nuevo      INTEGER NOT NULL,
                motivo           TEXT,
                fecha_movimiento TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
                FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario),
                CHECK (tipo_movimiento IN ('Ingreso','Salida','Merma','Ajuste'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ventas (
                id_venta         INTEGER PRIMARY KEY AUTOINCREMENT,
                id_cliente       INTEGER,
                id_empleado      INTEGER,
                id_promocion     INTEGER,
                canal_venta      TEXT    NOT NULL,
                fecha_venta      TEXT    DEFAULT (datetime('now')),
                subtotal         REAL    NOT NULL,
                descuento        REAL    DEFAULT 0,
                total            REAL    NOT NULL,
                tipo_comprobante TEXT    NOT NULL,
                metodo_pago      TEXT    NOT NULL,
                estado           TEXT    DEFAULT 'Pagada',
                FOREIGN KEY (id_cliente)   REFERENCES usuarios(id_usuario),
                FOREIGN KEY (id_empleado)  REFERENCES usuarios(id_usuario),
                FOREIGN KEY (id_promocion) REFERENCES promociones(id_promocion),
                CHECK (canal_venta      IN ('App','Web','Taquilla')),
                CHECK (tipo_comprobante IN ('Boleta','Factura')),
                CHECK (metodo_pago      IN ('Yape','Plin','Tarjeta','Efectivo')),
                CHECK (estado           IN ('Pagada','Anulada','Pendiente'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS detalle_entradas (
                id_detalle_entrada INTEGER PRIMARY KEY AUTOINCREMENT,
                id_venta           INTEGER NOT NULL,
                id_funcion         INTEGER NOT NULL,
                id_butaca          INTEGER NOT NULL,
                precio_unitario    REAL    NOT NULL,
                codigo_qr          TEXT    NOT NULL UNIQUE,
                estado_ingreso     TEXT    DEFAULT 'Pendiente',
                fecha_validacion   TEXT,
                id_validado_por    INTEGER,
                FOREIGN KEY (id_venta)        REFERENCES ventas(id_venta),
                FOREIGN KEY (id_funcion)      REFERENCES funciones(id_funcion),
                FOREIGN KEY (id_butaca)       REFERENCES butacas(id_butaca),
                FOREIGN KEY (id_validado_por) REFERENCES usuarios(id_usuario),
                CHECK (estado_ingreso IN ('Pendiente','Validado','Anulado'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS detalle_confiteria (
                id_detalle_confiteria INTEGER PRIMARY KEY AUTOINCREMENT,
                id_venta              INTEGER NOT NULL,
                id_producto           INTEGER NOT NULL,
                cantidad              INTEGER NOT NULL,
                precio_unitario       REAL    NOT NULL,
                subtotal              REAL    NOT NULL,
                FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta),
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reportes (
                id_reporte     INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario     INTEGER NOT NULL,
                tipo_reporte   TEXT    NOT NULL,
                fecha_inicio   TEXT    NOT NULL,
                fecha_fin      TEXT    NOT NULL,
                formato_export TEXT    NOT NULL,
                fecha_generado TEXT    DEFAULT (datetime('now')),
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
                CHECK (tipo_reporte   IN ('Ventas','Ocupacion','Confiteria','General')),
                CHECK (formato_export IN ('PDF','Excel'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS historial_accesos (
                id_historial  INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario    INTEGER NOT NULL,
                fecha_ingreso TEXT    DEFAULT (datetime('now')),
                ip            TEXT,
                dispositivo   TEXT,
                resultado     TEXT    DEFAULT 'Exitoso',
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
                CHECK (resultado IN ('Exitoso','Fallido'))
            )
        """.trimIndent())

        // Datos iniciales de roles
        db.execSQL("INSERT OR IGNORE INTO roles (nombre, descripcion) VALUES ('Administrador', 'Acceso total al sistema')")
        db.execSQL("INSERT OR IGNORE INTO roles (nombre, descripcion) VALUES ('Taquillero', 'Venta presencial de entradas')")
        db.execSQL("INSERT OR IGNORE INTO roles (nombre, descripcion) VALUES ('Cliente', 'Compra de entradas en app')")
        db.execSQL("INSERT OR IGNORE INTO roles (nombre, descripcion) VALUES ('Confiteria', 'Personal de confiteria')")
        db.execSQL("INSERT OR IGNORE INTO roles (nombre, descripcion) VALUES ('Control', 'Control de acceso QR')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS historial_accesos")
        db.execSQL("DROP TABLE IF EXISTS reportes")
        db.execSQL("DROP TABLE IF EXISTS detalle_confiteria")
        db.execSQL("DROP TABLE IF EXISTS detalle_entradas")
        db.execSQL("DROP TABLE IF EXISTS ventas")
        db.execSQL("DROP TABLE IF EXISTS movimientos_inventario")
        db.execSQL("DROP TABLE IF EXISTS productos")
        db.execSQL("DROP TABLE IF EXISTS categorias")
        db.execSQL("DROP TABLE IF EXISTS promociones")
        db.execSQL("DROP TABLE IF EXISTS butacas")
        db.execSQL("DROP TABLE IF EXISTS funciones")
        db.execSQL("DROP TABLE IF EXISTS salas")
        db.execSQL("DROP TABLE IF EXISTS peliculas")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        db.execSQL("DROP TABLE IF EXISTS roles")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
    }
}