package com.example.proyecto

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
///**
// * A simple [Fragment] subclass.
// * Use the [EditarVehiculo.newInstance] factory method to
// * create an instance of this fragment.
// */
class EditarVehiculo : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }

    private lateinit var db: FirebaseFirestore
    private lateinit var vehiculo: entVehiculo

    private lateinit var txtPlaca: TextView
    private lateinit var spTipoVehiculo: Spinner
    private lateinit var etCapacidad: EditText
    private lateinit var spEstado: Spinner
    private lateinit var etObservaciones: EditText
    private lateinit var txtInfoRegistro: TextView
    private lateinit var btnGuardarCambios: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    companion object {
        private const val ARG_VEHICULO_ID = "vehiculo_id"
        private const val ARG_PLACA = "placa"
        private const val ARG_TIPO_VEHICULO = "tipo_vehiculo"
        private const val ARG_CAPACIDAD = "capacidad"
        private const val ARG_ESTADO = "estado"
        private const val ARG_OBSERVACIONES = "observaciones"
        private const val ARG_FECHA_REGISTRO = "fecha_registro"
        private const val ARG_ULTIMO_MANTENIMIENTO = "ultimo_mantenimiento"

        fun newInstance(vehiculo: entVehiculo): EditarVehiculo {
            val fragment = EditarVehiculo()
            val args = Bundle()
            args.putString(ARG_VEHICULO_ID, vehiculo.id)
            args.putString(ARG_PLACA, vehiculo.placa)
            args.putString(ARG_TIPO_VEHICULO, vehiculo.tipoVehiculo)
            args.putDouble(ARG_CAPACIDAD, vehiculo.capacidadKg)
            args.putString(ARG_ESTADO, vehiculo.estado)
            args.putString(ARG_OBSERVACIONES, vehiculo.observaciones)
            args.putLong(ARG_FECHA_REGISTRO, vehiculo.fechaRegistro)
            args.putLong(ARG_ULTIMO_MANTENIMIENTO, vehiculo.ultimoMantenimiento ?: 0)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        arguments?.let {
            vehiculo = entVehiculo(
                id = it.getString(ARG_VEHICULO_ID, ""),
                placa = it.getString(ARG_PLACA, ""),
                tipoVehiculo = it.getString(ARG_TIPO_VEHICULO, ""),
                capacidadKg = it.getDouble(ARG_CAPACIDAD, 0.0),
                estado = it.getString(ARG_ESTADO, "Disponible"),
                observaciones = it.getString(ARG_OBSERVACIONES, ""),
                fechaRegistro = it.getLong(ARG_FECHA_REGISTRO, System.currentTimeMillis()),
                ultimoMantenimiento = it.getLong(ARG_ULTIMO_MANTENIMIENTO)
                    .takeIf { value -> value > 0 }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_editar_vehiculo, container, false)

        initViews(view)
        setupSpinners()
        cargarDatosVehiculo()

        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtPlaca = view.findViewById(R.id.txtPlaca)
        spTipoVehiculo = view.findViewById(R.id.spTipoVehiculo)
        etCapacidad = view.findViewById(R.id.etCapacidad)
        spEstado = view.findViewById(R.id.spEstado)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        txtInfoRegistro = view.findViewById(R.id.txtInfoRegistro)
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupSpinners() {
        // Tipos de vehículo
        val tiposVehiculo = arrayOf("Triciclo", "Motocicleta", "Camión Pequeño", "Camión Grande")
        val adapterTipos =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposVehiculo)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoVehiculo.adapter = adapterTipos

        // Estados
        val estados = arrayOf("Disponible", "En Ruta", "Mantenimiento", "Inactivo")
        val adapterEstados = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados
    }

    private fun cargarDatosVehiculo() {
        // Placa (solo lectura)
        txtPlaca.text = vehiculo.placa

        // Tipo de vehículo
        val tipoPosition = when (vehiculo.tipoVehiculo) {
            "Triciclo" -> 0
            "Motocicleta" -> 1
            "Camión Pequeño" -> 2
            "Camión Grande" -> 3
            else -> 0
        }
        spTipoVehiculo.setSelection(tipoPosition)

        // Capacidad
        etCapacidad.setText(vehiculo.capacidadKg.toString())

        // Estado
        val estadoPosition = when (vehiculo.estado) {
            "Disponible" -> 0
            "En Ruta" -> 1
            "Mantenimiento" -> 2
            "Inactivo" -> 3
            else -> 0
        }
        spEstado.setSelection(estadoPosition)

        // Observaciones
        etObservaciones.setText(vehiculo.observaciones)

        // Fecha de registro
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        txtInfoRegistro.text = "Registrado el: ${sdf.format(vehiculo.fechaRegistro)}"
    }

    private fun guardarCambios() {
        if (!validarCampos()) {
            return
        }

        btnGuardarCambios.isEnabled = false
        btnGuardarCambios.text = "Guardando..."

        val tipoVehiculo = spTipoVehiculo.selectedItem.toString()
        val capacidad = etCapacidad.text.toString().toDoubleOrNull() ?: 0.0
        val estado = spEstado.selectedItem.toString()
        val observaciones = etObservaciones.text.toString().trim()

        val updates = hashMapOf<String, Any>(
            "tipoVehiculo" to tipoVehiculo,
            "capacidadKg" to capacidad,
            "estado" to estado,
            "observaciones" to observaciones
        )

        db.collection("vehiculos")
            .document(vehiculo.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Vehículo actualizado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar vehículo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardarCambios.isEnabled = true
                btnGuardarCambios.text = "Guardar Cambios"
            }
    }

    private fun validarCampos(): Boolean {
        val capacidad = etCapacidad.text.toString().trim()

        when {
            capacidad.isEmpty() -> {
                etCapacidad.error = "Ingrese la capacidad"
                etCapacidad.requestFocus()
                return false
            }
            capacidad.toDoubleOrNull() == null || capacidad.toDouble() <= 0 -> {
                etCapacidad.error = "Capacidad inválida"
                etCapacidad.requestFocus()
                return false
            }
        }

        return true
    }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment EditarVehiculo.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            EditarVehiculo().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}