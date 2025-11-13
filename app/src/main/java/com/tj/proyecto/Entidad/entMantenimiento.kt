package com.tj.proyecto.Entidad

data class entMantenimiento(
    val id: String = "",
    val vehiculoId: String = "",
    val vehiculoPlaca: String = "",
    val fechaMantenimiento: Long = System.currentTimeMillis(),
    val descripcion: String = "",
    val costo: Double = 0.0,
    val proximoMantenimiento: Long? = null,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val registradoPor: String = ""
)