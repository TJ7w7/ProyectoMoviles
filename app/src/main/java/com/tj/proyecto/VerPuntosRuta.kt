package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.VerPuntosAdapter
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class VerPuntosRuta : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var rutaId: String
    private lateinit var nombreRuta: String

    private lateinit var txtNombreRuta: TextView
    private lateinit var txtTotalPuntos: TextView
    private lateinit var rvPuntosAsignados: RecyclerView
    private lateinit var btnEditarPuntos: MaterialButton
    private lateinit var btnVolver: MaterialButton
    private lateinit var layoutSinPuntos: View

    private val puntosAsignados = mutableListOf<PuntoConOrden>()
    private lateinit var adapter: VerPuntosAdapter

    data class PuntoConOrden(
        val punto: entPuntoRecoleccion,
        val orden: Int,
        val fechaAsignacion: Long
    )

    companion object {
        fun newInstance(rutaId: String, nombreRuta: String): VerPuntosRuta {
            val fragment = VerPuntosRuta()
            val args = Bundle()
            args.putString("rutaId", rutaId)
            args.putString("nombreRuta", nombreRuta)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rutaId = arguments?.getString("rutaId") ?: ""
        nombreRuta = arguments?.getString("nombreRuta") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ver_puntos_ruta, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        cargarPuntosAsignados()

        btnEditarPuntos.setOnClickListener {
            editarPuntos()
        }

        btnVolver.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtNombreRuta = view.findViewById(R.id.txtNombreRuta)
        txtTotalPuntos = view.findViewById(R.id.txtTotalPuntos)
        rvPuntosAsignados = view.findViewById(R.id.rvPuntosAsignados)
        btnEditarPuntos = view.findViewById(R.id.btnEditarPuntos)
        btnVolver = view.findViewById(R.id.btnVolver)
        layoutSinPuntos = view.findViewById(R.id.layoutSinPuntos)

        txtNombreRuta.text = nombreRuta
    }

    private fun setupRecyclerView() {
        adapter = VerPuntosAdapter(puntosAsignados)
        rvPuntosAsignados.layoutManager = LinearLayoutManager(requireContext())
        rvPuntosAsignados.adapter = adapter
    }

    private fun cargarPuntosAsignados() {
        db.collection("ruta_puntos")
            .whereEqualTo("rutaId", rutaId)
            .orderBy("orden")
            .get()
            .addOnSuccessListener { rutaPuntos ->
                if (rutaPuntos.isEmpty) {
                    mostrarSinPuntos()
                    return@addOnSuccessListener
                }

                puntosAsignados.clear()
                var puntosRestantes = rutaPuntos.size()

                for (rutaPunto in rutaPuntos) {
                    val puntoId = rutaPunto.getString("puntoId") ?: ""
                    val orden = rutaPunto.getLong("orden")?.toInt() ?: 0
                    val fechaAsignacion = rutaPunto.getLong("fechaAsignacion") ?: 0L

                    db.collection("puntos_recoleccion")
                        .document(puntoId)
                        .get()
                        .addOnSuccessListener { puntoDoc ->
                            if (puntoDoc.exists()) {
                                val punto = puntoDoc.toObject(entPuntoRecoleccion::class.java)
                                if (punto != null) {
                                    puntosAsignados.add(PuntoConOrden(punto, orden, fechaAsignacion))
                                }
                            }

                            puntosRestantes--
                            if (puntosRestantes == 0) {
                                puntosAsignados.sortBy { it.orden }
                                actualizarLista()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cargar puntos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                mostrarSinPuntos()
            }
    }

    private fun actualizarLista() {
        adapter.notifyDataSetChanged()
        txtTotalPuntos.text = "${puntosAsignados.size} puntos en la ruta"

        if (puntosAsignados.isEmpty()) {
            mostrarSinPuntos()
        } else {
            layoutSinPuntos.visibility = View.GONE
            rvPuntosAsignados.visibility = View.VISIBLE
        }
    }

    private fun mostrarSinPuntos() {
        layoutSinPuntos.visibility = View.VISIBLE
        rvPuntosAsignados.visibility = View.GONE
        txtTotalPuntos.text = "0 puntos en la ruta"
        btnEditarPuntos.text = "Asignar Puntos"
    }

    private fun editarPuntos() {
        val fragment = AsignarPuntosRuta.newInstance(rutaId, nombreRuta)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}