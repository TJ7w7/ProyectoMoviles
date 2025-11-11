package com.example.proyecto

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.Adapter.UsuarioAdapter
import com.example.proyecto.Entidad.entUsuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
class GestionarUsuarios : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etBuscar: EditText
    private lateinit var spTipoUsuario: Spinner
    private lateinit var spEstado: Spinner
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var llSinDatos: View

    private val usuariosList = mutableListOf<entUsuario>()
    private val usuariosFiltrados = mutableListOf<entUsuario>()
    private lateinit var adapter: UsuarioAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gestionar_usuarios, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinners()
        setupRecyclerView()
        setupBusqueda()
        cargarUsuarios()

        return view
    }

    private fun initViews(view: View) {
        etBuscar = view.findViewById(R.id.etBuscar)
        spTipoUsuario = view.findViewById(R.id.spTipoUsuario)
        spEstado = view.findViewById(R.id.spEstado)
        rvUsuarios = view.findViewById(R.id.rvUsuarios)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupSpinners() {
        // Tipo de usuario
        val tiposUsuario = arrayOf("Todos", "Administrador", "Recolector")
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposUsuario)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoUsuario.adapter = adapterTipos

        spTipoUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarUsuarios()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Estado
        val estados = arrayOf("Todos", "Activos", "Inactivos")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados

        spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarUsuarios()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = UsuarioAdapter(
            usuariosFiltrados,
            onEstadoChange = { usuario, nuevoEstado ->
                cambiarEstadoUsuario(usuario, nuevoEstado)
            },
            onVerDetalles = { usuario ->
                mostrarDetallesUsuario(usuario)
            },
            onEditar = { usuario ->
                abrirEdicion(usuario)
                //Toast.makeText(requireContext(), "Editar ${usuario.nombres} - Próximamente", Toast.LENGTH_SHORT).show()
            }
        )

        rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        rvUsuarios.adapter = adapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun abrirEdicion(usuario: entUsuario) {
        val fragment = EditarUsuario.newInstance(usuario)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun cargarUsuarios() {
        // Usar listener en tiempo real
        listenerRegistration = db.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar usuarios: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    usuariosList.clear()
                    for (document in snapshot.documents) {
                        val usuario = document.toObject(entUsuario::class.java)
                        if (usuario != null) {
                            usuariosList.add(usuario)
                        }
                    }
                    filtrarUsuarios()
                } else {
                    usuariosList.clear()
                    filtrarUsuarios()
                }
            }
    }

    private fun filtrarUsuarios() {
        val busqueda = etBuscar.text.toString().lowercase()
        val tipoSeleccionado = spTipoUsuario.selectedItem.toString()
        val estadoSeleccionado = spEstado.selectedItem.toString()

        usuariosFiltrados.clear()

        for (usuario in usuariosList) {
            // Filtro de búsqueda
            val coincideBusqueda = busqueda.isEmpty() ||
                    usuario.nombres.lowercase().contains(busqueda) ||
                    usuario.apellidos.lowercase().contains(busqueda) ||
                    usuario.correo.lowercase().contains(busqueda) ||
                    usuario.telefono.contains(busqueda)

            // Filtro de tipo
            val coincideTipo = tipoSeleccionado == "Todos" || usuario.tipoUsuario == tipoSeleccionado

            // Filtro de estado
            val coincideEstado = when (estadoSeleccionado) {
                "Activos" -> usuario.estado
                "Inactivos" -> !usuario.estado
                else -> true
            }

            if (coincideBusqueda && coincideTipo && coincideEstado) {
                usuariosFiltrados.add(usuario)
            }
        }

        // Actualizar vista
        if (usuariosFiltrados.isEmpty()) {
            rvUsuarios.visibility = View.GONE
            llSinDatos.visibility = View.VISIBLE
        } else {
            rvUsuarios.visibility = View.VISIBLE
            llSinDatos.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    private fun cambiarEstadoUsuario(usuario: entUsuario, nuevoEstado: Boolean) {
        db.collection("usuarios")
            .document(usuario.id)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                val mensaje = if (nuevoEstado) "Usuario activado" else "Usuario desactivado"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cambiar estado: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Revertir el cambio en la UI
                filtrarUsuarios()
            }
    }

    private fun mostrarDetallesUsuario(usuario: entUsuario) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_detalle_usuario, null)

        // Configurar vistas del diálogo
        dialogView.findViewById<TextView>(R.id.txtDetalleNombre).text =
            "${usuario.nombres} ${usuario.apellidos}"
        dialogView.findViewById<TextView>(R.id.txtDetalleTipo).text =
            "Tipo: ${usuario.tipoUsuario}"
        dialogView.findViewById<TextView>(R.id.txtDetalleTelefono).text =
            "Teléfono: ${usuario.telefono}"
        dialogView.findViewById<TextView>(R.id.txtDetalleCorreo).text =
            "Correo: ${usuario.correo}"
        dialogView.findViewById<TextView>(R.id.txtDetalleEstado).text =
            "Estado: ${if (usuario.estado) "Activo" else "Inactivo"}"

        // Fecha de registro
        val fechaRegistro = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date(usuario.fechaRegistro))
        dialogView.findViewById<TextView>(R.id.txtDetalleFechaRegistro).text =
            "Registrado: $fechaRegistro"

        AlertDialog.Builder(requireContext())
            .setTitle("Detalles del Usuario")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Editar") { _, _ ->
                // TODO: Implementar edición
                Toast.makeText(requireContext(), "Editar usuario - Próximamente", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Detener el listener cuando se destruya la vista
        listenerRegistration?.remove()
    }
}