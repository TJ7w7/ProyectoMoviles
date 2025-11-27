package com.tj.proyecto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


object PermissionHelper {
    fun Fragment.solicitarPermisoNotificaciones(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onResult(true)
                }
                else -> {
                    val requestPermission = registerForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        onResult(isGranted)
                    }
                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            onResult(true)
        }
    }
}