package com.tj.proyecto.Recolector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entAsignacionRuta
import com.tj.proyecto.Entidad.entUsuario
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PerfilRecolector : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombreCompleto: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtTelefono: TextView
    private lateinit var txtFechaRegistro: TextView

    private lateinit var cardEstado: MaterialCardView
    private lateinit var txtEstadoActual: TextView
    private lateinit var txtDescripcionEstado: TextView
    private lateinit var txtRolRecolector: TextView
    private lateinit var btnCambiarEstado: MaterialButton

    private lateinit var cardVehiculo: MaterialCardView
    private lateinit var txtVehiculoAsignado: TextView
    private lateinit var txtDetalleVehiculo: TextView
    private lateinit var layoutSinVehiculo: View

    private lateinit var cardRutaActual: MaterialCardView
    private lateinit var txtRutaActual: TextView
    private lateinit var txtDetalleRuta: TextView
    private lateinit var btnVerRuta: MaterialButton

    private lateinit var btnEditarPerfil: MaterialButton
    private var usuarioActual: entUsuario? = null
    private var asignacionActual: entAsignacionRuta? = null
    private var esAyudante: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_perfil_recolector, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews(view)
        cargarDatosUsuario()
        cargarAsignacionActual()

        btnEditarPerfil.setOnClickListener {
            editarPerfil()
        }

        btnCambiarEstado.setOnClickListener {
            cambiarEstado()
        }

        btnVerRuta.setOnClickListener {
            verDetalleRuta()
        }

        return view
    }
    private fun initViews(view: View) {
        // Datos personales
        txtNombreCompleto = view.findViewById(R.id.txtNombreCompleto)
        txtEmail = view.findViewById(R.id.txtEmail)
        txtTelefono = view.findViewById(R.id.txtTelefono)
        txtFechaRegistro = view.findViewById(R.id.txtFechaRegistro)

        // Estado
        cardEstado = view.findViewById(R.id.cardEstado)
        txtEstadoActual = view.findViewById(R.id.txtEstadoActual)
        txtDescripcionEstado = view.findViewById(R.id.txtDescripcionEstado)
        txtRolRecolector = view.findViewById(R.id.txtRolRecolector)
        btnCambiarEstado = view.findViewById(R.id.btnCambiarEstado)

        // Vehículo
        cardVehiculo = view.findViewById(R.id.cardVehiculo)
        txtVehiculoAsignado = view.findViewById(R.id.txtVehiculoAsignado)
        txtDetalleVehiculo = view.findViewById(R.id.txtDetalleVehiculo)
        layoutSinVehiculo = view.findViewById(R.id.layoutSinVehiculo)

        // Ruta actual
        cardRutaActual = view.findViewById(R.id.cardRutaActual)
        txtRutaActual = view.findViewById(R.id.txtRutaActual)
        txtDetalleRuta = view.findViewById(R.id.txtDetalleRuta)
        btnVerRuta = view.findViewById(R.id.btnVerRuta)

        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil)
    }

    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar datos: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    usuarioActual = snapshot.toObject(entUsuario::class.java)
                    mostrarDatosUsuario()
                }
            }
    }

    private fun mostrarDatosUsuario() {
        usuarioActual?.let { usuario ->
            txtNombreCompleto.text = "${usuario.nombres} ${usuario.apellidos}"
            txtEmail.text = usuario.correo
            txtTelefono.text = if (usuario.telefono.isNotEmpty()) {
                usuario.telefono
            } else {
                "No registrado"
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            txtFechaRegistro.text = "Miembro desde ${sdf.format(usuario.fechaRegistro)}"

            // Mostrar estado de la cuenta
            if (usuario.estado) {
                actualizarEstado("Activo")
            } else {
                actualizarEstado("Inactivo")
            }
        }
    }

    private fun actualizarEstado(estado: String) {
        txtEstadoActual.text = estado

        when (estado) {
            "Activo" -> {
                if (asignacionActual == null) {
                    cardEstado.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#4CAF50")
                    )
                    txtDescripcionEstado.text = "Disponible para asignaciones"
                    btnCambiarEstado.visibility = View.GONE
                }
            }
            "En Ruta" -> {
                cardEstado.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#FF9800")
                )
                txtDescripcionEstado.text = "Realizando recolección"
                btnCambiarEstado.text = "Completar Ruta"
                btnCambiarEstado.visibility = if (esAyudante) View.GONE else View.VISIBLE
            }
            "Inactivo" -> {
                cardEstado.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#F44336")
                )
                txtDescripcionEstado.text = "Cuenta desactivada"
                btnCambiarEstado.visibility = View.GONE
            }
        }
    }

    private fun cargarAsignacionActual() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar como conductor
        db.collection("asignaciones_rutas")
            .whereEqualTo("conductorId", userId)
            .whereIn("estado", listOf("Programada", "En Progreso"))
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    asignacionActual = snapshots.documents[0].toObject(entAsignacionRuta::class.java)
                    esAyudante = false
                    mostrarAsignacion()
                } else {
                    // Buscar como ayudante
                    buscarComoAyudante(userId)
                }
            }
    }

    private fun buscarComoAyudante(userId: String) {
        db.collection("asignaciones_rutas")
            .whereArrayContains("ayudantesIds", userId)
            .whereIn("estado", listOf("Programada", "En Progreso"))
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    asignacionActual = snapshots.documents[0].toObject(entAsignacionRuta::class.java)
                    esAyudante = true
                    mostrarAsignacion()
                } else {
                    // No tiene asignación
                    mostrarSinAsignacion()
                }
            }
    }

    private fun mostrarAsignacion() {
        asignacionActual?.let { asignacion ->
            cardRutaActual.visibility = View.VISIBLE
            cardVehiculo.visibility = View.VISIBLE

            // Actualizar estado según la asignación
            when (asignacion.estado) {
                "Programada" -> {
                    txtEstadoActual.text = "Ruta Programada"
                    cardEstado.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#2196F3")
                    )
                    txtDescripcionEstado.text = "Tiene una ruta asignada"
                    btnCambiarEstado.text = "Iniciar Ruta"
                    btnCambiarEstado.visibility = if (esAyudante) View.GONE else View.VISIBLE
                }
                "En Progreso" -> {
                    actualizarEstado("En Ruta")
                }
            }

            // Mostrar rol
            txtRolRecolector.text = if (esAyudante) "Rol: Ayudante" else "Rol: Conductor"
            txtRolRecolector.visibility = View.VISIBLE

            // Ruta
            txtRutaActual.text = asignacion.rutaNombre
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            txtDetalleRuta.text = """
                📍 ${asignacion.rutaCodigo}
                📅 ${sdf.format(asignacion.fechaAsignacion)}
                👤 Conductor: ${asignacion.conductorNombre}
                👥 Equipo: ${asignacion.ayudantesNombres.size + 1} personas
            """.trimIndent()

            // Vehículo
            txtVehiculoAsignado.text = asignacion.vehiculoPlaca
            cargarDetalleVehiculo(asignacion.vehiculoId)
            layoutSinVehiculo.visibility = View.GONE
        }
    }

    private fun mostrarSinAsignacion() {
        asignacionActual = null
        esAyudante = false

        cardRutaActual.visibility = View.GONE
        cardVehiculo.visibility = View.VISIBLE
        layoutSinVehiculo.visibility = View.VISIBLE
        txtVehiculoAsignado.visibility = View.GONE
        txtDetalleVehiculo.visibility = View.GONE
        txtRolRecolector.visibility = View.GONE

        // Restablecer estado
        if (usuarioActual?.estado == true) {
            actualizarEstado("Activo")
        }
    }

    private fun cargarDetalleVehiculo(vehiculoId: String) {
        db.collection("vehiculos")
            .document(vehiculoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tipo = document.getString("tipoVehiculo") ?: ""
                    val capacidad = document.getDouble("capacidadKg") ?: 0.0
                    val observaciones = document.getString("observaciones") ?: ""

                    txtDetalleVehiculo.text = """
                        🚛 Tipo: $tipo
                        📦 Capacidad: $capacidad Kg
                        ${if (observaciones.isNotEmpty()) "📝 $observaciones" else ""}
                    """.trimIndent()

                    txtVehiculoAsignado.visibility = View.VISIBLE
                    txtDetalleVehiculo.visibility = View.VISIBLE
                }
            }
    }

    private fun editarPerfil() {
        val fragment = EditarPerfilRecolector.newInstance(usuarioActual)
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

    private fun cambiarEstado() {
        asignacionActual?.let { asignacion ->
            when (asignacion.estado) {
                "Programada" -> iniciarRuta()
                "En Progreso" -> completarRuta()
            }
        }
    }

    private fun iniciarRuta() {
        if (esAyudante) {
            Toast.makeText(
                requireContext(),
                "Solo el conductor puede iniciar la ruta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Iniciar Ruta")
            .setMessage("¿Está listo para comenzar la recolección?")
            .setPositiveButton("Iniciar") { _, _ ->
                asignacionActual?.let { asignacion ->
                    db.collection("asignaciones_rutas")
                        .document(asignacion.id)
                        .update("estado", "En Progreso")
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "✓ Ruta iniciada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun completarRuta() {
        if (esAyudante) {
            Toast.makeText(
                requireContext(),
                "Solo el conductor puede completar la ruta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Completar Ruta")
            .setMessage("¿Ha finalizado la recolección de todos los puntos?")
            .setPositiveButton("Completar") { _, _ ->
                asignacionActual?.let { asignacion ->
                    val updates = hashMapOf<String, Any>(
                        "estado" to "Completada",
                        "fechaEjecucion" to System.currentTimeMillis()
                    )

                    db.collection("asignaciones_rutas")
                        .document(asignacion.id)
                        .update(updates)
                        .addOnSuccessListener {
                            // Liberar vehículo
                            db.collection("vehiculos")
                                .document(asignacion.vehiculoId)
                                .update("estado", "Disponible")

                            Toast.makeText(
                                requireContext(),
                                "✓ Ruta completada exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verDetalleRuta() {
        asignacionActual?.let { asignacion ->
            val fragment = com.tj.proyecto.DetalleAsignacion.newInstance(asignacion.id)
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
}