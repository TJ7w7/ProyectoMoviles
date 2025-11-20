package com.tj.proyecto

import android.os.Bundle
import android.util.Log
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

//    private fun registrarUsuario() {
//        // Validar campos
//        if (!validarCampos()) {
//            return
//        }
//
//        // Mostrar progress
//        btnRegistrar.isEnabled = false
//        btnRegistrar.text = "Registrando..."
//
//        val correo = etCorreo.text.toString().trim()
//        val password = etPassword.text.toString().trim()
//
//        // Crear usuario en Firebase Authentication
//        auth.createUserWithEmailAndPassword(correo, password)
//            .addOnSuccessListener { authResult ->
//                val userId = authResult.user?.uid ?: return@addOnSuccessListener
//
//                // Crear objeto Usuario
//                val tipoUsuario = if (rbAdministrador.isChecked) "Administrador" else "Recolector"
//                val usuario = entUsuario(
//                    id = userId,
//                    tipoUsuario = tipoUsuario,
//                    nombres = etNombres.text.toString().trim(),
//                    apellidos = etApellidos.text.toString().trim(),
//                    telefono = etTelefono.text.toString().trim(),
//                    correo = correo,
//                    fechaRegistro = System.currentTimeMillis(),
//                    estado = true,
//                    registradoPor = auth.currentUser?.uid ?: ""
//                )
//
//                // Guardar en Firestore
//                db.collection("usuarios")
//                    .document(userId)
//                    .set(usuario)
//                    .addOnSuccessListener {
//                        Toast.makeText(
//                            requireContext(),
//                            "Usuario registrado exitosamente",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        limpiarCampos()
//                        btnRegistrar.isEnabled = true
//                        btnRegistrar.text = "Registrar Usuario"
//
//                        // Opcional: volver atrás
//                        requireActivity().supportFragmentManager.popBackStack()
//                    }
//                    .addOnFailureListener { e ->
//                        Toast.makeText(
//                            requireContext(),
//                            "Error al guardar datos: ${e.message}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                        btnRegistrar.isEnabled = true
//                        btnRegistrar.text = "Registrar Usuario"
//                    }
//            }
//            .addOnFailureListener { e ->
//                var mensaje = "Error al crear usuario"
//                when {
//                    e.message?.contains("email address is already") == true ->
//                        mensaje = "El correo ya está registrado"
//                    e.message?.contains("password") == true ->
//                        mensaje = "La contraseña debe tener al menos 6 caracteres"
//                    e.message?.contains("network") == true ->
//                        mensaje = "Error de conexión. Verifica tu internet"
//                }
//
//                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
//                btnRegistrar.isEnabled = true
//                btnRegistrar.text = "Registrar Usuario"
//            }
//    }

    private fun registrarUsuario() {
        if (!validarCampos()) {
            return
        }

        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                val tipoUsuario = if (rbAdministrador.isChecked) "Administrador" else "Recolector"
                val usuario = entUsuario(
                    id = userId,
                    tipoUsuario = tipoUsuario,
                    nombres = nombres,
                    apellidos = apellidos,
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
                        // Enviar correo con contraseña temporal
                        enviarCorreoBienvenida(correo, nombres, apellidos, password, tipoUsuario)

                        Toast.makeText(
                            requireContext(),
                            "Usuario registrado. Correo enviado exitosamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                        limpiarCampos()
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrar Usuario"
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

    private fun enviarCorreoBienvenida(
        correo: String,
        nombres: String,
        apellidos: String,
        password: String,
        tipoUsuario: String
    ) {
        // Crear documento en colección 'mail' (usado por Firebase Extensions)
        val emailData = hashMapOf(
            "to" to correo,
            "message" to hashMapOf(
                "subject" to "Bienvenido a EcoManager",
                "text" to """
                Hola $nombres $apellidos,
                
                Tu cuenta ha sido creada exitosamente en nuestro sistema.
                
                Detalles de tu cuenta:
                - Tipo de usuario: $tipoUsuario
                - Correo: $correo
                - Contraseña temporal: $password
                
                Por favor, cambia tu contraseña después de iniciar sesión por primera vez.
                
                Saludos,
                Equipo de EcoManager
            """.trimIndent(),
                "html" to """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <h2 style="color: #4CAF50; text-align: center;">Bienvenido al Sistema</h2>
                        <p>Hola <strong>$nombres $apellidos</strong>,</p>
                        <p>Tu cuenta ha sido creada exitosamente en nuestro sistema.</p>
                        
                        <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <h3 style="color: #333; margin-top: 0;">Detalles de tu cuenta:</h3>
                            <p><strong>Tipo de usuario:</strong> $tipoUsuario</p>
                            <p><strong>Correo:</strong> $correo</p>
                            <p><strong>Contraseña temporal:</strong> <span style="background-color: #ffeb3b; padding: 5px 10px; border-radius: 3px; font-family: monospace;">$password</span></p>
                        </div>
                        
                        <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>⚠️ Importante:</strong> Por seguridad, te recomendamos cambiar tu contraseña después de iniciar sesión por primera vez.</p>
                        </div>
                        
                        <p style="text-align: center; color: #666; margin-top: 30px;">
                            Si tienes alguna pregunta, no dudes en contactarnos.
                        </p>
                        
                        <p style="text-align: center; color: #999; font-size: 12px; margin-top: 20px;">
                            © 2025 Sistema EcoManager
                        </p>
                    </div>
                </body>
                </html>
            """.trimIndent()
            )
        )

        db.collection("mail")
            .add(emailData)
            .addOnSuccessListener {
                Log.d("RegistroUsuarios", "Correo programado para envío")
            }
            .addOnFailureListener { e ->
                Log.e("RegistroUsuarios", "Error al programar correo: ${e.message}")
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