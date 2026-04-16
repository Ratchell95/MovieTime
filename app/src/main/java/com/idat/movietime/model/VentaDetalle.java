package com.idat.movietime.model;

import java.util.ArrayList;
import java.util.List;

public class VentaDetalle {
    public int    idVenta;
    public String fechaVenta;
    public double subtotal;
    public double descuento;
    public double total;
    public String tipoComprobante;
    public String metodoPago;
    public String canalVenta;
    public String estadoVenta;

    public String tituloPeliculaAux;
    public String imagenUrlAux;
    public int    idPeliculaAux;
    public List<EntradaItem> entradas = new ArrayList<>();
    public String tipoButaca;

    public List<ConfiteriaItem> productosConfiteria = new ArrayList<>();

    public boolean tieneEntradas() {
        return entradas != null && !entradas.isEmpty();
    }

    public boolean tieneConfiteria() {
        return productosConfiteria != null && !productosConfiteria.isEmpty();
    }
    public boolean tieneDescuento() {
        return descuento > 0;
    }

    public static class EntradaItem {
        public int    idDetalleEntrada;
        public double precioUnitario;
        public String codigoQR;
        public String estadoIngreso;
        public String fechaValidacion;
        public String tituloPelicula;
        public String clasificacion;
        public String formato;
        public String fechaHoraFuncion;
        public String tipoFuncion;
        public String nombreSala;
        public String tipoSala;
        public String fila;
        public int    numero;
        public String tipoButaca;

        public String getButacaLabel() {
            return (fila != null ? fila : "") + numero;
        }
    }

    public static class ConfiteriaItem {
        public int    idProducto;
        public String nombreProducto;
        public int    cantidad;
        public double precioUnitario;
        public double subtotal;
    }
}