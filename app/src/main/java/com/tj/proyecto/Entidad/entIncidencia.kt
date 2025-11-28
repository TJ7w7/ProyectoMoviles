package com.tj.proyecto.Entidad

data class entIncidencia(
    var id: String = "", // ID del documento
    val asignacionId: String = "",
    val puntoId: String = "",
    val puntoNombre: String = "",
    val orden: Int = 0,
    val motivo: String = "", // "QR Dañado", "Bloqueo", etc.
    val estado: String = "", // "Pendiente", "Resuelta"
    val fechaReporte: Long = 0,
    val recolectorId: String = "",
    val fotoUrl: String = "" // Si la hay
)
