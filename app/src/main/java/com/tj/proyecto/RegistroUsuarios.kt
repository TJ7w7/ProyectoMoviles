package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.tj.proyecto.Entidad.entUsuario
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroUsuarios : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var rgTipoUsuario: RadioGroup
    private lateinit var rbAdministrador: RadioButton
    private lateinit var rbTrabajador: RadioButton
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registro_usuarios, container, false)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar views
        initViews(view)

        // Configurar listeners
        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        rgTipoUsuario = view.findViewById(R.id.rgTipoUsuario)
        rbAdministrador = view.findViewById(R.id.rbAdministrador)
        rbTrabajador = view.findViewById(R.id.rbTrabajador)
        etNombres = view.findViewById(R.id.etNombres)
        etApellidos = view.findViewById(R.id.etApellidos)
        etTelefono = view.findViewById(R.id.etTelefono)
        etCorreo = view.findViewById(R.id.etCorreo)
        etPassword = view.findViewById(R.id.etPassword)
        btnRegistrar = view.findViewById(R.id.btnRegistrar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun registrarUsuario() {
        // Validar campos
        if (!validarCampos()) {
            return
        }

        // Mostrar progress
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Crear objeto Usuario
                val tipoUsuario = if (rbAdministrador.isChecked) "Administrador" else "Recolector"
                val usuario = entUsuario(
                    id = userId,
                    tipoUsuario = tipoUsuario,
                    nombres = etNombres.text.toString().trim(),
                    apellidos = etApellidos.text.toString().trim(),
                    telefono = etTelefono.text.toString().trim(),
                    correo = correo,
                    fechaRegistro = System.currentTimeMillis(),
                    estado = true,
                    registradoPor = auth.currentUser?.uid ?: ""
                )

                // Guardar en Firestore
                db.collection("usuarios")
                    .document(userId)
                    .set(usuario)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Usuario registrado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        limpiarCampos()
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrar Usuario"

                        // Opcional: volver atrás
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error al guardar datos: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrar Usuario"
                    }
            }
            .addOnFailureListener { e ->
                var mensaje = "Error al crear usuario"
                when {
                    e.message?.contains("email address is already") == true ->
                        mensaje = "El correo ya está registrado"
                    e.message?.contains("password") == true ->
                        mensaje = "La contraseña debe tener al menos 6 caracteres"
                    e.message?.contains("network") == true ->
                        mensaje = "Error de conexión. Verifica tu internet"
                }

                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar Usuario"
            }
    }

    private fun validarCampos(): Boolean {
        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()

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
            correo.isEmpty() -> {
                etCorreo.error = "Ingrese el correo"
                etCorreo.requestFocus()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                etCorreo.error = "Correo inválido"
                etCorreo.requestFocus()
                return false
            }
            password.isEmpty() -> {
                etPassword.error = "Ingrese la contraseña"
                etPassword.requestFocus()
                return false
            }
            password.length < 6 -> {
                etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                return false
            }
        }

        return true
    }

    private fun limpiarCampos() {
        etNombres.text.clear()
        etApellidos.text.clear()
        etTelefono.text.clear()
        etCorreo.text.clear()
        etPassword.text.clear()
        rbTrabajador.isChecked = true
    }

}