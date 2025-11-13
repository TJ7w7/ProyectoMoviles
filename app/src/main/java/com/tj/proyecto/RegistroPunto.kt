package com.tj.proyecto

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
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage

class RegistroPunto : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    // Views
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
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    // Ubicación seleccionada
    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    // Launcher para seleccionar ubicación desde el mapa
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registro_punto, container, false)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Inicializar views
        initViews(view)

        // Configurar Spinners
        setupSpinners()

        // Configurar listeners
        btnSeleccionarUbicacion.setOnClickListener {
            abrirMapaParaSeleccionar()
        }

        btnRegistrar.setOnClickListener {
            registrarPunto()
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
        btnRegistrar = view.findViewById(R.id.btnRegistrar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupSpinners() {
        // Zonas
        val zonas = arrayOf(
            "Seleccione zona",
            "Centro",
            "Norte",
            "Sur",
            "Este",
            "Oeste"
        )
        val adapterZonas = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            zonas
        )
        adapterZonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapterZonas

        // Tipos
        val tipos = arrayOf(
            "Seleccione tipo",
            "Domicilio",
            "Comercio",
            "Institución",
            "Contenedor Público"
        )
        val adapterTipos = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tipos
        )
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapterTipos

        // Frecuencias
        val frecuencias = arrayOf(
            "Diaria",
            "Interdiaria",
            "Semanal",
            "Quincenal",
            "Mensual"
        )
        val adapterFrecuencias = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            frecuencias
        )
        adapterFrecuencias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFrecuencia.adapter = adapterFrecuencias
    }

    private fun abrirMapaParaSeleccionar() {
        // Abre tu Activity de Mapa para seleccionar ubicación
        val intent = Intent(requireContext(), SeleccionarUbicacionActivity::class.java)

        // Si ya hay una ubicación seleccionada, enviarla al mapa
        if (latitudSeleccionada != null && longitudSeleccionada != null) {
            intent.putExtra("latitud", latitudSeleccionada)
            intent.putExtra("longitud", longitudSeleccionada)
        }

        seleccionarUbicacionLauncher.launch(intent)
    }

    private fun registrarPunto() {
        // Validar campos
        if (!validarCampos()) {
            return
        }

        // Mostrar progress
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val nombre = etNombre.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val zona = spZona.selectedItem.toString()
        val tipo = spTipo.selectedItem.toString()
        val horario = etHorario.text.toString().trim()
        val frecuencia = spFrecuencia.selectedItem.toString()
        val observaciones = etObservaciones.text.toString().trim()

        // Obtener materiales seleccionados
        val materialesAceptados = mutableListOf<String>()
        if (cbPlastico.isChecked) materialesAceptados.add("Plástico")
        if (cbCarton.isChecked) materialesAceptados.add("Cartón")
        if (cbVidrio.isChecked) materialesAceptados.add("Vidrio")
        if (cbMetal.isChecked) materialesAceptados.add("Metal")
        if (cbPapel.isChecked) materialesAceptados.add("Papel")
        if (cbOrganico.isChecked) materialesAceptados.add("Orgánico")

        // Generar ID único
        val puntoId = db.collection("puntos_recoleccion").document().id

        // Generar código QR con el ID del punto
        val qrBitmap = QRCodeHelper.generateQRCode(puntoId, 512, 512)

        if (qrBitmap == null) {
            Toast.makeText(
                requireContext(),
                "Error al generar código QR",
                Toast.LENGTH_SHORT
            ).show()
            btnRegistrar.isEnabled = true
            btnRegistrar.text = "Registrar Punto de Recolección"
            return
        }

        // Subir QR a Firebase Storage
        val qrBytes = QRCodeHelper.bitmapToByteArray(qrBitmap)
        val qrRef = storage.reference.child("codigos_qr/$puntoId.png")

        qrRef.putBytes(qrBytes)
            .addOnSuccessListener {
                // Obtener URL de descarga del QR
                qrRef.downloadUrl.addOnSuccessListener { uri ->
                    guardarPuntoEnFirestore(
                        puntoId,
                        nombre,
                        direccion,
                        zona,
                        tipo,
                        horario,
                        frecuencia,
                        materialesAceptados,
                        observaciones,
                        uri.toString()
                    )
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Error al obtener URL del QR: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Registrar Punto de Recolección"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al subir código QR: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar Punto de Recolección"
            }

//        // Crear GeoPoint
//        val ubicacion = GeoPoint(latitudSeleccionada!!, longitudSeleccionada!!)
//
//        // Crear objeto PuntoRecoleccion
//        val punto = entPuntoRecoleccion(
//            id = puntoId,
//            nombre = nombre,
//            direccion = direccion,
//            ubicacion = ubicacion,
//            zona = zona,
//            tipo = tipo,
//            horarioPreferido = horario,
//            frecuencia = frecuencia,
//            tiposMaterialAceptado = materialesAceptados,
//            estado = true,
//            observaciones = observaciones,
//            fechaRegistro = System.currentTimeMillis(),
//            registradoPor = auth.currentUser?.uid ?: ""
//        )
//
//        // Guardar en Firestore
//        db.collection("puntos_recoleccion")
//            .document(puntoId)
//            .set(punto)
//            .addOnSuccessListener {
//                Toast.makeText(
//                    requireContext(),
//                    "Punto de recolección registrado exitosamente",
//                    Toast.LENGTH_SHORT
//                ).show()
//                limpiarCampos()
//                btnRegistrar.isEnabled = true
//                btnRegistrar.text = "Registrar Punto de Recolección"
//
//                // Opcional: volver atrás
//                requireActivity().supportFragmentManager.popBackStack()
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(
//                    requireContext(),
//                    "Error al guardar punto: ${e.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//                btnRegistrar.isEnabled = true
//                btnRegistrar.text = "Registrar Punto de Recolección"
//            }
    }

    private fun guardarPuntoEnFirestore(
        puntoId: String,
        nombre: String,
        direccion: String,
        zona: String,
        tipo: String,
        horario: String,
        frecuencia: String,
        materialesAceptados: List<String>,
        observaciones: String,
        codigoQRUrl: String
    ) {
        val ubicacion = GeoPoint(latitudSeleccionada!!, longitudSeleccionada!!)

        val punto = entPuntoRecoleccion(
            id = puntoId,
            nombre = nombre,
            direccion = direccion,
            ubicacion = ubicacion,
            zona = zona,
            tipo = tipo,
            horarioPreferido = horario,
            frecuencia = frecuencia,
            tiposMaterialAceptado = materialesAceptados,
            estado = true,
            observaciones = observaciones,
            fechaRegistro = System.currentTimeMillis(),
            registradoPor = auth.currentUser?.uid ?: "",
            codigoQR = codigoQRUrl
        )

        db.collection("puntos_recoleccion")
            .document(puntoId)
            .set(punto)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Punto registrado exitosamente con código QR",
                    Toast.LENGTH_SHORT
                ).show()
                limpiarCampos()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar Punto de Recolección"
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al guardar punto: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar Punto de Recolección"
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
            !cbPlastico.isChecked && !cbCarton.isChecked && !cbVidrio.isChecked && !cbMetal.isChecked && !cbPapel.isChecked && !cbOrganico.isChecked -> {
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

    private fun limpiarCampos() {
        etNombre.text.clear()
        etDireccion.text.clear()
        spZona.setSelection(0)
        spTipo.setSelection(0)
        etHorario.text.clear()
        spFrecuencia.setSelection(0)
        cbPlastico.isChecked = false
        cbCarton.isChecked = false
        cbVidrio.isChecked = false
        cbMetal.isChecked = false
        cbPapel.isChecked = false
        cbOrganico.isChecked = false
        etObservaciones.text.clear()
        latitudSeleccionada = null
        longitudSeleccionada = null
        txtCoordenadas.text = "Sin ubicación seleccionada"
        txtCoordenadas.setTextColor(resources.getColor(android.R.color.darker_gray, null))
    }
}