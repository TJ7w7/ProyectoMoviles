package com.tj.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.tj.proyecto.Entidad.entRuta
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RegistrarRuta : Fragment() {
    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var etNombreRuta: EditText
    private lateinit var etCodigoRuta: EditText
    private lateinit var spZona: Spinner
    private lateinit var etDescripcion: EditText
    private lateinit var chipGroupDias: ChipGroup
    private lateinit var btnContinuar: MaterialButton

    private val diasSeleccionados = mutableListOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registrar_ruta, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinner()
        setupDiasChips()
//        setupValidacion()
        generarCodigoUnico()

        btnContinuar.setOnClickListener {
            if (validarFormulario()) {
                guardarRuta()
            }
        }

        return view
    }

    private fun initViews(view: View) {
        etNombreRuta = view.findViewById(R.id.etNombreRuta)
        etCodigoRuta = view.findViewById(R.id.etCodigoRuta)
        etCodigoRuta.isEnabled = false
        spZona = view.findViewById(R.id.spZona)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        chipGroupDias = view.findViewById(R.id.chipGroupDias)
        btnContinuar = view.findViewById(R.id.btnContinuar)
    }

    private fun setupSpinner() {
        val zonas = arrayOf("Centro", "Norte", "Sur", "Este", "Oeste")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapter
    }

    private fun setupDiasChips() {
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        for (dia in dias) {
            val chip = Chip(requireContext())
            chip.text = dia
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    diasSeleccionados.add(dia)
                } else {
                    diasSeleccionados.remove(dia)
                }
            }
            chipGroupDias.addView(chip)
        }
    }

    private fun generarCodigoUnico() {
        // Consultar la última ruta para generar el siguiente código
        db.collection("rutas")
            .orderBy("codigo", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val siguienteCodigo = if (documents.isEmpty) {
                    "RUTA001"
                } else {
                    val ultimoCodigo = documents.documents[0].getString("codigo") ?: "RUTA000"
                    generarSiguienteCodigo(ultimoCodigo)
                }
                etCodigoRuta.setText(siguienteCodigo)
            }
            .addOnFailureListener {
                // Fallback: código basado en timestamp
                val codigoFallback = "RUT${System.currentTimeMillis().toString().takeLast(3)}"
                etCodigoRuta.setText(codigoFallback)
            }
    }

    private fun generarSiguienteCodigo(ultimoCodigo: String): String {
        return try {
            val regex = """(\D+)(\d+)""".toRegex()
            val matchResult = regex.find(ultimoCodigo)

            if (matchResult != null) {
                val (prefijo, numero) = matchResult.destructured
                val siguienteNumero = numero.toInt() + 1
                "$prefijo${siguienteNumero.toString().padStart(3, '0')}"
            } else {
                "RUTA${(System.currentTimeMillis() % 1000).toString().padStart(3, '0')}"
            }
        } catch (e: Exception) {
            "RUTA${(System.currentTimeMillis() % 1000).toString().padStart(3, '0')}"
        }
    }

    private fun validarFormulario(): Boolean {
        when {
            etNombreRuta.text.isEmpty() -> {
                etNombreRuta.error = "Ingrese el nombre de la ruta"
                etNombreRuta.requestFocus()
                return false
            }
            diasSeleccionados.isEmpty() -> {
                Toast.makeText(requireContext(), "Seleccione al menos un día", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun guardarRuta() {
        btnContinuar.isEnabled = false
        btnContinuar.text = "Guardando..."

        // Generar ID para la ruta
        val rutaId = db.collection("rutas").document().id

        // Crear objeto ruta
        val ruta = entRuta(
            id = rutaId,
            nombre = etNombreRuta.text.toString().trim(),
            codigo = etCodigoRuta.text.toString().trim().uppercase(),
            zona = spZona.selectedItem.toString(),
            descripcion = etDescripcion.text.toString().trim(),
            diasSemana = diasSeleccionados.toList(),
            estado = "Activa",
            fechaCreacion = System.currentTimeMillis(),
        )

        // Guardar en Firestore
        db.collection("rutas")
            .document(rutaId)
            .set(ruta)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Ruta creada exitosamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Navegar al fragmento de asignar puntos
                val fragment = AsignarPuntosRuta.newInstance(rutaId, ruta.nombre)
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    .replace(R.id.fragment_container, fragment)
//                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al crear la ruta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnContinuar.isEnabled = true
                btnContinuar.text = "Continuar a Asignar Puntos"
            }
    }
}