package com.example.proyecto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SeleccionarUbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var ubicacionSeleccionada: LatLng? = null
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var fabMiUbicacion: FloatingActionButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                obtenerUbicacionActual()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                obtenerUbicacionActual()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_ubicacion)

        // Configurar toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seleccionar Ubicación"

        btnConfirmar = findViewById(R.id.btnConfirmar)
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Botón confirmar
        btnConfirmar.setOnClickListener {
            if (ubicacionSeleccionada != null) {
                val resultIntent = Intent()
                resultIntent.putExtra("latitud", ubicacionSeleccionada!!.latitude)
                resultIntent.putExtra("longitud", ubicacionSeleccionada!!.longitude)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Seleccione una ubicación en el mapa", Toast.LENGTH_SHORT).show()
            }
        }

        fabMiUbicacion.setOnClickListener {
            if (verificarPermisos()) {
                obtenerUbicacionActual()
            } else {
                solicitarPermisos()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Verificar si hay ubicación previa
        val latitud = intent.getDoubleExtra("latitud", 0.0)
        val longitud = intent.getDoubleExtra("longitud", 0.0)

        val ubicacionInicial = if (latitud != 0.0 && longitud != 0.0) {
            LatLng(latitud, longitud)
        } else {
            // Ubicación por defecto (Huaraz, Perú)
            LatLng(-9.5293, -77.5278)
        }

        // Mover cámara a la ubicación inicial
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionInicial, 15f))

        // Si hay ubicación previa, agregar marcador
        if (latitud != 0.0 && longitud != 0.0) {
            ubicacionSeleccionada = ubicacionInicial
            mMap.addMarker(MarkerOptions().position(ubicacionInicial).title("Ubicación seleccionada"))
        }

        // Listener para seleccionar ubicación al hacer clic en el mapa
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
            ubicacionSeleccionada = latLng
        }

        // Habilitar controles
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isCompassEnabled = true

        // Habilitar mi ubicación si hay permisos
        if (verificarPermisos()) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun verificarPermisos(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisos() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun obtenerUbicacionActual() {
        if (!verificarPermisos()) {
            Toast.makeText(this, "Sin permisos de ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Mostrar progreso
            Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()

            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    val ubicacionActual = LatLng(location.latitude, location.longitude)

                    // Limpiar marcadores anteriores
                    mMap.clear()

                    // Agregar marcador
                    mMap.addMarker(
                        MarkerOptions()
                            .position(ubicacionActual)
                            .title("Mi ubicación actual")
                    )

                    // Mover cámara
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(ubicacionActual, 17f)
                    )

                    // Guardar ubicación seleccionada
                    ubicacionSeleccionada = ubicacionActual

                    Toast.makeText(
                        this,
                        "Ubicación obtenida",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación. Intente de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al obtener ubicación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Error de permisos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}