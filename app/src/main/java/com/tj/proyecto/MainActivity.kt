package com.tj.proyecto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.tj.proyecto.Recolector.Recolector

class MainActivity : AppCompatActivity() {

    // 1. Definimos el "Launcher" (El objeto que lanza la pregunta y escucha la respuesta)
    // Es importante declararlo AQUÍ, como variable de la clase, no dentro de un método.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // El usuario aceptó
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        } else {
            // El usuario rechazó
            Toast.makeText(this, "Sin notificaciones no podrás ver las rutas asignadas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 2. Llamamos a la función de verificar permisos al iniciar
        verificarPermisosNotificaciones()

        // 2. SEMÁFORO: ¿El usuario ya tiene sesión iniciada?
        val usuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (usuarioActual != null) {
            // SI YA ESTÁ LOGUEADO:
            // Saltamos directo al Menú Principal.
            // Como tu menú ya carga la ruta automáticamente, no enviamos ningún dato extra.
            val intent = Intent(this, Recolector::class.java)
            startActivity(intent)
            finish() // Cerramos el Login para que no pueda volver atrás
            return
        }

        val btnIniciarSecion = findViewById<MaterialButton>(R.id.btnIniciarSesion)

        btnIniciarSecion.setOnClickListener {

            val intent = Intent(this, IniciarSesion::class.java)
            startActivity(intent)
        }
    }

    private fun verificarPermisosNotificaciones() {
        // Solo es necesario pedir permiso explícito en Android 13 (Tiramisu) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Caso A: Ya tiene permiso, no hacemos nada
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso, todo ok
                }

                // Caso B: No tiene permiso, hay que pedirlo
                else -> {
                    // Lanza la ventanita del sistema
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}