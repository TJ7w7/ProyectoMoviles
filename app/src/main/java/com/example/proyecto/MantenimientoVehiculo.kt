package com.example.proyecto

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MantenimientoVehiculo : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var vehiculo: entVehiculo

    private lateinit var txtPlaca: TextView
    private lateinit var txtTipoVehiculo: TextView
    private lateinit var etFechaMantenimiento: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etCosto: EditText
    private lateinit var etProximoMantenimiento: EditText
    private lateinit var btnRegistrarMantenimiento: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private var fechaMantenimientoTimestamp: Long = System.currentTimeMillis()
    private var fechaProximoMantenimientoTimestamp: Long? = null

    companion object {
        private const val ARG_VEHICULO = "vehiculo"

        fun newInstance(vehiculo: entVehiculo): MantenimientoVehiculo {
            val fragment = MantenimientoVehiculo()
            val args = Bundle()
            args.putString("vehiculo_id", vehiculo.id)
            args.putString("placa", vehiculo.placa)
            args.putString("tipo_vehiculo", vehiculo.tipoVehiculo)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        arguments?.let {
            vehiculo = entVehiculo(
                id = it.getString("vehiculo_id", ""),
                placa = it.getString("placa", ""),
                tipoVehiculo = it.getString("tipo_vehiculo", "")
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mantenimiento_vehiculo, container, false)

        initViews(view)
        setupDatePickers()
        cargarDatosVehiculo()

        btnRegistrarMantenimiento.setOnClickListener {
            registrarMantenimiento()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        txtPlaca = view.findViewById(R.id.txtPlaca)
        txtTipoVehiculo = view.findViewById(R.id.txtTipoVehiculo)
        etFechaMantenimiento = view.findViewById(R.id.etFechaMantenimiento)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        etCosto = view.findViewById(R.id.etCosto)
        etProximoMantenimiento = view.findViewById(R.id.etProximoMantenimiento)
        btnRegistrarMantenimiento = view.findViewById(R.id.btnRegistrarMantenimiento)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupDatePickers() {
        // DatePicker para fecha de mantenimiento
        etFechaMantenimiento.setOnClickListener {
            showDatePicker(etFechaMantenimiento) { timestamp ->
                fechaMantenimientoTimestamp = timestamp
            }
        }

        // DatePicker para próximo mantenimiento
        etProximoMantenimiento.setOnClickListener {
            showDatePicker(etProximoMantenimiento) { timestamp ->
                fechaProximoMantenimientoTimestamp = timestamp
            }
        }
    }

    private fun showDatePicker(editText: EditText, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = Calendar.getInstance()
                fecha.set(selectedYear, selectedMonth, selectedDay)
                val timestamp = fecha.timeInMillis

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                editText.setText(sdf.format(fecha.time))
                onDateSelected(timestamp)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun cargarDatosVehiculo() {
        txtPlaca.text = vehiculo.placa
        txtTipoVehiculo.text = vehiculo.tipoVehiculo

        // Establecer fecha actual por defecto
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFechaMantenimiento.setText(sdf.format(System.currentTimeMillis()))
    }

    private fun registrarMantenimiento() {
        if (!validarCampos()) {
            return
        }

        btnRegistrarMantenimiento.isEnabled = false
        btnRegistrarMantenimiento.text = "Registrando..."

        val descripcion = etDescripcion.text.toString().trim()
        val costo = etCosto.text.toString().toDoubleOrNull() ?: 0.0

        // Crear objeto de mantenimiento
        val mantenimiento = hashMapOf(
            "id" to db.collection("mantenimientos").document().id,
            "vehiculoId" to vehiculo.id,
            "vehiculoPlaca" to vehiculo.placa,
            "fechaMantenimiento" to fechaMantenimientoTimestamp,
            "descripcion" to descripcion,
            "costo" to costo,
            "fechaRegistro" to System.currentTimeMillis(),
            "registradoPor" to "admin" // TODO: Obtener usuario actual
        )

        // Agregar próximo mantenimiento si está especificado
        fechaProximoMantenimientoTimestamp?.let {
            mantenimiento["proximoMantenimiento"] = it
        }

        // Guardar en colección de mantenimientos
        db.collection("mantenimientos")
            .document(mantenimiento["id"] as String)
            .set(mantenimiento)
            .addOnSuccessListener {
                // Actualizar último mantenimiento del vehículo
                val updates = hashMapOf<String, Any>(
                    "ultimoMantenimiento" to fechaMantenimientoTimestamp
                )

                db.collection("vehiculos")
                    .document(vehiculo.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Mantenimiento registrado exitosamente",
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
                        resetButton()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al registrar mantenimiento: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                resetButton()
            }
    }

    private fun validarCampos(): Boolean {
        val descripcion = etDescripcion.text.toString().trim()
        val costo = etCosto.text.toString().trim()

        when {
            descripcion.isEmpty() -> {
                etDescripcion.error = "Ingrese la descripción del mantenimiento"
                etDescripcion.requestFocus()
                return false
            }
            costo.isEmpty() -> {
                etCosto.error = "Ingrese el costo"
                etCosto.requestFocus()
                return false
            }
            costo.toDoubleOrNull() == null || costo.toDouble() < 0 -> {
                etCosto.error = "Costo inválido"
                etCosto.requestFocus()
                return false
            }
        }

        return true
    }

    private fun resetButton() {
        btnRegistrarMantenimiento.isEnabled = true
        btnRegistrarMantenimiento.text = "Registrar Mantenimiento"
    }
}