package com.idat.movietime.model


data class Funcion(
    val idFuncion:       Int     = 0,
    val idPelicula:      Int     = 0,
    val idSala:          Int     = 0,
    val fechaHora:       String  = "",
    val precioBase:      Double  = 0.0,
    val aforoDisponible: Int     = 0,
    val tipoFuncion:     String  = "Publica",
    val organizador:     String? = null,
    val estado:          String  = "Programada"
)

data class Butaca(
    val idButaca: Int    = 0,
    val idSala:   Int    = 0,
    val fila:     String = "",
    val numero:   Int    = 0,
    val tipo:     String = "Estandar",
    val estado:   String = "Disponible"
)

data class Venta(
    val idVenta:         Int     = 0,
    val idCliente:       Int?    = null,
    val idEmpleado:      Int?    = null,
    val idPromocion:     Int?    = null,
    val canalVenta:      String  = "App",
    val fechaVenta:      String  = "",
    val subtotal:        Double  = 0.0,
    val descuento:       Double  = 0.0,
    val total:           Double  = 0.0,
    val tipoComprobante: String  = "Boleta",
    val metodoPago:      String  = "Tarjeta",
    val estado:          String  = "Pagada"
)

data class Producto(
    val idProducto:  Int     = 0,
    val nombre:      String  = "",
    val descripcion: String? = null,
    val precio:      Double  = 0.0,
    val stockActual: Int     = 0,
    val stockMinimo: Int     = 5,
    val idCategoria: Int?    = null,
    val estado:      String  = "Activo",
    val imagenUrl:   String? = null
)

data class Promocion(
    val idPromocion:   Int    = 0,
    val nombre:        String = "",
    val descripcion:   String = "",
    val tipoDescuento: String = "Porcentaje",
    val valor:         Double = 0.0,
    val aplicaA:       String = "Ambos",
    val fechaInicio:   String = "",
    val fechaFin:      String = "",
    val estado:        String = "Activa"
)
