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


public class EntradasAdapter extends RecyclerView.Adapter<EntradasAdapter.VH> {

    private static final TimeZone TZ_UTC  = TimeZone.getTimeZone("UTC");
    private static final TimeZone TZ_PERU = TimeZone.getTimeZone("America/Lima");

    private final List<VentaDetalle>     items;
    private final Consumer<VentaDetalle> onItemClick;

    public EntradasAdapter(List<VentaDetalle> items, Consumer<VentaDetalle> onItemClick) {
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
        VentaDetalle venta = items.get(position);

        String estadoIngreso = "Pendiente";
        if (venta.tieneEntradas()) {
            String ei = venta.entradas.get(0).estadoIngreso;
            if (ei != null && !ei.isEmpty()) estadoIngreso = ei;
        }

        switch (estadoIngreso) {
            case "Validado":
                h.tvHistEstado.setText(" Usado");
                h.tvHistEstado.setTextColor(0xFF4CAF50);
                break;
            case "Pendiente":
            default:
                h.tvHistEstado.setText("🎟 Pendiente");
                h.tvHistEstado.setTextColor(0xFFFF9800);
                break;
        }


        if (venta.tieneEntradas()) {
            VentaDetalle.EntradaItem primera = venta.entradas.get(0);

            String titulo = primera.tituloPelicula != null ? primera.tituloPelicula : "Entrada";
            if (primera.formato != null && !primera.formato.isEmpty())
                titulo += "  (" + primera.formato + ")";
            h.tvHistTitulo.setText(titulo);

            h.tvHistFecha.setText(formatFechaFuncion(primera.fechaHoraFuncion));

            String butacaInfo = "Butaca " + primera.getButacaLabel();
            if (primera.nombreSala != null && !primera.nombreSala.isEmpty())
                butacaInfo += "  ·  " + primera.nombreSala;
            if (primera.tipoSala != null && !primera.tipoSala.isEmpty())
                butacaInfo += "  (" + primera.tipoSala + ")";
            if (venta.entradas.size() > 1)
                butacaInfo += "  ·  " + venta.entradas.size() + " entradas";
            h.tvHistButaca.setText(butacaInfo);

        } else {

            h.tvHistTitulo.setText("Entrada");
            h.tvHistFecha.setText(formatFechaCorta(venta.fechaVenta));
            h.tvHistButaca.setText("—");
        }

        String codigoQR = "—";
        if (venta.tieneEntradas()) {
            String qr = venta.entradas.get(0).codigoQR;
            if (qr != null && !qr.isEmpty()) codigoQR = qr;
        }
        h.tvHistMetodo.setText("🔑 " + codigoQR);


        h.tvHistTotal.setText("S/ " + String.format("%.2f", venta.total));

        h.itemView.setOnClickListener(view -> onItemClick.accept(venta));
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
