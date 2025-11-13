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

class EditarRuta : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rutaId: String
    private var rutaActual: entRuta? = null

    private lateinit var etNombreRuta: EditText
    private lateinit var etCodigoRuta: EditText
    private lateinit var spZona: Spinner
    private lateinit var etDescripcion: EditText
    private lateinit var chipGroupDias: ChipGroup
    private lateinit var spEstado: Spinner
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private val diasSeleccionados = mutableListOf<String>()

    companion object {
        fun newInstance(rutaId: String): EditarRuta {
            val fragment = EditarRuta()
            val args = Bundle()
            args.putString("rutaId", rutaId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rutaId = arguments?.getString("rutaId") ?: ""
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_editar_ruta, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinners()
        setupDiasChips()
        cargarDatosRuta()

        btnGuardar.setOnClickListener {
            if (validarFormulario()) {
                guardarCambios()
            }
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        etNombreRuta = view.findViewById(R.id.etNombreRuta)
        etCodigoRuta = view.findViewById(R.id.etCodigoRuta)
        etCodigoRuta.isEnabled = false // El código no se puede editar
        spZona = view.findViewById(R.id.spZona)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        chipGroupDias = view.findViewById(R.id.chipGroupDias)
        spEstado = view.findViewById(R.id.spEstado)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupSpinners() {
        // Spinner de zonas
        val zonas = arrayOf("Centro", "Norte", "Sur", "Este", "Oeste")
        val adapterZonas = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapterZonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapterZonas

        // Spinner de estados
        val estados = arrayOf("Activa", "Inactiva", "En Revisión")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados
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

    private fun cargarDatosRuta() {
        db.collection("rutas")
            .document(rutaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    rutaActual = document.toObject(entRuta::class.java)
                    rutaActual?.let { ruta ->
                        etNombreRuta.setText(ruta.nombre)
                        etCodigoRuta.setText(ruta.codigo)
                        etDescripcion.setText(ruta.descripcion)

                        // Seleccionar zona
                        val zonas = arrayOf("Centro", "Norte", "Sur", "Este", "Oeste")
                        val zonaIndex = zonas.indexOf(ruta.zona)
                        if (zonaIndex >= 0) {
                            spZona.setSelection(zonaIndex)
                        }

                        // Seleccionar estado
                        val estados = arrayOf("Activa", "Inactiva", "En Revisión")
                        val estadoIndex = estados.indexOf(ruta.estado)
                        if (estadoIndex >= 0) {
                            spEstado.setSelection(estadoIndex)
                        }

                        // Marcar días seleccionados
                        diasSeleccionados.clear()
                        diasSeleccionados.addAll(ruta.diasSemana)

                        for (i in 0 until chipGroupDias.childCount) {
                            val chip = chipGroupDias.getChildAt(i) as Chip
                            chip.isChecked = ruta.diasSemana.contains(chip.text.toString())
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Ruta no encontrada", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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

    private fun guardarCambios() {
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        val rutaActualizada = hashMapOf(
            "nombre" to etNombreRuta.text.toString().trim(),
            "zona" to spZona.selectedItem.toString(),
            "descripcion" to etDescripcion.text.toString().trim(),
            "diasSemana" to diasSeleccionados.toList(),
            "estado" to spEstado.selectedItem.toString()
        )

        db.collection("rutas")
            .document(rutaId)
            .update(rutaActualizada as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Ruta actualizada exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar Cambios"
            }
    }

}