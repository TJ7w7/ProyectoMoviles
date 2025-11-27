package com.tj.proyecto

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {
    fun actualizarTokenEnFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // 1. Obtener el token actual del dispositivo
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Error al obtener token FCM", task.exception)
                    return@addOnCompleteListener
                }

                // 2. Token recuperado
                val token = task.result
                Log.d("FCM", "Token actual del dispositivo: $token")

                // 3. Guardar en Firestore bajo el ID del usuario
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("usuarios").document(userId)

                userRef.update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "¡Token vinculado al usuario exitosamente!")
                    }
                    .addOnFailureListener { e ->
                        // Si falla el update, puede ser que el documento no exista, intentamos set con merge
                        // O simplemente lo logueamos
                        Log.e("FCM", "Error al guardar token: ${e.message}")
                    }
            }
        } else {
            Log.d("FCM", "No se actualizó el token porque no hay usuario logueado.")
        }
    }
}