package com.tj.proyecto.Recolector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.MisAsignacionesAdapter
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MisAsignaciones : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvAsignaciones: RecyclerView
    private lateinit var layoutSinDatos: View
    private lateinit var txtTitulo: TextView

    private val asignacionesList = mutableListOf<entAsignacionRuta>()
    private lateinit var adapter: MisAsignacionesAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mis_asignaciones, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews(view)
        setupRecyclerView()
        cargarAsignaciones()

        return view
    }

    private fun initViews(view: View) {
        txtTitulo = view.findViewById(R.id.txtTitulo)
        rvAsignaciones = view.findViewById(R.id.rvAsignaciones)
        layoutSinDatos = view.findViewById(R.id.layoutSinDatos)
    }

    private fun setupRecyclerView() {
        adapter = MisAsignacionesAdapter(asignacionesList) { asignacion ->
            abrirDetalleAsignacion(asignacion)
        }
        rvAsignaciones.layoutManager = LinearLayoutManager(requireContext())
        rvAsignaciones.adapter = adapter
    }

    private fun cargarAsignaciones() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar como conductor
        db.collection("asignaciones_rutas")
            .whereEqualTo("conductorId", userId)
            .orderBy("fechaAsignacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                asignacionesList.clear()
                if (snapshots != null) {
                    for (document in snapshots) {
                        val asignacion = document.toObject(entAsignacionRuta::class.java)
                        asignacionesList.add(asignacion)
                    }
                }

                // Buscar como ayudante
                buscarComoAyudante(userId)
            }
    }

    private fun buscarComoAyudante(userId: String) {
        db.collection("asignaciones_rutas")
            .whereArrayContains("ayudantesIds", userId)
            .orderBy("fechaAsignacion", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                for (document in snapshots) {
                    val asignacion = document.toObject(entAsignacionRuta::class.java)
                    // Evitar duplicados
                    if (!asignacionesList.any { it.id == asignacion.id }) {
                        asignacionesList.add(asignacion)
                    }
                }

                // Ordenar por fecha
                asignacionesList.sortByDescending { it.fechaAsignacion }
                adapter.notifyDataSetChanged()

                if (asignacionesList.isEmpty()) {
                    layoutSinDatos.visibility = View.VISIBLE
                    rvAsignaciones.visibility = View.GONE
                } else {
                    layoutSinDatos.visibility = View.GONE
                    rvAsignaciones.visibility = View.VISIBLE
                }
            }
    }

    private fun abrirDetalleAsignacion(asignacion: entAsignacionRuta) {
        if (asignacion.estado == "Programada" || asignacion.estado == "En Progreso") {
            // Abrir en modo de ruta activa
            val fragment = RecolectorRuta.newInstance(asignacion.id)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } else {
            // Ver solo detalles
            val fragment = com.tj.proyecto.DetalleAsignacion.newInstance(asignacion.id)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}