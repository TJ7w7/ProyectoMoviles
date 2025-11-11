package com.example.proyecto

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarPunto : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var punto: entPuntoRecoleccion

    private lateinit var etNombre: EditText
    private lateinit var etDireccion: EditText
    private lateinit var spZona: Spinner
    private lateinit var spTipo: Spinner
    private lateinit var btnSeleccionarUbicacion: MaterialButton
    private lateinit var txtCoordenadas: TextView
    private lateinit var etHorario: EditText
    private lateinit var spFrecuencia: Spinner
    private lateinit var cbPlastico: CheckBox
    private lateinit var cbCarton: CheckBox
    private lateinit var cbVidrio: CheckBox
    private lateinit var cbMetal: CheckBox
    private lateinit var cbPapel: CheckBox
    private lateinit var cbOrganico: CheckBox
    private lateinit var etObservaciones: EditText
    private lateinit var switchEstado: SwitchCompat
    private lateinit var txtInfoRegistro: TextView
    private lateinit var btnGuardarCambios: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    private val seleccionarUbicacionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            latitudSeleccionada = data?.getDoubleExtra("latitud", 0.0)
            longitudSeleccionada = data?.getDoubleExtra("longitud", 0.0)

            if (latitudSeleccionada != null && longitudSeleccionada != null) {
                txtCoordenadas.text = "Lat: ${"%.6f".format(latitudSeleccionada)}, Lng: ${"%.6f".format(longitudSeleccionada)}"
                txtCoordenadas.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    companion object {
        fun newInstance(punto: entPuntoRecoleccion): EditarPunto {
            val fragment = EditarPunto()
            val args = Bundle()
            // Serializar el punto como argumentos individuales
            args.putString("id", punto.id)
            args.putString("nombre", punto.nombre)
            args.putString("direccion", punto.direccion)
            args.putDouble("latitud", punto.ubicacion?.latitude ?: 0.0)
            args.putDouble("longitud", punto.ubicacion?.longitude ?: 0.0)
            args.putString("zona", punto.zona)
            args.putString("tipo", punto.tipo)
            args.putString("horarioPreferido", punto.horarioPreferido)
            args.putString("frecuencia", punto.frecuencia)
            args.putStringArrayList("materiales", ArrayList(punto.tiposMaterialAceptado))
            args.putString("observaciones", punto.observaciones)
            args.putBoolean("estado", punto.estado)
            args.putLong("fechaRegistro", punto.fechaRegistro)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // Reconstruir punto desde argumentos
        arguments?.let { args ->
            val lat = args.getDouble("latitud", 0.0)
            val lng = args.getDouble("longitud", 0.0)
            val ubicacion = if (lat != 0.0 && lng != 0.0) GeoPoint(lat, lng) else null

            punto = entPuntoRecoleccion(
                id = args.getString("id", ""),
                nombre = args.getString("nombre", ""),
                direccion = args.getString("direccion", ""),
                ubicacion = ubicacion,
                zona = args.getString("zona", ""),
                tipo = args.getString("tipo", ""),
                horarioPreferido = args.getString("horarioPreferido", ""),
                frecuencia = args.getString("frecuencia", ""),
                tiposMaterialAceptado = args.getStringArrayList("materiales") ?: listOf(),
                observaciones = args.getString("observaciones", ""),
                estado = args.getBoolean("estado", true),
                fechaRegistro = args.getLong("fechaRegistro", System.currentTimeMillis())
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_editar_punto, container, false)

        initViews(view)
        setupSpinners()
        cargarDatosPunto()

        btnSeleccionarUbicacion.setOnClickListener {
            abrirMapaParaSeleccionar()
        }

        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        etNombre = view.findViewById(R.id.etNombre)
        etDireccion = view.findViewById(R.id.etDireccion)
        spZona = view.findViewById(R.id.spZona)
        spTipo = view.findViewById(R.id.spTipo)
        btnSeleccionarUbicacion = view.findViewById(R.id.btnSeleccionarUbicacion)
        txtCoordenadas = view.findViewById(R.id.txtCoordenadas)
        etHorario = view.findViewById(R.id.etHorario)
        spFrecuencia = view.findViewById(R.id.spFrecuencia)
        cbPlastico = view.findViewById(R.id.cbPlastico)
        cbCarton = view.findViewById(R.id.cbCarton)
        cbVidrio = view.findViewById(R.id.cbVidrio)
        cbMetal = view.findViewById(R.id.cbMetal)
        cbPapel = view.findViewById(R.id.cbPapel)
        cbOrganico = view.findViewById(R.id.cbOrganico)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        switchEstado = view.findViewById(R.id.switchEstado)
        txtInfoRegistro = view.findViewById(R.id.txtInfoRegistro)
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupSpinners() {
        // Zonas
        val zonas = arrayOf("Seleccione zona", "Centro", "Norte", "Sur", "Este", "Oeste")
        val adapterZonas = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapterZonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapterZonas

        // Tipos
        val tipos = arrayOf("Seleccione tipo", "Domicilio", "Comercio", "Institución", "Contenedor Público")
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapterTipos

        // Frecuencias
        val frecuencias = arrayOf("Diaria", "Interdiaria", "Semanal", "Quincenal", "Mensual")
        val adapterFrecuencias = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, frecuencias)
        adapterFrecuencias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFrecuencia.adapter = adapterFrecuencias
    }

    private fun cargarDatosPunto() {
        // Datos básicos
        etNombre.setText(punto.nombre)
        etDireccion.setText(punto.direccion)

        // Zona
        val zonas = arrayOf("Seleccione zona", "Centro", "Norte", "Sur", "Este", "Oeste")
        val zonaIndex = zonas.indexOf(punto.zona)
        if (zonaIndex >= 0) spZona.setSelection(zonaIndex)

        // Tipo
        val tipos = arrayOf("Seleccione tipo", "Domicilio", "Comercio", "Institución", "Contenedor Público")
        val tipoIndex = tipos.indexOf(punto.tipo)
        if (tipoIndex >= 0) spTipo.setSelection(tipoIndex)

        // Ubicación
        val ubicacionActual = punto.ubicacion
        if (ubicacionActual != null) {
            latitudSeleccionada = ubicacionActual.latitude
            longitudSeleccionada = ubicacionActual.longitude
            txtCoordenadas.text = "Lat: ${"%.6f".format(latitudSeleccionada)}, Lng: ${"%.6f".format(longitudSeleccionada)}"
            txtCoordenadas.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            txtCoordenadas.text = "Sin ubicación seleccionada"
            txtCoordenadas.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        // Horario y frecuencia
        etHorario.setText(punto.horarioPreferido)
        val frecuencias = arrayOf("Diaria", "Interdiaria", "Semanal", "Quincenal", "Mensual")
        val frecuenciaIndex = frecuencias.indexOf(punto.frecuencia)
        if (frecuenciaIndex >= 0) spFrecuencia.setSelection(frecuenciaIndex)

        // Materiales
        cbPlastico.isChecked = punto.tiposMaterialAceptado.contains("Plástico")
        cbCarton.isChecked = punto.tiposMaterialAceptado.contains("Cartón")
        cbVidrio.isChecked = punto.tiposMaterialAceptado.contains("Vidrio")
        cbMetal.isChecked = punto.tiposMaterialAceptado.contains("Metal")
        cbPapel.isChecked = punto.tiposMaterialAceptado.contains("Papel")
        cbOrganico.isChecked = punto.tiposMaterialAceptado.contains("Orgánico")

        // Observaciones y estado
        etObservaciones.setText(punto.observaciones)
        switchEstado.isChecked = punto.estado

        // Fecha de registro
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        txtInfoRegistro.text = "Registrado el: ${sdf.format(Date(punto.fechaRegistro))}"
    }

    private fun abrirMapaParaSeleccionar() {
        val intent = Intent(requireContext(), SeleccionarUbicacionActivity::class.java)

        if (latitudSeleccionada != null && longitudSeleccionada != null) {
            intent.putExtra("latitud", latitudSeleccionada)
            intent.putExtra("longitud", longitudSeleccionada)
        }

        seleccionarUbicacionLauncher.launch(intent)
    }

    private fun guardarCambios() {
        if (!validarCampos()) {
            return
        }

        btnGuardarCambios.isEnabled = false
        btnGuardarCambios.text = "Guardando..."

        val nombre = etNombre.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val zona = spZona.selectedItem.toString()
        val tipo = spTipo.selectedItem.toString()
        val horario = etHorario.text.toString().trim()
        val frecuencia = spFrecuencia.selectedItem.toString()
        val observaciones = etObservaciones.text.toString().trim()
        val estado = switchEstado.isChecked

        // Materiales
        val materialesAceptados = mutableListOf<String>()
        if (cbPlastico.isChecked) materialesAceptados.add("Plástico")
        if (cbCarton.isChecked) materialesAceptados.add("Cartón")
        if (cbVidrio.isChecked) materialesAceptados.add("Vidrio")
        if (cbMetal.isChecked) materialesAceptados.add("Metal")
        if (cbPapel.isChecked) materialesAceptados.add("Papel")
        if (cbOrganico.isChecked) materialesAceptados.add("Orgánico")

        // Ubicación
        val ubicacion = GeoPoint(latitudSeleccionada!!, longitudSeleccionada!!)

        // Preparar actualización
        val updates = hashMapOf<String, Any>(
            "nombre" to nombre,
            "direccion" to direccion,
            "zona" to zona,
            "tipo" to tipo,
            "ubicacion" to ubicacion,
            "horarioPreferido" to horario,
            "frecuencia" to frecuencia,
            "tiposMaterialAceptado" to materialesAceptados,
            "observaciones" to observaciones,
            "estado" to estado
        )

        // Actualizar en Firestore
        db.collection("puntos_recoleccion")
            .document(punto.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Punto actualizado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()

                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar punto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardarCambios.isEnabled = true
                btnGuardarCambios.text = "Guardar Cambios"
            }
    }

    private fun validarCampos(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()

        when {
            nombre.isEmpty() -> {
                etNombre.error = "Ingrese el nombre del punto"
                etNombre.requestFocus()
                return false
            }
            direccion.isEmpty() -> {
                etDireccion.error = "Ingrese la dirección"
                etDireccion.requestFocus()
                return false
            }
            spZona.selectedItemPosition == 0 -> {
                Toast.makeText(requireContext(), "Seleccione la zona", Toast.LENGTH_SHORT).show()
                return false
            }
            spTipo.selectedItemPosition == 0 -> {
                Toast.makeText(requireContext(), "Seleccione el tipo de punto", Toast.LENGTH_SHORT).show()
                return false
            }
            latitudSeleccionada == null || longitudSeleccionada == null -> {
                Toast.makeText(
                    requireContext(),
                    "Debe seleccionar la ubicación en el mapa",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
            !cbPlastico.isChecked && !cbCarton.isChecked && !cbVidrio.isChecked &&
                    !cbMetal.isChecked && !cbPapel.isChecked && !cbOrganico.isChecked -> {
                Toast.makeText(
                    requireContext(),
                    "Seleccione al menos un tipo de material",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }

        return true
    }
}