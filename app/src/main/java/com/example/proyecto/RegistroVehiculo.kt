package com.example.proyecto

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.proyecto.Entidad.entVehiculo
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class RegistroVehiculo : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var etPlaca: EditText
    private lateinit var spTipoVehiculo: Spinner
    private lateinit var etCapacidad: EditText
    private lateinit var spEstado: Spinner
    private lateinit var etFechaMantenimiento: EditText
    private lateinit var etObservaciones: EditText
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    private var fechaMantenimientoTimestamp: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registro_vehiculo, container, false)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializar views
        initViews(view)

        // Configurar Spinners
        setupSpinners()

        // Configurar DatePicker
        setupDatePicker()

        // Configurar listeners
        btnRegistrar.setOnClickListener {
            registrarVehiculo()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun initViews(view: View) {
        etPlaca = view.findViewById(R.id.etPlaca)
        spTipoVehiculo = view.findViewById(R.id.spTipoVehiculo)
        etCapacidad = view.findViewById(R.id.etCapacidad)
        spEstado = view.findViewById(R.id.spEstado)
        etFechaMantenimiento = view.findViewById(R.id.etFechaMantenimiento)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        btnRegistrar = view.findViewById(R.id.btnRegistrar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun setupSpinners() {
        // Tipos de vehículo
        val tiposVehiculo = arrayOf(
            "Seleccione tipo",
            "Triciclo",
            "Motocicleta",
            "Camión Pequeño",
            "Camión Grande"
        )
        val adapterTipos = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tiposVehiculo
        )
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoVehiculo.adapter = adapterTipos

        // Estados
        val estados = arrayOf(
            "Disponible",
            "En Ruta",
            "Mantenimiento",
            "Inactivo"
        )
        val adapterEstados = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            estados
        )
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstado.adapter = adapterEstados
    }

    private fun setupDatePicker() {
        etFechaMantenimiento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val fecha = Calendar.getInstance()
                    fecha.set(selectedYear, selectedMonth, selectedDay)
                    fechaMantenimientoTimestamp = fecha.timeInMillis

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    etFechaMantenimiento.setText(sdf.format(fecha.time))
                },
                year, month, day
            )
            datePickerDialog.show()
        }
    }

    private fun registrarVehiculo() {
        // Validar campos
        if (!validarCampos()) {
            return
        }

        // Mostrar progress
        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val placa = etPlaca.text.toString().trim().uppercase()
        val tipoVehiculo = spTipoVehiculo.selectedItem.toString()
        val capacidad = etCapacidad.text.toString().toDoubleOrNull() ?: 0.0
        val estado = spEstado.selectedItem.toString()
        val observaciones = etObservaciones.text.toString().trim()

        // Verificar si la placa ya existe
        db.collection("vehiculos")
            .whereEqualTo("placa", placa)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(
                        requireContext(),
                        "Ya existe un vehículo con esa placa",
                        Toast.LENGTH_LONG
                    ).show()
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Registrar Vehículo"
                    return@addOnSuccessListener
                }

                // Generar ID único
                val vehiculoId = db.collection("vehiculos").document().id

                // Crear objeto Vehiculo
                val vehiculo = entVehiculo(
                    id = vehiculoId,
                    placa = placa,
                    tipoVehiculo = tipoVehiculo,
                    capacidadKg = capacidad,
                    estado = estado,
                    fechaRegistro = System.currentTimeMillis(),
                    ultimoMantenimiento = fechaMantenimientoTimestamp,
                    observaciones = observaciones,
                    registradoPor = auth.currentUser?.uid ?: ""
                )

                // Guardar en Firestore
                db.collection("vehiculos")
                    .document(vehiculoId)
                    .set(vehiculo)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Vehículo registrado exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        limpiarCampos()
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrar Vehículo"

                        // Opcional: volver atrás
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error al guardar vehículo: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrar Vehículo"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al verificar placa: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Registrar Vehículo"
            }
    }

    private fun validarCampos(): Boolean {
        val placa = etPlaca.text.toString().trim()
        val capacidad = etCapacidad.text.toString().trim()

        when {
            placa.isEmpty() -> {
                etPlaca.error = "Ingrese la placa"
                etPlaca.requestFocus()
                return false
            }
            placa.length < 6 -> {
                etPlaca.error = "Placa inválida (mínimo 6 caracteres)"
                etPlaca.requestFocus()
                return false
            }
            spTipoVehiculo.selectedItemPosition == 0 -> {
                Toast.makeText(requireContext(), "Seleccione el tipo de vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
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

    private fun limpiarCampos() {
        etPlaca.text.clear()
        spTipoVehiculo.setSelection(0)
        etCapacidad.text.clear()
        spEstado.setSelection(0)
        etFechaMantenimiento.text.clear()
        etObservaciones.text.clear()
        fechaMantenimientoTimestamp = null
    }
}