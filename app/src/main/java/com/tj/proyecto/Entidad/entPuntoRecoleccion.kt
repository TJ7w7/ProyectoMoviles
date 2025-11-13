package com.tj.proyecto.Entidad

import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

data class entPuntoRecoleccion(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val ubicacion: GeoPoint? = null,
    val zona: String = "",
    val tipo: String = "",
    val horarioPreferido: String = "",
    val frecuencia: String = "",
    val tiposMaterialAceptado: List<String> = listOf(),
    val estado: Boolean = true,
    val observaciones: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val registradoPor: String = "",
    val codigoQR: String = ""
): Serializable