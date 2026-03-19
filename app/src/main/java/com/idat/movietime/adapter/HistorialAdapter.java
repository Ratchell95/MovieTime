package com.idat.movietime.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idat.movietime.R;
import com.idat.movietime.model.VentaDetalle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.VH> {

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
                .inflate(R.layout.item_historial_venta, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VentaDetalle v = items.get(position);

        h.tvFechaVenta.setText(formatFecha(v.fechaVenta));

        if ("Anulada".equals(v.estadoVenta)) {
            h.tvEstadoVenta.setText("Anulada");
            h.tvEstadoVenta.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            h.tvEstadoVenta.setText("Pagada");
            h.tvEstadoVenta.setBackgroundResource(R.drawable.bg_badge_green);
        }

        if (v.tieneEntradas()) {
            h.layoutEntradas.setVisibility(View.VISIBLE);
            h.containerEntradas.removeAllViews();

            VentaDetalle.EntradaItem primera = v.entradas.get(0);

            TextView tvPelicula = new TextView(h.itemView.getContext());
            tvPelicula.setTextSize(13f);
            tvPelicula.setTextColor(0xFF111111);
            tvPelicula.setTypeface(null, Typeface.BOLD);
            tvPelicula.setPadding(0, 0, 0, 2);
            tvPelicula.setText(primera.tituloPelicula);
            h.containerEntradas.addView(tvPelicula);

            String infoSala = primera.nombreSala + "  ·  Butaca " + primera.getButacaLabel()
                    + "\n" + formatFechaFuncion(primera.fechaHoraFuncion);
            TextView tvInfo = new TextView(h.itemView.getContext());
            tvInfo.setTextSize(12f);
            tvInfo.setTextColor(0xFF555555);
            tvInfo.setPadding(0, 0, 0, 2);
            tvInfo.setText(infoSala);
            h.containerEntradas.addView(tvInfo);

            if (v.entradas.size() > 1) {
                StringBuilder butacas = new StringBuilder("Butacas: ");
                for (VentaDetalle.EntradaItem ei : v.entradas)
                    butacas.append(ei.getButacaLabel()).append(", ");
                butacas.setLength(butacas.length() - 2);

                TextView tvBut = new TextView(h.itemView.getContext());
                tvBut.setTextSize(12f);
                tvBut.setTextColor(0xFF555555);
                tvBut.setPadding(0, 0, 0, 2);
                tvBut.setText(butacas.toString());
                h.containerEntradas.addView(tvBut);
            }

            TextView tvCod = new TextView(h.itemView.getContext());
            tvCod.setTextSize(11f);
            tvCod.setTextColor(0xFF888888);
            tvCod.setPadding(0, 0, 0, 8);
            tvCod.setTypeface(Typeface.MONOSPACE);
            tvCod.setText("Cód:  " + (primera.codigoQR != null ? primera.codigoQR : "—"));
            h.containerEntradas.addView(tvCod);

        } else {
            h.layoutEntradas.setVisibility(View.GONE);
        }

        if (v.tieneConfiteria()) {
            h.layoutConfiteria.setVisibility(View.VISIBLE);
            h.containerConfiteria.removeAllViews();
            for (VentaDetalle.ConfiteriaItem item : v.productosConfiteria) {
                TextView tv = new TextView(h.itemView.getContext());
                tv.setTextSize(12f);
                tv.setTextColor(0xFF444444);
                tv.setPadding(0, 0, 0, 4);
                tv.setText(item.cantidad + "x  " + item.nombreProducto
                        + "   S/ " + String.format("%.2f", item.subtotal));
                h.containerConfiteria.addView(tv);
            }
        } else {
            h.layoutConfiteria.setVisibility(View.GONE);
        }

        h.tvMetodoPago.setText(v.metodoPago + "  ·  " + v.tipoComprobante);
        h.tvCanalVenta.setText("Comprado el " + formatFechaCorta(v.fechaVenta));
        h.tvTotalVenta.setText("S/ " + String.format("%.2f", v.total));

        h.itemView.setOnClickListener(view -> onItemClick.accept(v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView     tvFechaVenta, tvEstadoVenta, tvMetodoPago, tvCanalVenta, tvTotalVenta;
        LinearLayout layoutEntradas, containerEntradas, layoutConfiteria, containerConfiteria;

        VH(@NonNull View v) {
            super(v);
            tvFechaVenta        = v.findViewById(R.id.tvFechaVenta);
            tvEstadoVenta       = v.findViewById(R.id.tvEstadoVenta);
            layoutEntradas      = v.findViewById(R.id.layoutEntradas);
            containerEntradas   = v.findViewById(R.id.containerEntradas);
            layoutConfiteria    = v.findViewById(R.id.layoutConfiteria);
            containerConfiteria = v.findViewById(R.id.containerConfiteria);
            tvMetodoPago        = v.findViewById(R.id.tvMetodoPago);
            tvCanalVenta        = v.findViewById(R.id.tvCanalVenta);
            tvTotalVenta        = v.findViewById(R.id.tvTotalVenta);
        }
    }

    private String formatFecha(String raw) {
        if (raw == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(raw);
            return new SimpleDateFormat("EEE dd MMM, yyyy  HH:mm", new Locale("es", "PE")).format(d);
        } catch (Exception e) { return raw; }
    }

    private String formatFechaFuncion(String raw) {
        if (raw == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(raw);
            return new SimpleDateFormat("EEE dd MMM, yyyy  hh:mm a", new Locale("es", "PE")).format(d);
        } catch (Exception e) { return raw; }
    }

    private String formatFechaCorta(String raw) {
        if (raw == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(raw);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
        } catch (Exception e) { return raw; }
    }
}