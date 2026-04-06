package com.idat.movietime

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.idat.movietime.db.DatabaseHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvEstado: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var yaEscaneado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        findViewById<TextView>(R.id.tvToolbarTitulo)?.text = "Escanear QR"
        findViewById<android.view.View>(R.id.btnAtras)?.setOnClickListener { finish() }

        previewView = findViewById(R.id.previewView)
        tvEstado    = findViewById(R.id.tvEstado)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!yaEscaneado) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees)
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                                val raw = barcode.rawValue ?: continue
                                                yaEscaneado = true
                                                procesarQR(raw)
                                                break
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this,
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                tvEstado.text = "Error al iniciar cámara"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun procesarQR(contenido: String) {
        val partes = contenido.split("|")
        val intent = Intent(this, DetalleQRActivity::class.java)

        if (partes.size >= 7 && partes[0] == "MOVIETIME") {
            val codigoQR = partes[1]
            val pelicula = partes.getOrElse(2) { "" }
            val sala     = partes.getOrElse(3) { "" }
            val butaca   = partes.getOrElse(4) { "" }
            val fecha    = partes.getOrElse(5) { "" }
            val estado   = partes.getOrElse(6) { "Pendiente" }

            val db      = DatabaseHelper(this)
            val idVenta = db.getIdVentaPorCodigoQR(codigoQR)

            intent.putExtra("qr_invalido", false)
            intent.putExtra("codigo_qr",   codigoQR)
            intent.putExtra("id_venta",    idVenta)
            intent.putExtra("pelicula",    pelicula)
            intent.putExtra("sala",        sala)
            intent.putExtra("butaca",      butaca)
            intent.putExtra("fecha",       fecha)
            intent.putExtra("estado",      estado)
        } else {
            intent.putExtra("qr_invalido", true)
            intent.putExtra("codigo_qr",   contenido)
        }

        runOnUiThread { startActivity(intent) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            tvEstado.text = "Se necesita permiso de cámara"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}