package com.tj.proyecto.Recolector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tj.proyecto.R
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.google.zxing.ResultPoint
class QrScanner : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var btnCancelar: MaterialButton
    private var puntoIdEsperado: String = ""
    private var escaneoActivo = true

    companion object {
        const val EXTRA_PUNTO_ID = "puntoId"
        const val EXTRA_ASIGNACION_ID = "asignacionId"
        const val EXTRA_ORDEN = "orden"
        const val RESULT_QR_VALIDO = 100
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            iniciarEscaner()
        } else {
            Toast.makeText(
                this,
                "Permiso de cámara necesario para escanear códigos QR",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        puntoIdEsperado = intent.getStringExtra(EXTRA_PUNTO_ID) ?: ""

        barcodeView = findViewById(R.id.barcodeView)
        btnCancelar = findViewById(R.id.btnCancelar)

        btnCancelar.setOnClickListener {
            finish()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarEscaner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    this,
                    "Se necesita acceso a la cámara para escanear códigos QR",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun iniciarEscaner() {
        barcodeView.setStatusText("Escanea el código QR del punto")
        barcodeView.decodeContinuous(callback)
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (!escaneoActivo) return

            result?.text?.let { qrCode ->
                escaneoActivo = false
                barcodeView.pause()
                procesarQRCode(qrCode)
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // No necesitamos hacer nada aquí
        }
    }

    private fun procesarQRCode(qrCode: String) {
        if (qrCode == puntoIdEsperado) {
            Toast.makeText(this, "✓ QR Válido", Toast.LENGTH_SHORT).show()

            // Devolver resultado exitoso
            val resultIntent = Intent().apply {
                putExtra(EXTRA_PUNTO_ID, puntoIdEsperado)
                putExtra(EXTRA_ASIGNACION_ID, intent.getStringExtra(EXTRA_ASIGNACION_ID))
                putExtra(EXTRA_ORDEN, intent.getIntExtra(EXTRA_ORDEN, 0))
            }
            setResult(RESULT_QR_VALIDO, resultIntent)
            finish()
        } else {
            Toast.makeText(
                this,
                "QR Incorrecto\nEste no es el punto asignado",
                Toast.LENGTH_LONG
            ).show()

            // Reactivar escaneo después de 2 segundos
            barcodeView.postDelayed({
                escaneoActivo = true
                barcodeView.resume()
            }, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
    }
}