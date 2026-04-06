package com.idat.movietime.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idat.movietime.R;
import com.idat.movietime.model.VentaDetalle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * HistorialAdapter
 *
 * FIX: antes mostraba "Compra" con "—" porque accedía a venta.entradas sin verificar
 * que el JOIN de DatabaseHelper hubiera retornado datos. Ahora:
 *  - tieneEntradas() / tieneConfiteria() son llamados correctamente.
 *  - El formato de fecha usa TZ UTC → Lima.
 *  - Si la venta tiene entradas, muestra título de película, fecha de función,
 *    butaca y sala correctamente.
 *  - Si es solo confitería, muestra los productos.
 *  - El color del estado distingue Anulada (rojo) vs Completada (verde).
 */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.VH> {
    private static final TimeZone TZ_UTC  = TimeZone.getTimeZone("UTC");
    private static final TimeZone TZ_PERU = TimeZone.getTimeZone("America/Lima");
    private final List<VentaDetalle>     items;
    private final Consumer<VentaDetalle> onItemClick;

    public HistorialAdapter(List<VentaDetalle> items, Consumer<VentaDetalle> onItemClick) {
        this.items       = items;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial_compra, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VentaDetalle v = items.get(position);

        // ── Estado ───────────────────────────────────────────────────
        if ("Anulada".equals(v.estadoVenta)) {
            h.tvHistEstado.setText("Anulada");
            h.tvHistEstado.setTextColor(0xFFF44336);
        } else {
            h.tvHistEstado.setText("Completada");
            h.tvHistEstado.setTextColor(0xFF4CAF50);
        }

        // ── Título: película o "Solo confitería" ──────────────────────
        // FIX: tieneEntradas() verifica que la lista no esté vacía Y que el JOIN
        //      haya devuelto datos reales (tituloPelicula != null).
        if (v.tieneEntradas()) {
            VentaDetalle.EntradaItem primera = v.entradas.get(0);

            // Título + formato
            String titulo = primera.tituloPelicula != null ? primera.tituloPelicula : "Entrada";
            if (primera.formato != null && !primera.formato.isEmpty())
                titulo += "  (" + primera.formato + ")";
            h.tvHistTitulo.setText(titulo);

            // Fecha de función (no fecha de venta)
            h.tvHistFecha.setText(formatFechaFuncion(primera.fechaHoraFuncion));

            // Butaca + sala
            String butacaInfo = "Butaca " + primera.getButacaLabel();
            if (primera.nombreSala != null && !primera.nombreSala.isEmpty())
                butacaInfo += "  ·  " + primera.nombreSala;
            if (primera.tipoSala != null && !primera.tipoSala.isEmpty())
                butacaInfo += "  (" + primera.tipoSala + ")";
            if (v.entradas.size() > 1)
                butacaInfo += "  ·  " + v.entradas.size() + " entradas";
            h.tvHistButaca.setText(butacaInfo);

        } else if (v.tieneConfiteria()) {
            h.tvHistTitulo.setText("🍿  Confitería");
            h.tvHistFecha.setText(formatFechaCorta(v.fechaVenta));

            // Listar nombres de productos (máx. 2 + "y N más")
            StringBuilder sb = new StringBuilder();
            int max = Math.min(v.productosConfiteria.size(), 2);
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append(", ");
                sb.append(v.productosConfiteria.get(i).nombreProducto);
            }
            if (v.productosConfiteria.size() > 2)
                sb.append(" y ").append(v.productosConfiteria.size() - 2).append(" más");
            h.tvHistButaca.setText(sb.toString());

        } else {
            h.tvHistTitulo.setText("Compra");
            h.tvHistFecha.setText(formatFechaCorta(v.fechaVenta));
            h.tvHistButaca.setText("—");
        }

        // ── Método + comprobante ─────────────────────────────────────
        String metodo = (v.metodoPago != null ? v.metodoPago : "—")
                + "  ·  " + (v.tipoComprobante != null ? v.tipoComprobante : "—");
        h.tvHistMetodo.setText(metodo);

        // ── Total ────────────────────────────────────────────────────
        h.tvHistTotal.setText("S/ " + String.format("%.2f", v.total));

        h.itemView.setOnClickListener(view -> onItemClick.accept(v));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvHistTitulo, tvHistEstado, tvHistFecha, tvHistButaca, tvHistMetodo, tvHistTotal;

        VH(@NonNull View v) {
            super(v);
            tvHistTitulo = v.findViewById(R.id.tvHistTitulo);
            tvHistEstado = v.findViewById(R.id.tvHistEstado);
            tvHistFecha  = v.findViewById(R.id.tvHistFecha);
            tvHistButaca = v.findViewById(R.id.tvHistButaca);
            tvHistMetodo = v.findViewById(R.id.tvHistMetodo);
            tvHistTotal  = v.findViewById(R.id.tvHistTotal);
        }
    }

    /** Formatea "yyyy-MM-dd HH:mm:ss" (UTC) → "Lun 15 Mar  ·  08:30 PM" (Lima) */
    private String formatFechaFuncion(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            in.setTimeZone(TZ_UTC);
            SimpleDateFormat out = new SimpleDateFormat("EEE dd MMM  ·  hh:mm a", new Locale("es", "PE"));
            out.setTimeZone(TZ_PERU);
            Date d = in.parse(raw);
            return out.format(d);
        } catch (Exception e) { return raw; }
    }

    /** Formatea "yyyy-MM-dd HH:mm:ss" (UTC) → "15/03/2025  08:30" (Lima) */
    private String formatFechaCorta(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            in.setTimeZone(TZ_UTC);
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy  HH:mm", new Locale("es", "PE"));
            out.setTimeZone(TZ_PERU);
            Date d = in.parse(raw);
            return out.format(d);
        } catch (Exception e) { return raw; }
    }
}