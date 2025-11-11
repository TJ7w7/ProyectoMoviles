package com.example.proyecto

import android.R.attr.fragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeDashboard : Fragment(){
    private lateinit var txtFecha: TextView
    private lateinit var cardRegistrarUsuario: MaterialCardView
    private lateinit var cardCrearRuta: MaterialCardView
//    private lateinit var cardAsignarRuta: MaterialCardView
//    private lateinit var cardEstadisticas: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home_dashboard, container, false)

        initViews(view)
        setupFecha()
        setupAccesosRapidos()

        return view
    }

    private fun initViews(view: View) {
        txtFecha = view.findViewById(R.id.txtFecha)
        cardRegistrarUsuario = view.findViewById(R.id.cardRegistrarUsuario)
        cardCrearRuta = view.findViewById(R.id.cardCrearRuta)
//        cardAsignarRuta = view.findViewById(R.id.cardAsignarRuta)
//        cardEstadisticas = view.findViewById(R.id.cardEstadisticas)
    }

    private fun setupFecha() {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", Locale("es", "ES"))
        txtFecha.text = sdf.format(Date())
    }

    private fun setupAccesosRapidos() {
        cardRegistrarUsuario.setOnClickListener {
//            navigateTo(R.id.nav_registrar_usuario)
            navigateTo(RegistroUsuarios())
        }

        cardCrearRuta.setOnClickListener {
            navigateTo(RegistrarRuta())
        }

//        cardAsignarRuta.setOnClickListener {
//            navigateTo(R.id.nav_asignar_ruta)
//        }

//        cardEstadisticas.setOnClickListener {
//            navigateTo(R.id.nav_reporte_estadisticas)
//        }
    }


    private fun navigateTo(fragment: Fragment) {
//        // Simular click en el menú del drawer
//        val activity = requireActivity() as? Administrador
////        activity?.onNavigationItemSelected(drawer)

        val activity = requireActivity() as? Administrador
        activity?.let {
            it.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}