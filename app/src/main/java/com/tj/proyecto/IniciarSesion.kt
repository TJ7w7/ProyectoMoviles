package com.tj.proyecto

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.tj.proyecto.Recolector.Recolector
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class IniciarSesion : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIngresar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_iniciar_sesion)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los campos del layout
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnIngresar = findViewById(R.id.btnIngresar)

        btnIngresar.setOnClickListener {
            iniciarSesion()
        }
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        btnIngresar.isEnabled = false
        btnIngresar.text = "Ingresando..."

        auth.signInWithEmailAndPassword(correo, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Buscar en Firestore el tipo de usuario
                db.collection("usuarios").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val tipoUsuario = document.getString("tipoUsuario")

                            when (tipoUsuario) {
                                "Administrador" -> {
                                    startActivity(Intent(this, Administrador::class.java))
                                    finish()
                                }
                                "Recolector", "Trabajador" -> {
                                    startActivity(Intent(this, Recolector::class.java))
                                    finish()
                                }
                                else -> {
                                    Toast.makeText(
                                        this,
                                        "Tipo de usuario desconocido",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Usuario no encontrado en Firestore", Toast.LENGTH_LONG).show()
                        }

                        btnIngresar.isEnabled = true
                        btnIngresar.text = "Ingresar"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al obtener datos: ${e.message}", Toast.LENGTH_LONG).show()
                        btnIngresar.isEnabled = true
                        btnIngresar.text = "Ingresar"
                    }
            }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.message?.contains("no user record") == true -> "El correo no está registrado"
                    e.message?.contains("password") == true -> "Contraseña incorrecta"
                    e.message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
                    else -> "Error al iniciar sesión: ${e.message}"
                }

                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                btnIngresar.isEnabled = true
                btnIngresar.text = "Ingresar"
            }
    }
}