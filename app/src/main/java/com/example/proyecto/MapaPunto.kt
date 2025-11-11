package com.example.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapaPunto : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var punto: entPuntoRecoleccion? = null

    companion object {
        private const val ARG_PUNTO = "punto"

        fun newInstance(punto: entPuntoRecoleccion): MapaPunto {
            val fragment = MapaPunto()
            val args = Bundle().apply {
                putSerializable(ARG_PUNTO, punto)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            punto = it.getSerializable(ARG_PUNTO) as? entPuntoRecoleccion
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mapa_punto, container, false)

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

        // Mostrar el punto en el mapa
        mostrarPuntoEnMapa()
    }

    private fun mostrarPuntoEnMapa() {
        val punto = this.punto
        if (punto?.ubicacion != null) {
            val latLng = LatLng(punto.ubicacion.latitude, punto.ubicacion.longitude)

            // Agregar marcador
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(punto.nombre)
                    .snippet("${punto.tipo} - ${punto.direccion}")
                    .icon(BitmapDescriptorFactory.defaultMarker(getColorForTipo(punto.tipo)))
            )

            // Mover cámara al punto
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            // Mostrar info window
            googleMap.setOnMapLoadedCallback {
                // El marcador se mostrará automáticamente
            }
        } else {
            Toast.makeText(requireContext(), "El punto no tiene ubicación registrada", Toast.LENGTH_SHORT).show()
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