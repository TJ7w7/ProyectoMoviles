package com.tj.proyecto.Entidad

data class entUsuario(
    val id: String = "",
    val tipoUsuario: String = "", // "Administrador" o "Recolector"
    val nombres: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val correo: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val estado: Boolean = true,
    val registradoPor: String = ""
)