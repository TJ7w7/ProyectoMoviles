package com.tj.proyecto.Recolector

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.tj.proyecto.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistroPesoMaterial : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var asignacionId: String
    private lateinit var rutaId: String

    private lateinit var txtFechaHora: TextView
    private lateinit var txtRutaInfo: TextView
    private lateinit var chipGroupMateriales: ChipGroup
    private lateinit var editPesoPlastico: EditText
    private lateinit var editPesoCarton: EditText
    private lateinit var editPesoVidrio: EditText
    private lateinit var editPesoPapel: EditText
    private lateinit var editObservaciones: EditText
    private lateinit var btnGuardarYFinalizar: MaterialButton
    private lateinit var btnCancelar: MaterialButton

    companion object {
        private const val TAG = "RegistrarPeso"

        fun newInstance(asignacionId: String, rutaId: String): RegistroPesoMaterial {
            val fragment = RegistroPesoMaterial()
            val args = Bundle()
            args.putString("asignacionId", asignacionId)
            args.putString("rutaId", rutaId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asignacionId = arguments?.getString("asignacionId") ?: ""
        rutaId = arguments?.getString("rutaId") ?: ""
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_registro_peso_material, container, false)

        initViews(view)
        cargarDatosRuta()
        mostrarFechaHoraActual()

        btnGuardarYFinalizar.setOnClickListener {
            guardarRegistroYFinalizar()
        }

        btnCancelar.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view

    }

    private fun initViews(view: View) {
        txtFechaHora = view.findViewById(R.id.txtFechaHora)
        txtRutaInfo = view.findViewById(R.id.txtRutaInfo)
        chipGroupMateriales = view.findViewById(R.id.chipGroupMateriales)
        editPesoPlastico = view.findViewById(R.id.editPesoPlastico)
        editPesoCarton = view.findViewById(R.id.editPesoCarton)
        editPesoVidrio = view.findViewById(R.id.editPesoVidrio)
        editPesoPapel = view.findViewById(R.id.editPesoPapel)
        editObservaciones = view.findViewById(R.id.editObservaciones)
        btnGuardarYFinalizar = view.findViewById(R.id.btnGuardarYFinalizar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
    }

    private fun mostrarFechaHoraActual() {
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault())
        val fechaHora = sdf.format(Date())
        txtFechaHora.text = "📅 $fechaHora"
    }

    private fun cargarDatosRuta() {
        db.collection("rutas")
            .document(rutaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombreRuta = document.getString("nombre") ?: "Ruta sin nombre"
                    txtRutaInfo.text = "📍 Ruta: $nombreRuta"
                }
            }
    }

    private fun guardarRegistroYFinalizar() {
        // Obtener pesos
        val pesoPlastico = editPesoPlastico.text.toString().toDoubleOrNull() ?: 0.0
        val pesoCarton = editPesoCarton.text.toString().toDoubleOrNull() ?: 0.0
        val pesoVidrio = editPesoVidrio.text.toString().toDoubleOrNull() ?: 0.0
        val pesoPapel = editPesoPapel.text.toString().toDoubleOrNull() ?: 0.0

        val pesoTotal = pesoPlastico + pesoCarton + pesoVidrio + pesoPapel

        // Validar que al menos haya un peso registrado
        if (pesoTotal <= 0) {
            Toast.makeText(
                requireContext(),
                "Debes registrar al menos el peso de un material",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Crear mapa de materiales recolectados
        val materialesRecolectados = hashMapOf(
            "plastico" to pesoPlastico,
            "carton" to pesoCarton,
            "vidrio" to pesoVidrio,
            "papel" to pesoPapel,
        )

        // Crear registro
        val registroData = hashMapOf(
            "asignacionId" to asignacionId,
            "rutaId" to rutaId,
            "recolectorId" to FirebaseAuth.getInstance().currentUser?.uid,
            "fechaHoraRegistro" to System.currentTimeMillis(),
            "materialesRecolectados" to materialesRecolectados,
            "pesoTotal" to pesoTotal,
            "observaciones" to editObservaciones.text.toString(),
            "unidadMedida" to "kg"
        )

        Log.d(TAG, "Guardando registro de materiales...")

        // Guardar en Firestore
        db.collection("registros_materiales")
            .add(registroData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Registro guardado con ID: ${documentReference.id}")

                // Actualizar la asignación a "Completada"
                db.collection("asignaciones_rutas")
                    .document(asignacionId)
                    .update(
                        "estado", "Completada",
                        "fechaFinalizacion", System.currentTimeMillis(),
                        "registroMaterialId", documentReference.id
                    )
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "✓ Ruta completada y materiales registrados",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Volver al fragment anterior (RecolectorRuta)
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al actualizar asignación: ${e.message}")
                        Toast.makeText(
                            requireContext(),
                            "Error al finalizar ruta",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar registro: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error al guardar registro de materiales",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}