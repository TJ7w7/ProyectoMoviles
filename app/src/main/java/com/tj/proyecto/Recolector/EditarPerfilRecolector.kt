package com.tj.proyecto.Recolector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.tj.proyecto.R
import com.tj.proyecto.Entidad.entUsuario
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilRecolector : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombreCompleto: TextView
    private lateinit var txtEmail: TextView
    private lateinit var etTelefono: EditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private var usuarioActual: entUsuario? = null

    companion object {
        fun newInstance(usuario: entUsuario?): EditarPerfilRecolector {
            val fragment = EditarPerfilRecolector()
            val args = Bundle()
            usuario?.let {
                args.putString("nombres", it.nombres)
                args.putString("apellidos", it.apellidos)
                args.putString("correo", it.correo)
                args.putString("telefono", it.telefono)
            }
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_editar_perfil_recolector, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews(view)
        cargarDatosActuales()

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtNombreCompleto = view.findViewById(R.id.txtNombreCompleto)
        txtEmail = view.findViewById(R.id.txtEmail)
        etTelefono = view.findViewById(R.id.etTelefono)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun cargarDatosActuales() {
        val nombres = arguments?.getString("nombres") ?: ""
        val apellidos = arguments?.getString("apellidos") ?: ""
        val correo = arguments?.getString("correo") ?: ""
        val telefono = arguments?.getString("telefono") ?: ""

        txtNombreCompleto.text = "$nombres $apellidos"
        txtEmail.text = correo
        etTelefono.setText(telefono)
    }

    private fun guardarCambios() {
        val telefono = etTelefono.text.toString().trim()

        if (telefono.isEmpty()) {
            etTelefono.error = "Ingrese su teléfono"
            etTelefono.requestFocus()
            return
        }

        if (telefono.length < 9) {
            etTelefono.error = "Teléfono debe tener al menos 9 dígitos"
            etTelefono.requestFocus()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios")
            .document(userId)
            .update("telefono", telefono)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "✓ Teléfono actualizado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar Cambios"
            }
    }

}