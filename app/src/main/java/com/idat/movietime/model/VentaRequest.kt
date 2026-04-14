package com.idat.movietime.model

data class VentaRequest(
    var idCliente: Int? = null,
    var idPromocion: Int? = null,
    var canalVenta: String = "App",
    var tipoComprobante: String = "",
    var metodoPago: String = "",
    var entradas: List<EntradaItem> = emptyList(),
    var confiteria: List<ConfiteriaItem> = emptyList()
) {
    data class EntradaItem(
        val idFuncion: Int,
        val idButaca: Int
    )
    data class ConfiteriaItem(
        val idProducto: Int,
        val cantidad: Int
    )
}

// También necesitas la respuesta
data class VentaResumenResponse(
    val idVenta: Int,
    val estado: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)