package com.example.proyecto.Entidad

data class entRuta(
    val id: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val zona: String = "",
    val descripcion: String = "",
    val diasSemana: List<String> = listOf(),
    val estado: String = "Activa",
    val fechaCreacion: Long = System.currentTimeMillis(),
    var cantidadPuntos: Int? = null
)