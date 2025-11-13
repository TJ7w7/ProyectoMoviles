package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapaGeneral : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mapa_general, container, false)

        db = FirebaseFirestore.getInstance()

        // Obtener el SupportMapFragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configurar el mapa
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true

        // Cargar todos los puntos
        cargarTodosLosPuntos()
    }
    private fun cargarTodosLosPuntos() {
        db.collection("puntos_recoleccion")
            .whereEqualTo("estado", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "No hay puntos activos para mostrar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val boundsBuilder = LatLngBounds.builder()
                var puntosCargados = 0

                for (document in documents) {
                    val punto = document.toObject(entPuntoRecoleccion::class.java)
                    if (punto.ubicacion != null) {
                        val latLng = LatLng(punto.ubicacion.latitude, punto.ubicacion.longitude)

                        // Agregar marcador
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(punto.nombre)
                                .snippet("${punto.tipo} - ${punto.zona}")
                                .icon(BitmapDescriptorFactory.defaultMarker(getColorForTipo(punto.tipo)))
                        )

                        boundsBuilder.include(latLng)
                        puntosCargados++
                    }
                }

                // Ajustar zoom para mostrar todos los markers
                if (puntosCargados > 0) {
                    val bounds = boundsBuilder.build()
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    Toast.makeText(requireContext(), "$puntosCargados puntos cargados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No se encontraron puntos con ubicación", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar puntos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    private fun getColorForTipo(tipo: String): Float {
        return when (tipo) {
            "Domicilio" -> BitmapDescriptorFactory.HUE_GREEN
            "Comercio" -> BitmapDescriptorFactory.HUE_BLUE
            "Institución" -> BitmapDescriptorFactory.HUE_ORANGE
            "Contenedor Público" -> BitmapDescriptorFactory.HUE_VIOLET
            else -> BitmapDescriptorFactory.HUE_RED
        }
    }
}