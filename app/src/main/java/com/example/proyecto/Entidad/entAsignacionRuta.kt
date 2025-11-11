package com.example.proyecto.Entidad

data class entAsignacionRuta(
    val id: String = "",
    val rutaId: String = "",
    val rutaCodigo: String = "",
    val rutaNombre: String = "",
    val vehiculoId: String = "",
    val vehiculoPlaca: String = "",
    val conductorId: String = "",
    val conductorNombre: String = "",
    val ayudantesIds: List<String> = listOf(),
    val ayudantesNombres: List<String> = listOf(),
    val fechaAsignacion: Long = System.currentTimeMillis(),
    val fechaEjecucion: Long? = null,
    val estado: String = "Programada", // Programada, En Progreso, Completada, Cancelada
    val observaciones: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val registradoPor: String = ""
)