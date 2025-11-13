package com.tj.proyecto.Entidad

data class entVehiculo(
    val id: String = "",
    val placa: String = "",
    val tipoVehiculo: String = "",
    val capacidadKg: Double = 0.0,
    val estado: String = "Disponible",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val ultimoMantenimiento: Long? = null,
    val observaciones: String = "",
    val registradoPor: String = ""
)