package com.example.proyecto.Recolector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Camera
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.Adapter.FotosEvidenciaAdapter
import com.example.proyecto.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
//import java.util.jar.Manifest

class DetallesPuntoRecoleccion : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var puntoId: String
    private lateinit var asignacionId: String
    private var puntoOrden: Int = 0

    // Views
    private lateinit var txtNombre: TextView
    private lateinit var txtDireccion: TextView
    private lateinit var txtTipo: TextView
    private lateinit var txtZona: TextView
    private lateinit var txtFrecuencia: TextView
    private lateinit var txtHorario: TextView
    private lateinit var txtMateriales: TextView
    private lateinit var txtObservaciones: TextView
    private lateinit var cardCamera: CardView
    private lateinit var previewView: PreviewView
    private lateinit var btnTomarFoto: MaterialButton
    private lateinit var btnFinalizarPunto: MaterialButton
    private lateinit var rvFotosEvidencia: RecyclerView

    private var camera: androidx.camera.core.Camera? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File

    private val fotosEvidencia = mutableListOf<String>()
    private lateinit var fotosAdapter: FotosEvidenciaAdapter

    companion object {
        private const val TAG = "DetallesPunto"

        fun newInstance(puntoId: String, asignacionId: String, orden: Int): DetallesPuntoRecoleccion {
            val fragment = DetallesPuntoRecoleccion()
            val args = Bundle()
            args.putString("puntoId", puntoId)
            args.putString("asignacionId", asignacionId)
            args.putInt("orden", orden)
            fragment.arguments = args
            return fragment
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara necesario", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        puntoId = arguments?.getString("puntoId") ?: ""
        asignacionId = arguments?.getString("asignacionId") ?: ""
        puntoOrden = arguments?.getInt("orden") ?: 0

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detalles_punto_recoleccion, container, false)

        initViews(view)
        setupRecyclerView()
        cargarDatosPunto()
        checkCameraPermission()

        btnTomarFoto.setOnClickListener {
            tomarFoto()
        }

        btnFinalizarPunto.setOnClickListener {
            finalizarPunto()
        }

        return view
    }

    private fun initViews(view: View) {
        txtNombre = view.findViewById(R.id.txtNombre)
        txtDireccion = view.findViewById(R.id.txtDireccion)
        txtTipo = view.findViewById(R.id.txtTipo)
        txtZona = view.findViewById(R.id.txtZona)
        txtFrecuencia = view.findViewById(R.id.txtFrecuencia)
        txtHorario = view.findViewById(R.id.txtHorario)
        txtMateriales = view.findViewById(R.id.txtMateriales)
        txtObservaciones = view.findViewById(R.id.txtObservaciones)
        cardCamera = view.findViewById(R.id.cardCamera)
        previewView = view.findViewById(R.id.previewView)
        btnTomarFoto = view.findViewById(R.id.btnTomarFoto)
        btnFinalizarPunto = view.findViewById(R.id.btnFinalizarPunto)
        rvFotosEvidencia = view.findViewById(R.id.rvFotosEvidencia)
    }

    private fun setupRecyclerView() {
        fotosAdapter = FotosEvidenciaAdapter(fotosEvidencia) { url ->
            // Opción para eliminar foto si es necesario
        }
        rvFotosEvidencia.layoutManager = GridLayoutManager(requireContext(), 3)
        rvFotosEvidencia.adapter = fotosAdapter
    }

    private fun cargarDatosPunto() {
        db.collection("puntos_recoleccion")
            .document(puntoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    txtNombre.text = document.getString("nombre") ?: "Sin nombre"
                    txtDireccion.text = "📍 ${document.getString("direccion") ?: "Sin dirección"}"
                    txtTipo.text = "Tipo: ${document.getString("tipo") ?: "N/A"}"
                    txtZona.text = "Zona: ${document.getString("zona") ?: "N/A"}"
                    txtFrecuencia.text = "Frecuencia: ${document.getString("frecuencia") ?: "N/A"}"
                    txtHorario.text = "Horario: ${document.getString("horarioPreferido") ?: "N/A"}"

                    val materiales = document.get("tiposMaterialAceptado") as? List<String>
                    txtMateriales.text = "♻️ ${materiales?.joinToString(", ") ?: "No especificado"}"

                    val obs = document.getString("observaciones")
                    txtObservaciones.text = if (obs.isNullOrEmpty()) "Sin observaciones" else obs
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                   this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar cámara", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun tomarFoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    subirFotoAStorage(photoFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar foto: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Error al tomar foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun subirFotoAStorage(file: File) {
        val storageRef = storage.reference
        val fileName = "${System.currentTimeMillis()}.jpg"
        val fotoRef = storageRef.child("evidencias/$asignacionId/$puntoId/$fileName")

        val uri = Uri.fromFile(file)

        fotoRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                fotoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val urlFoto = downloadUri.toString()
                    fotosEvidencia.add(urlFoto)
                    fotosAdapter.notifyItemInserted(fotosEvidencia.size - 1)

                    Toast.makeText(requireContext(), "Foto guardada", Toast.LENGTH_SHORT).show()

                    // Eliminar archivo temporal
                    file.delete()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al subir foto", e)
                Toast.makeText(requireContext(), "Error al guardar foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun finalizarPunto() {
        if (fotosEvidencia.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Debes tomar al menos una foto de evidencia",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Log.d(TAG, "Guardando evidencias para punto: $puntoId")

        val evidenciaData = hashMapOf(
            "asignacionId" to asignacionId,
            "puntoId" to puntoId,
            "orden" to puntoOrden,
            "fotos" to fotosEvidencia,
            "fechaRecoleccion" to System.currentTimeMillis(),
            "recolectorId" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        )

        db.collection("evidencias_recoleccion")
            .add(evidenciaData)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Evidencias guardadas en BD")

                Toast.makeText(requireContext(), "✓ Punto completado", Toast.LENGTH_SHORT).show()

                // Simplemente volver atrás
                // RecolectorRuta recargará los estados desde la BD automáticamente en onResume
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar evidencias: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al finalizar punto", Toast.LENGTH_SHORT).show()
            }
    }

//    private fun finalizarPunto() {
//        if (fotosEvidencia.isEmpty()) {
//            Toast.makeText(
//                requireContext(),
//                "Debes tomar al menos una foto de evidencia",
//                Toast.LENGTH_LONG
//            ).show()
//            return
//        }
//
//        // Guardar evidencias en Firestore
//        val evidenciaData = hashMapOf(
//            "asignacionId" to asignacionId,
//            "puntoId" to puntoId,
//            "orden" to puntoOrden,
//            "fotos" to fotosEvidencia,
//            "fechaRecoleccion" to System.currentTimeMillis(),
//            "recolectorId" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
//        )
//
//        db.collection("evidencias_recoleccion")
//            .add(evidenciaData)
//            .addOnSuccessListener {
//                // PRIMERO: Enviar resultado al fragment padre
//                val result = Bundle().apply {
//                    putString("puntoId", puntoId)
//                }
//                parentFragmentManager.setFragmentResult("puntoCompletado", result)
//
//                // SEGUNDO: Mostrar mensaje
//                Toast.makeText(requireContext(), "✓ Punto completado", Toast.LENGTH_SHORT).show()
//
//                // TERCERO: Cerrar fragments (este y el escáner si existe)
//                parentFragmentManager.popBackStack()
//                parentFragmentManager.popBackStack()
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Error al guardar evidencias", e)
//                Toast.makeText(requireContext(), "Error al finalizar punto", Toast.LENGTH_SHORT).show()
//            }
//    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, "RecoleccionApp").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir
        else requireContext().filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}