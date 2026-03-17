package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class QRScannerActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        tvEstado = findViewById(R.id.tvEstado)

        iniciarScanner()
    }

    private fun iniciarScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Apunta la cámara al código QR de la entrada")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents != null) {
            val codigoQR = result.contents
            tvEstado.text = "QR leído: $codigoQR"
            validarQR(codigoQR)
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            finish()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun validarQR(codigoQR: String) {

        Toast.makeText(
            this,
            "✓ Acceso válido: $codigoQR",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}
