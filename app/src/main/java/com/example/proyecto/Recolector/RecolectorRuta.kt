package com.example.proyecto.Recolector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.Adapter.PuntosRecoleccionAdapterTr
import com.example.proyecto.R
import com.example.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts


class RecolectorRuta : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var asignacionId: String
    private lateinit var googleMap: GoogleMap
    private lateinit var cardMapa: CardView
    private lateinit var btnExpandirMapa: MaterialButton
    private lateinit var btnIniciarRuta: MaterialButton
    private lateinit var txtEstadoRuta: TextView
    private lateinit var txtProgreso: TextView
    private lateinit var rvPuntosRecoleccion: RecyclerView

    private val puntosRecoleccion = mutableListOf<PuntoConEstado>()
    private lateinit var adapter: PuntosRecoleccionAdapterTr

    private var mapaExpandido = false
    private var rutaIniciada = false
    private var currentPolyline: Polyline? = null

    data class PuntoConEstado(
        val punto: entPuntoRecoleccion,
        val orden: Int,
        var estado: EstadoRecoleccion = EstadoRecoleccion.PENDIENTE,
        var horaRecoleccion: Long? = null
    )

    enum class EstadoRecoleccion {
        PENDIENTE,
        EN_CURSO,
        COMPLETADO
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001

        fun newInstance(asignacionId: String): RecolectorRuta {
            val fragment = RecolectorRuta()
            val args = Bundle()
            args.putString("asignacionId", asignacionId)
            fragment.arguments = args
            return fragment
        }
    }

    // Launcher para el escáner QR (Activity)
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QrScanner.RESULT_QR_VALIDO) {
            val puntoId = result.data?.getStringExtra(QrScanner.EXTRA_PUNTO_ID) ?: ""
            val asignacionId = result.data?.getStringExtra(QrScanner.EXTRA_ASIGNACION_ID) ?: ""
            val orden = result.data?.getIntExtra(QrScanner.EXTRA_ORDEN, 0) ?: 0

            // Abrir detalles del punto
            val fragment = DetallesPuntoRecoleccion.newInstance(puntoId, asignacionId, orden)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asignacionId = arguments?.getString("asignacionId") ?: ""
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recolector_ruta, container, false)

        initViews(view)
        setupMap()
        setupRecyclerView()
        cargarDatosRuta()

        btnExpandirMapa.setOnClickListener {
            toggleMapaExpandido()
        }

        btnIniciarRuta.setOnClickListener {
            iniciarRuta()
        }
        return view
    }

    private fun initViews(view: View) {
        cardMapa = view.findViewById(R.id.cardMapa)
        btnExpandirMapa = view.findViewById(R.id.btnExpandirMapa)
        btnIniciarRuta = view.findViewById(R.id.btnIniciarRuta)
        txtEstadoRuta = view.findViewById(R.id.txtEstadoRuta)
        txtProgreso = view.findViewById(R.id.txtProgreso)
        rvPuntosRecoleccion = view.findViewById(R.id.rvPuntosRecoleccion)
    }

    private fun setupRecyclerView() {
        adapter = PuntosRecoleccionAdapterTr(
            puntosRecoleccion,
            onPuntoClick = { punto -> enfocarPuntoEnMapa(punto) },
            onEscanearQR = { punto -> abrirEscanerQR(punto) }
        )
        rvPuntosRecoleccion.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosRecoleccion.adapter = adapter
    }

    // Abrir escáner como Activity en pantalla completa
    private fun abrirEscanerQR(punto: PuntoConEstado) {
        val intent = Intent(requireContext(), QrScanner::class.java).apply {
            putExtra(QrScanner.EXTRA_PUNTO_ID, punto.punto.id ?: "")
            putExtra(QrScanner.EXTRA_ASIGNACION_ID, asignacionId)
            putExtra(QrScanner.EXTRA_ORDEN, punto.orden)
        }
        qrScannerLauncher.launch(intent)
    }

    // Función pública para marcar completado desde DetallesPuntoRecoleccion
    fun marcarPuntoCompletadoExterno(puntoId: String) {
        val punto = puntosRecoleccion.find { it.punto.id == puntoId }
        punto?.let {
            marcarPuntoCompletado(it)
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isCompassEnabled = true
        }

        if (checkLocationPermission()) {
            enableMyLocation()
        }

        googleMap.setOnMarkerClickListener { marker ->
            val punto = marker.tag as? PuntoConEstado
            punto?.let {
                mostrarInfoPunto(it)
            }
            true
        }
    }

    private fun cargarDatosRuta() {
        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { asignacionDoc ->
                val rutaId = asignacionDoc.getString("rutaId") ?: return@addOnSuccessListener
                cargarPuntosRuta(rutaId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarPuntosRuta(rutaId: String) {
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .get()
            .addOnSuccessListener { rutaPuntos ->
                val puntosTemp = mutableListOf<Pair<Int, String>>()

                for (doc in rutaPuntos) {
                    val orden = doc.getLong("orden")?.toInt() ?: 0
                    val puntoId = doc.getString("puntoId") ?: ""
                    puntosTemp.add(orden to puntoId)
                }

                puntosTemp.sortBy { it.first }
                cargarDetallesPuntos(puntosTemp)
            }
    }

    private fun cargarDetallesPuntos(puntosOrdenados: List<Pair<Int, String>>) {
        var puntosRestantes = puntosOrdenados.size

        for ((orden, puntoId) in puntosOrdenados) {
            db.collection("puntos_recoleccion")
                .document(puntoId)
                .get()
                .addOnSuccessListener { puntoDoc ->
                    if (puntoDoc.exists()) {
                        val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
                        if (punto != null) {
                            puntosRecoleccion.add(PuntoConEstado(punto, orden))
                        }
                    }

                    puntosRestantes--
                    if (puntosRestantes == 0) {
                        puntosRecoleccion.sortBy { it.orden }
                        mostrarPuntosEnMapa()
                        adapter.notifyDataSetChanged()
                        actualizarProgreso()

                        if (puntosRecoleccion.isNotEmpty()) {
                            puntosRecoleccion[0].estado = EstadoRecoleccion.EN_CURSO
                        }
                    }
                }
        }
    }

    private fun mostrarPuntosEnMapa() {
        if (!::googleMap.isInitialized || puntosRecoleccion.isEmpty()) return

        val builder = LatLngBounds.Builder()
        val locations = mutableListOf<LatLng>()

        puntosRecoleccion.forEachIndexed { index, puntoEstado ->
            val geoPoint = puntoEstado.punto.ubicacion
            val latLng = LatLng(geoPoint?.latitude ?: 0.0, geoPoint?.longitude ?: 0.0)
            locations.add(latLng)
            builder.include(latLng)

            val markerColor = when (puntoEstado.estado) {
                EstadoRecoleccion.COMPLETADO -> Color.GREEN
                EstadoRecoleccion.EN_CURSO -> Color.rgb(255, 165, 0)
                EstadoRecoleccion.PENDIENTE -> Color.RED
            }

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("${index + 1}. ${puntoEstado.punto.nombre}")
                    .snippet(puntoEstado.punto.direccion)
                    .icon(createCustomMarker(markerColor, index + 1))
                    .anchor(0.5f, 0.5f)
            )
            marker?.tag = puntoEstado
        }

        if (locations.size > 1) {
            currentPolyline?.remove()
            currentPolyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(locations)
                    .color(Color.BLUE)
                    .width(10f)
            )
        }

        try {
            val bounds = builder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            if (locations.isNotEmpty()) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locations[0], 15f))
            }
        }
    }

    private fun createCustomMarker(color: Int, numero: Int): BitmapDescriptor {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD

        val textBounds = android.graphics.Rect()
        paint.getTextBounds(numero.toString(), 0, numero.toString().length, textBounds)
        canvas.drawText(
            numero.toString(),
            size / 2f,
            size / 2f + textBounds.height() / 2f,
            paint
        )

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun enfocarPuntoEnMapa(punto: PuntoConEstado) {
        val geoPoint = punto.punto.ubicacion
        val latLng = LatLng(geoPoint?.latitude ?: 0.0, geoPoint?.longitude ?: 0.0)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    private fun findMarkerForPunto(punto: PuntoConEstado): Marker? {
        return null
    }

    private fun marcarPuntoCompletado(punto: PuntoConEstado) {
        punto.estado = EstadoRecoleccion.COMPLETADO
        punto.horaRecoleccion = System.currentTimeMillis()

        val currentIndex = puntosRecoleccion.indexOf(punto)
        if (currentIndex < puntosRecoleccion.size - 1) {
            puntosRecoleccion[currentIndex + 1].estado = EstadoRecoleccion.EN_CURSO
        }

        adapter.notifyDataSetChanged()
        actualizarProgreso()

        googleMap.clear()
        mostrarPuntosEnMapa()

        Toast.makeText(requireContext(), "✓ Punto completado", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarInfoPunto(punto: PuntoConEstado) {
        // Implementar según necesidades
    }

    private fun toggleMapaExpandido() {
        mapaExpandido = !mapaExpandido

        val params = cardMapa.layoutParams
        params.height = if (mapaExpandido) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        }
        cardMapa.layoutParams = params

        btnExpandirMapa.text = if (mapaExpandido) "Minimizar Mapa ↓" else "Expandir Mapa ↑"
        rvPuntosRecoleccion.visibility = if (mapaExpandido) View.GONE else View.VISIBLE
    }

    private fun iniciarRuta() {
        if (!rutaIniciada) {
            rutaIniciada = true
            btnIniciarRuta.text = "Finalizar Ruta"
            txtEstadoRuta.text = "Estado: En Progreso"
            txtEstadoRuta.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))

            db.collection("asignaciones_rutas")
                .document(asignacionId)
                .update(
                    "estado", "En Progreso",
                    "fechaEjecucion", System.currentTimeMillis()
                )
        } else {
            finalizarRuta()
        }
    }

    private fun finalizarRuta() {
        val completados = puntosRecoleccion.count { it.estado == EstadoRecoleccion.COMPLETADO }
        val total = puntosRecoleccion.size

        if (completados < total) {
            Toast.makeText(
                requireContext(),
                "Aún quedan ${total - completados} puntos pendientes",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .update("estado", "Completada")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✓ Ruta completada", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
    }

    private fun actualizarProgreso() {
        val completados = puntosRecoleccion.count { it.estado == EstadoRecoleccion.COMPLETADO }
        val total = puntosRecoleccion.size
        txtProgreso.text = "Progreso: $completados/$total puntos"
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }
}