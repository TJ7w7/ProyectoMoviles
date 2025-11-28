package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tj.proyecto.Adapter.IncidenciasAdapter
import com.tj.proyecto.Entidad.entIncidencia


class Incidencias : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var spEstado: Spinner
    private lateinit var spTipo: Spinner
    private lateinit var rvIncidencias: RecyclerView
    private lateinit var llSinDatos: View

    // Lista maestra (todos los datos) y adaptador
    private var listaCompleta = listOf<entIncidencia>()
    private lateinit var adapter: IncidenciasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_incidencias, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        setupSpinners() // Configurar spinners y sus listeners
        cargarIncidencias() // Descargar datos

        return view
    }

    private fun initViews(view: View) {
        spEstado = view.findViewById(R.id.spEstado)
        spTipo = view.findViewById(R.id.spTipo)
        rvIncidencias = view.findViewById(R.id.rvIncidencias)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupRecyclerView() {
        // Inicializamos el adapter con lista vacía y la acción de click
        adapter = IncidenciasAdapter(listOf()) { incidencia ->
            irADetalleAsignacion(incidencia.asignacionId)
        }
        rvIncidencias.layoutManager = LinearLayoutManager(requireContext())
        rvIncidencias.adapter = adapter
    }

    private fun setupSpinners() {
        // 1. Spinner Estados
        val estados = arrayOf("Todas", "Pendiente", "Resuelta") // Simplificado a lo que usamos
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        // 2. Spinner Tipos (Motivos)
        val tipos = arrayOf("Todos", "QR Dañado / No visible", "Calle/Acceso Bloqueado", "Contenedor no existe", "Obra en la vía", "Otro")
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapterTipos

        // 3. Listeners para filtrar cuando cambian
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                aplicarFiltros()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spEstado.onItemSelectedListener = listener
        spTipo.onItemSelectedListener = listener
    }

    private fun cargarIncidencias() {
        // Escuchamos en tiempo real
        db.collection("incidencias")
            .orderBy("fechaReporte", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Error cargando incidencias", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    val listaTemp = mutableListOf<entIncidencia>()
                    for (doc in value) {
                        val obj = doc.toObject(entIncidencia::class.java)
                        obj.id = doc.id // Guardamos el ID del documento
                        listaTemp.add(obj)
                    }
                    listaCompleta = listaTemp
                    aplicarFiltros() // Mostramos los datos filtrados
                }
            }
    }

    private fun aplicarFiltros() {
        val estadoSeleccionado = spEstado.selectedItem.toString()
        val tipoSeleccionado = spTipo.selectedItem.toString()

        val listaFiltrada = listaCompleta.filter { item ->
            // Filtro de Estado
            val pasaEstado = (estadoSeleccionado == "Todas") || (item.estado == estadoSeleccionado)

            // Filtro de Tipo (Buscamos si el motivo contiene la palabra clave o es exacto)
            val pasaTipo = (tipoSeleccionado == "Todos") || (item.motivo == tipoSeleccionado)

            pasaEstado && pasaTipo
        }

        adapter.actualizarLista(listaFiltrada)

        // Mostrar u ocultar mensaje de "Sin datos"
        if (listaFiltrada.isEmpty()) {
            llSinDatos.visibility = View.VISIBLE
            rvIncidencias.visibility = View.GONE
        } else {
            llSinDatos.visibility = View.GONE
            rvIncidencias.visibility = View.VISIBLE
        }
    }

    private fun irADetalleAsignacion(asignacionId: String) {
        // Navegamos a la pantalla donde el Admin puede VALIDAR/SALTAR el punto
        val fragment = DetalleAsignacion.newInstance(asignacionId)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Asegúrate que este ID sea el correcto de tu FrameLayout principal
            .addToBackStack(null)
            .commit()
    }
}