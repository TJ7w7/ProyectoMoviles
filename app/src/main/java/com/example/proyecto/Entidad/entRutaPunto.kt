package com.example.proyecto.Entidad

data class entRutaPunto(
    val id: String = "",
    val rutaId: String = "",
    val puntoId: String = "",
    val orden: Int = 0,
    val fechaAsignacion: Long = System.currentTimeMillis(),
)