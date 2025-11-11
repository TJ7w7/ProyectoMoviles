package com.example.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.example.proyecto.Entidad.entUsuario
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarUsuario : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var usuario: entUsuario

    private lateinit var txtTipoUsuario: TextView
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etTelefono: EditText
    private lateinit var txtCorreo: TextView
    private lateinit var switchEstado: SwitchCompat
    private lateinit var txtInfoRegistro: TextView
    private lateinit var btnGuardarCambios: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    companion object {
        private const val ARG_USUARIO_ID = "usuario_id"
        private const val ARG_TIPO_USUARIO = "tipo_usuario"
        private const val ARG_NOMBRES = "nombres"
        private const val ARG_APELLIDOS = "apellidos"
        private const val ARG_TELEFONO = "telefono"
        private const val ARG_CORREO = "correo"
        private const val ARG_ESTADO = "estado"
        private const val ARG_FECHA_REGISTRO = "fecha_registro"

        fun newInstance(usuario: entUsuario): EditarUsuario {
            val fragment = EditarUsuario()
            val args = Bundle()
            args.putString(ARG_USUARIO_ID, usuario.id)
            args.putString(ARG_TIPO_USUARIO, usuario.tipoUsuario)
            args.putString(ARG_NOMBRES, usuario.nombres)
            args.putString(ARG_APELLIDOS, usuario.apellidos)
            args.putString(ARG_TELEFONO, usuario.telefono)
            args.putString(ARG_CORREO, usuario.correo)
            args.putBoolean(ARG_ESTADO, usuario.estado)
            args.putLong(ARG_FECHA_REGISTRO, usuario.fechaRegistro)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // Reconstruir usuario desde argumentos
        arguments?.let {
            usuario = entUsuario(
                id = it.getString(ARG_USUARIO_ID, ""),
                tipoUsuario = it.getString(ARG_TIPO_USUARIO, ""),
                nombres = it.getString(ARG_NOMBRES, ""),
                apellidos = it.getString(ARG_APELLIDOS, ""),
                telefono = it.getString(ARG_TELEFONO, ""),
                correo = it.getString(ARG_CORREO, ""),
                estado = it.getBoolean(ARG_ESTADO, true),
                fechaRegistro = it.getLong(ARG_FECHA_REGISTRO, System.currentTimeMillis())
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_editar_usuario, container, false)

        initViews(view)
        cargarDatosUsuario()

        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtTipoUsuario = view.findViewById(R.id.txtTipoUsuario)
        etNombres = view.findViewById(R.id.etNombres)
        etApellidos = view.findViewById(R.id.etApellidos)
        etTelefono = view.findViewById(R.id.etTelefono)
        txtCorreo = view.findViewById(R.id.txtCorreo)
        switchEstado = view.findViewById(R.id.switchEstado)
        txtInfoRegistro = view.findViewById(R.id.txtInfoRegistro)
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun cargarDatosUsuario() {
        // Tipo de usuario
        txtTipoUsuario.text = usuario.tipoUsuario
        if (usuario.tipoUsuario == "Administrador") {
            txtTipoUsuario.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_purple)
            )
        }

        // Datos editables
        etNombres.setText(usuario.nombres)
        etApellidos.setText(usuario.apellidos)
        etTelefono.setText(usuario.telefono)

        // Correo (solo lectura)
        txtCorreo.text = usuario.correo

        // Estado
        switchEstado.isChecked = usuario.estado

        // Fecha de registro
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        txtInfoRegistro.text = "Registrado el: ${sdf.format(Date(usuario.fechaRegistro))}"
    }

    private fun guardarCambios() {
        // Validar campos
        if (!validarCampos()) {
            return
        }

        btnGuardarCambios.isEnabled = false
        btnGuardarCambios.text = "Guardando..."

        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val estado = switchEstado.isChecked

        // Preparar los datos a actualizar
        val updates = hashMapOf<String, Any>(
            "nombres" to nombres,
            "apellidos" to apellidos,
            "telefono" to telefono,
            "estado" to estado
        )

        // Actualizar en Firestore
        db.collection("usuarios")
            .document(usuario.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Usuario actualizado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Volver atrás
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar usuario: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardarCambios.isEnabled = true
                btnGuardarCambios.text = "Guardar Cambios"
            }
    }

    private fun validarCampos(): Boolean {
        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        when {
            nombres.isEmpty() -> {
                etNombres.error = "Ingrese los nombres"
                etNombres.requestFocus()
                return false
            }
            apellidos.isEmpty() -> {
                etApellidos.error = "Ingrese los apellidos"
                etApellidos.requestFocus()
                return false
            }
            telefono.isEmpty() -> {
                etTelefono.error = "Ingrese el teléfono"
                etTelefono.requestFocus()
                return false
            }
            telefono.length != 9 -> {
                etTelefono.error = "El teléfono debe tener 9 dígitos"
                etTelefono.requestFocus()
                return false
            }
        }

        return true
    }
}