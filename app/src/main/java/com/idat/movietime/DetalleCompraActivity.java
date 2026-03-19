package com.idat.movietime;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.idat.movietime.R;
import com.idat.movietime.db.DatabaseHelper;
import com.idat.movietime.model.VentaDetalle;

import java.util.EnumMap;
import java.util.Map;

/**
 * Muestra el detalle completo de una compra:
 *   - QR generado localmente con ZXing (sin servidor)
 *   - Datos de la entrada: película, sala, butaca, función
 *   - Productos de confitería
 *   - Resumen de pago
 *
 * El QR codifica un texto con el formato:
 *   MOVIETIME|<codigo>|<pelicula>|<sala>|<butaca>|<fecha>|<estado>
 *
 * Al escanearlo desde otro móvil (Google Lens, cualquier app QR),
 * se mostrará ese texto directamente — no requiere servidor.
 */
public class DetalleCompraActivity extends AppCompatActivity {

    private static final int QR_SIZE_PX = 600; // Resolución interna del bitmap

    // Views
    private TextView     tvEstadoCompra, tvFechaDetalle;
    private ImageView    ivQR;
    private TextView     tvCodigoQR, tvEstadoIngreso;
    private TextView     tvPelicula, tvSala, tvButaca, tvFuncion;
    private LinearLayout cardQR, cardConfiteria, containerProductos;
    private LinearLayout layoutDescuento;
    private TextView     tvSubtotal, tvDescuento, tvTotalDetalle;
    private TextView     tvMetodoDetalle, tvComprobanteDetalle;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_compra);

        bindViews();
        ((android.widget.TextView) findViewById(R.id.tvToolbarTitulo)).setText("Detalle de compra");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        int idVenta = getIntent().getIntExtra("id_venta", -1);
        if (idVenta == -1) { finish(); return; }

        dbHelper = new DatabaseHelper(this);
        VentaDetalle venta = dbHelper.getDetalleVenta(idVenta);

        if (venta == null) { finish(); return; }

        poblarVista(venta);
    }

    // ── Poblar UI ────────────────────────────────────────────────────

    private void poblarVista(VentaDetalle venta) {

        // Estado y fecha
        if ("Anulada".equals(venta.estadoVenta)) {
            tvEstadoCompra.setText("✕ Compra anulada");
            tvEstadoCompra.setTextColor(Color.parseColor("#F44336"));
        } else {
            tvEstadoCompra.setText("✓ Compra exitosa");
            tvEstadoCompra.setTextColor(Color.parseColor("#4CAF50"));
        }
        tvFechaDetalle.setText(formatFecha(venta.fechaVenta));

        // ── Entrada + QR ─────────────────────────────────────────────
        if (venta.tieneEntradas()) {
            // Mostramos la primera entrada en el card QR principal.
            // Si hay varias entradas, cada una tiene su propio QR único.
            VentaDetalle.EntradaItem primera = venta.entradas.get(0);

            cardQR.setVisibility(View.VISIBLE);
            tvPelicula.setText(primera.tituloPelicula + " (" + primera.formato + ")");
            tvSala.setText(primera.nombreSala);
            tvButaca.setText("Butaca " + primera.getButacaLabel() + " · " + primera.tipoButaca);
            tvFuncion.setText(formatFecha(primera.fechaHoraFuncion));
            tvCodigoQR.setText(primera.codigoQR);

            // Badge estado ingreso
            tvEstadoIngreso.setText("● " + primera.estadoIngreso);
            switch (primera.estadoIngreso) {
                case "Validado":
                    tvEstadoIngreso.setTextColor(Color.parseColor("#4CAF50")); break;
                case "Anulado":
                    tvEstadoIngreso.setTextColor(Color.parseColor("#F44336")); break;
                default: // Pendiente
                    tvEstadoIngreso.setTextColor(Color.parseColor("#FFA726")); break;
            }

            // Generar QR — contenido con todos los datos legibles
            generarQR(primera.buildQrContent());

        } else {
            cardQR.setVisibility(View.GONE);
        }

        // ── Confitería ───────────────────────────────────────────────
        if (venta.tieneConfiteria()) {
            cardConfiteria.setVisibility(View.VISIBLE);
            containerProductos.removeAllViews();

            for (VentaDetalle.ConfiteriaItem item : venta.productosConfiteria) {
                View fila = getLayoutInflater()
                        .inflate(R.layout.item_confiteria_fila, containerProductos, false);

                ((TextView) fila.findViewById(R.id.tvNombreProducto))
                        .setText(item.cantidad + "x  " + item.nombreProducto);
                ((TextView) fila.findViewById(R.id.tvPrecioProducto))
                        .setText("S/ " + String.format("%.2f", item.subtotal));

                containerProductos.addView(fila);
            }
        } else {
            cardConfiteria.setVisibility(View.GONE);
        }

        // ── Resumen de pago ──────────────────────────────────────────
        tvSubtotal.setText("S/ " + String.format("%.2f", venta.subtotal));

        if (venta.tieneDescuento()) {
            layoutDescuento.setVisibility(View.VISIBLE);
            tvDescuento.setText("-S/ " + String.format("%.2f", venta.descuento));
        }

        tvTotalDetalle.setText("S/ " + String.format("%.2f", venta.total));
        tvMetodoDetalle.setText(venta.metodoPago);
        tvComprobanteDetalle.setText(venta.tipoComprobante);
    }

    // ── Generación del QR (ZXing, sin servidor) ──────────────────────

    /**
     * Genera un Bitmap QR a partir del contenido de texto y lo asigna al ImageView.
     *
     * Dependencia en build.gradle (app):
     *   implementation 'com.google.zxing:core:3.5.3'
     *
     * Al escanear desde cualquier lector QR (Google Lens, etc.) se mostrará
     * el texto tal como está en `content` — sin necesidad de servidor.
     */
    private void generarQR(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE,
                    QR_SIZE_PX, QR_SIZE_PX, hints);

            // Convertir BitMatrix → Bitmap con fondo transparente y módulos negros
            int w = matrix.getWidth(), h = matrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivQR.setImageBitmap(bmp);

        } catch (WriterException e) {
            ivQR.setImageResource(android.R.drawable.ic_dialog_alert);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void bindViews() {
        tvEstadoCompra      = findViewById(R.id.tvEstadoCompra);
        tvFechaDetalle      = findViewById(R.id.tvFechaDetalle);
        ivQR                = findViewById(R.id.ivQR);
        tvCodigoQR          = findViewById(R.id.tvCodigoQR);
        tvEstadoIngreso     = findViewById(R.id.tvEstadoIngreso);
        tvPelicula          = findViewById(R.id.tvPelicula);
        tvSala              = findViewById(R.id.tvSala);
        tvButaca            = findViewById(R.id.tvButaca);
        tvFuncion           = findViewById(R.id.tvFuncion);
        cardQR              = findViewById(R.id.cardQR);
        cardConfiteria      = findViewById(R.id.cardConfiteria);
        containerProductos  = findViewById(R.id.containerProductos);
        layoutDescuento     = findViewById(R.id.layoutDescuento);
        tvSubtotal          = findViewById(R.id.tvSubtotal);
        tvDescuento         = findViewById(R.id.tvDescuento);
        tvTotalDetalle      = findViewById(R.id.tvTotalDetalle);
        tvMetodoDetalle     = findViewById(R.id.tvMetodoDetalle);
        tvComprobanteDetalle= findViewById(R.id.tvComprobanteDetalle);
    }

    private String formatFecha(String raw) {
        if (raw == null) return "";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(raw);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat(
                    "EEE dd MMM · hh:mm a", new java.util.Locale("es", "PE"));
            return out.format(d);
        } catch (Exception e) { return raw; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}