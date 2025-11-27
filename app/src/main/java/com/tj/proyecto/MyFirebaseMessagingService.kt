package com.tj.proyecto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el token en Firestore cuando se genera uno nuevo
        guardarTokenEnFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM_TEST", "¡Mensaje recibido en el servicio! Datos: ${message.data}")

        // Manejar la notificación recibida
        message.notification?.let {
            Log.d("FCM_TEST", "Trae notificación: ${it.title}") // <--- OTRO LOG
            mostrarNotificacion(
                titulo = it.title ?: "Nueva Asignación",
                mensaje = it.body ?: "",
                data = message.data
            )
        }

        // Si solo hay datos (sin notification payload)
        if (message.data.isNotEmpty()) {
            val titulo = message.data["titulo"] ?: "Nueva Asignación de Ruta"
            val mensaje = message.data["mensaje"] ?: ""
            mostrarNotificacion(titulo, mensaje, message.data)
        }
    }

    private fun mostrarNotificacion(
        titulo: String,
        mensaje: String,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "asignaciones_rutas"

        // Crear canal de notificación para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Asignaciones de Rutas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevas asignaciones de rutas"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app al tocar la notificación
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Puedes pasar datos extras si quieres abrir una pantalla específica
            putExtra("tipo", "asignacion_ruta")
            data["asignacionId"]?.let { putExtra("asignacionId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cambia por tu icono android.R.drawable.ic_menu_info_details
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun guardarTokenEnFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    android.util.Log.d("FCM", "Token actualizado en Firestore")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FCM", "Error al actualizar token: ${e.message}")
                }
        }
    }
}