package com.tj.proyecto.Recolector

import android.Manifest
import android.app.AlertDialog
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
import com.tj.proyecto.Adapter.PuntosRecoleccionAdapterTr
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import android.widget.Button
import android.widget.RadioButton


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

    // Variable para guardar la foto temporalmente en memoria
    private var fotoIncidenciaBitmap: android.graphics.Bitmap? = null

    // Launcher para tomar foto pequeña (Thumbnail) - ¡Es lo más rápido para demos!
    private val tomarFotoIncidenciaLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            fotoIncidenciaBitmap = bitmap
            // Buscamos la imagen en el diálogo (un poco truculento porque el diálogo está en otro scope,
            // pero lo manejaremos actualizando una variable global o vista referenciada)
            vistaDialogoActual?.findViewById<ImageView>(R.id.imgEvidenciaPreview)?.setImageBitmap(bitmap)
        }
    }

    // Variable auxiliar para referenciar la vista del diálogo
    private var vistaDialogoActual: View? = null

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

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QrScanner.RESULT_QR_VALIDO) {
            val puntoId = result.data?.getStringExtra(QrScanner.EXTRA_PUNTO_ID) ?: ""
            val asignacionIdResult = result.data?.getStringExtra(QrScanner.EXTRA_ASIGNACION_ID) ?: ""
            val orden = result.data?.getIntExtra(QrScanner.EXTRA_ORDEN, 0) ?: 0

            // Abrir detalles del punto
            val fragment = DetallesPuntoRecoleccion.newInstance(puntoId, asignacionIdResult, orden)
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
        cargarEstadoRuta()

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
            onMapGoogle = { punto -> abrirGoogleMap(punto)},
            onEscanearQR = { punto -> abrirEscanerQR(punto) },

            onReportarClick = { punto -> mostrarDialogoReporte(punto) }
        )
        rvPuntosRecoleccion.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosRecoleccion.adapter = adapter
    }

    private fun abrirGoogleMap(punto: PuntoConEstado) {
        // 1. Obtenemos las coordenadas del punto seleccionado
        val geoPoint = punto.punto.ubicacion
        val lat = geoPoint?.latitude ?: 0.0
        val lng = geoPoint?.longitude ?: 0.0

        // 2. Creamos la URI especial para navegación GPS
        // "google.navigation:q=" le dice al celular que queremos una ruta hacia esas coordenadas
        val gmmIntentUri = android.net.Uri.parse("google.navigation:q=$lat,$lng")

        // 3. Creamos el Intent para abrir la app externa
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        // Le decimos que use específicamente la app de Google Maps
        mapIntent.setPackage("com.google.android.apps.maps")

        // 4. Lanzamos la app (con protección por si no la tiene instalada)
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            // Si no tiene Google Maps, mostramos un aviso o usamos la web
            Toast.makeText(requireContext(), "Por favor instala Google Maps", Toast.LENGTH_SHORT).show()
        }
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

    private fun cargarEstadoRuta() {
        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val estadoRuta = document.getString("estado") ?: "Programada"

                    when (estadoRuta) {
                        "En Progreso" -> {
                            rutaIniciada = true
                            btnIniciarRuta.text = "Finalizar Ruta"
                            txtEstadoRuta.text = "Estado: En Progreso"
                            txtEstadoRuta.setTextColor(
                                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                            )
                        }
                        "Completada" -> {
                            rutaIniciada = true
                            btnIniciarRuta.text = "Ruta Completada"
                            btnIniciarRuta.isEnabled = false
                            txtEstadoRuta.text = "Estado: Completada"
                            txtEstadoRuta.setTextColor(
                                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                            )
                        }
                        else -> { // "Programada"
                            rutaIniciada = false
                            btnIniciarRuta.text = "Iniciar Ruta"
                            txtEstadoRuta.text = "Estado: Programada"
                            txtEstadoRuta.setTextColor(
                                ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                            )
                        }
                    }

                    Log.d("RecolectorRuta", "Estado de ruta cargado: $estadoRuta")
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecolectorRuta", "Error al cargar estado de ruta: ${e.message}")
            }
    }

    override fun onResume() {
        super.onResume()

        cargarEstadoRuta()

        // Si ya se cargaron los puntos, recargar sus estados desde BD
        if (puntosRecoleccion.isNotEmpty()) {
            Log.d("RecolectorRuta", "onResume: Recargando estados desde BD")
            cargarEstadosPuntosDesdeDB()
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

                        // Cargar estados desde BD
                        cargarEstadosPuntosDesdeDB()
                    }
                }
        }
    }

    private fun cargarEstadosPuntosDesdeDB() {
        db.collection("evidencias_recoleccion")
            .whereEqualTo("asignacionId", asignacionId)
            .get()
            .addOnSuccessListener { evidencias ->
                val puntosCompletadosIds = mutableSetOf<String>()

                // Obtener todos los puntos que tienen evidencias (están completados)
                for (doc in evidencias) {
                    val puntoId = doc.getString("puntoId")
                    if (puntoId != null) {
                        puntosCompletadosIds.add(puntoId)
                    }
                }

                Log.d("RecolectorRuta", "Puntos completados en BD: $puntosCompletadosIds")

                // Actualizar estados en la lista
                var primerPendienteEncontrado = false

                puntosRecoleccion.forEach { punto ->
                    punto.estado = if (puntosCompletadosIds.contains(punto.punto.id)) {
                        EstadoRecoleccion.COMPLETADO
                    } else if (!primerPendienteEncontrado) {
                        primerPendienteEncontrado = true
                        EstadoRecoleccion.EN_CURSO
                    } else {
                        EstadoRecoleccion.PENDIENTE
                    }
                }

                // Actualizar UI
                mostrarPuntosEnMapa()
                adapter.notifyDataSetChanged()
                actualizarProgreso()
            }
            .addOnFailureListener { e ->
                Log.e("RecolectorRuta", "Error al cargar estados: ${e.message}")
                // Si falla, establecer el primer punto como EN_CURSO
                if (puntosRecoleccion.isNotEmpty()) {
                    puntosRecoleccion[0].estado = EstadoRecoleccion.EN_CURSO
                }
                mostrarPuntosEnMapa()
                adapter.notifyDataSetChanged()
                actualizarProgreso()
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

            val puntoId = puntoEstado.punto.id ?: ""

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

    private fun marcarPuntoCompletado(punto: PuntoConEstado) {
        punto.estado = EstadoRecoleccion.COMPLETADO
        punto.horaRecoleccion = System.currentTimeMillis()

        val currentIndex = puntosRecoleccion.indexOf(punto)
        if (currentIndex < puntosRecoleccion.size - 1) {
            puntosRecoleccion[currentIndex + 1].estado = EstadoRecoleccion.EN_CURSO
        }

        adapter.notifyDataSetChanged()
        actualizarProgreso()

//        googleMap.clear()
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

            // Actualizar estado en Firestore
            db.collection("asignaciones_rutas")
                .document(asignacionId)
                .update(
                    "estado", "En Progreso",
                    "fechaEjecucion", System.currentTimeMillis()
                )
                .addOnSuccessListener {
                    Log.d("RecolectorRuta", "Estado actualizado a 'En Progreso'")
                }
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

        // Obtener rutaId desde la asignación
        db.collection("asignaciones_rutas")
            .document(asignacionId)
            .get()
            .addOnSuccessListener { document ->
                val rutaId = document.getString("rutaId") ?: ""

                // Abrir fragment de registro de peso
                val fragment = RegistroPesoMaterial.newInstance(asignacionId, rutaId)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Log.e("RecolectorRuta", "Error al obtener rutaId: ${e.message}")
                Toast.makeText(requireContext(), "Error al procesar ruta", Toast.LENGTH_SHORT).show()
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

    private fun mostrarDialogoReporte(punto: PuntoConEstado) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater

        // Usamos la variable global para acceder luego desde el launcher
        vistaDialogoActual = inflater.inflate(R.layout.dialog_reportar_incidencia, null)

        val rgMotivos = vistaDialogoActual!!.findViewById<RadioGroup>(R.id.rgMotivos)
        val btnFoto = vistaDialogoActual!!.findViewById<Button>(R.id.btnTomarFotoIncidencia)

        fotoIncidenciaBitmap = null

        btnFoto.setOnClickListener {
            tomarFotoIncidenciaLauncher.launch(null)
        }

        builder.setView(vistaDialogoActual)
        builder.setTitle("Reportar Incidencia - Punto ${punto.orden}")
        builder.setPositiveButton("Enviar Reporte") { _, _ ->
            val selectedId = rgMotivos.checkedRadioButtonId
            var motivo = "Otro"
            if (selectedId != -1) {
                val rb = vistaDialogoActual!!.findViewById<RadioButton>(selectedId)
                motivo = rb.text.toString()
            }
            guardarIncidencia(punto, motivo)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun guardarIncidencia(punto: PuntoConEstado, motivo: String) {
        val progress = android.app.ProgressDialog(requireContext())
        progress.setMessage("Enviando...")
        progress.show()

        val data = hashMapOf<String, Any?>(
            "asignacionId" to asignacionId,
            "puntoId" to punto.punto.id,
            "puntoNombre" to punto.punto.nombre,
            "orden" to punto.orden,
            "motivo" to motivo,
            "fechaReporte" to System.currentTimeMillis(),
            "estado" to "Pendiente",
            "fotoUrl" to ""
        )

        if (fotoIncidenciaBitmap != null) {
            val ref = FirebaseStorage.getInstance().reference.child("incidencias/${System.currentTimeMillis()}.jpg")
            val baos = ByteArrayOutputStream()
            fotoIncidenciaBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, baos)

            ref.putBytes(baos.toByteArray()).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    data["fotoUrl"] = uri.toString()
                    guardarEnFirestore(data, progress)
                }
            }
        } else {
            guardarEnFirestore(data, progress)
        }
    }

    private fun guardarEnFirestore(data: HashMap<String, Any?>, progressDialog: android.app.ProgressDialog) {
        db.collection("incidencias")
            .add(data)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "✅ Incidencia reportada", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error al enviar reporte", Toast.LENGTH_SHORT).show()
            }
    }

//    private fun cargarEstadosPuntosDesdeDB() {
//        db.collection("evidencias_recoleccion")
//            .whereEqualTo("asignacionId", asignacionId)
//            .addSnapshotListener { snapshots, e -> // USAMOS LISTENER EN TIEMPO REAL
//                if (e != null) return@addSnapshotListener
//
//                val puntosCompletados = snapshots!!.documents.mapNotNull { it.getString("puntoId") }.toSet()
//
//                var primerPendiente = false
//                puntosRecoleccion.forEach { p ->
//                    if (puntosCompletados.contains(p.punto.id)) {
//                        p.estado = EstadoRecoleccion.COMPLETADO
//                    } else if (!primerPendiente) {
//                        p.estado = EstadoRecoleccion.EN_CURSO
//                        primerPendiente = true
//                    } else {
//                        p.estado = EstadoRecoleccion.PENDIENTE
//                    }
//                }
//                adapter.notifyDataSetChanged()
//            }
//    }
}