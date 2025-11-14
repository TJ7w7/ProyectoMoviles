package com.tj.proyecto.Recolector

import android.os.Bundle
import android.util.Log
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
    private lateinit var btnEditarPerfil: MaterialButton

    private var usuarioActual: entUsuario? = null

    companion object {
        private const val TAG = "PerfilRecolector"
    }
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

        setupListeners()

        return view
    }
    private fun initViews(view: View) {
        txtNombreCompleto = view.findViewById(R.id.txtNombreCompleto)
        txtEmail = view.findViewById(R.id.txtEmail)
        txtTelefono = view.findViewById(R.id.txtTelefono)
        txtFechaRegistro = view.findViewById(R.id.txtFechaRegistro)
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil)
    }

    private fun setupListeners() {
        btnEditarPerfil.setOnClickListener {
            editarPerfil()
        }


    }

    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al cargar usuario: ${error.message}", error)
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
}