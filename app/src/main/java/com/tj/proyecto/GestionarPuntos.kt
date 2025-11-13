package com.tj.proyecto

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tj.proyecto.Adapter.PuntoRecoleccionAdapter
import com.tj.proyecto.Entidad.entPuntoRecoleccion
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class GestionarPuntos : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etBuscar: EditText
    private lateinit var spZona: Spinner
    private lateinit var spTipo: Spinner
    private lateinit var btnVerMapa: MaterialButton
    private lateinit var rvPuntos: RecyclerView
    private lateinit var llSinDatos: View

    private val puntosList = mutableListOf<entPuntoRecoleccion>()
    private val puntosFiltrados = mutableListOf<entPuntoRecoleccion>()
    private lateinit var adapter: PuntoRecoleccionAdapter
    private var listenerRegistration: ListenerRegistration? = null

    private var puntoParaDescargar: entPuntoRecoleccion? = null

    // Solicitar permisos de escritura
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            puntoParaDescargar?.let { descargarQR(it) }
        } else {
            Toast.makeText(
                requireContext(),
                "Se necesita permiso para guardar el código QR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gestionar_puntos, container, false)

        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupSpinners()
        setupRecyclerView()
        setupBusqueda()
        cargarPuntos()

        btnVerMapa.setOnClickListener {
            abrirMapaGeneral()
        }

        return view
    }

    private fun initViews(view: View) {
        etBuscar = view.findViewById(R.id.etBuscar)
        spZona = view.findViewById(R.id.spZona)
        spTipo = view.findViewById(R.id.spTipo)
        btnVerMapa = view.findViewById(R.id.btnVerMapa)
        rvPuntos = view.findViewById(R.id.rvPuntos)
        llSinDatos = view.findViewById(R.id.llSinDatos)
    }

    private fun setupSpinners() {
        // Zonas
        val zonas = arrayOf("Todas las zonas", "Centro", "Norte", "Sur", "Este", "Oeste")
        val adapterZonas = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, zonas)
        adapterZonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spZona.adapter = adapterZonas

        spZona.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarPuntos()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Tipos
        val tipos = arrayOf("Todos los tipos", "Domicilio", "Comercio", "Institución", "Contenedor Público")
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapterTipos

        spTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarPuntos()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = PuntoRecoleccionAdapter(
            puntosFiltrados,
            onEstadoChange = { punto, nuevoEstado ->
                cambiarEstadoPunto(punto, nuevoEstado)
            },
            onVerMapa = { punto ->
                abrirMapaPunto(punto)
            },
            onEditar = { punto ->
                abrirEdicion(punto)
            },
            onDescargarQR = { punto ->
                verificarPermisosYDescargar(punto)
            }
        )

        rvPuntos.layoutManager = LinearLayoutManager(requireContext())
        rvPuntos.adapter = adapter
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarPuntos()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarPuntos() {
        // Usar listener en tiempo real
        listenerRegistration = db.collection("puntos_recoleccion")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar puntos: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    puntosList.clear()
                    for (document in snapshot.documents) {
                        val punto = document.toObject(entPuntoRecoleccion::class.java)
                        if (punto != null) {
                            puntosList.add(punto)
                        }
                    }
                    filtrarPuntos()
                } else {
                    puntosList.clear()
                    filtrarPuntos()
                }
            }
    }

    private fun filtrarPuntos() {
        val busqueda = etBuscar.text.toString().lowercase()
        val zonaSeleccionada = spZona.selectedItem.toString()
        val tipoSeleccionado = spTipo.selectedItem.toString()

        puntosFiltrados.clear()

        for (punto in puntosList) {
            // Filtro de búsqueda
            val coincideBusqueda = busqueda.isEmpty() ||
                    punto.nombre.lowercase().contains(busqueda) ||
                    punto.direccion.lowercase().contains(busqueda)

            // Filtro de zona
            val coincideZona = zonaSeleccionada == "Todas las zonas" || punto.zona == zonaSeleccionada

            // Filtro de tipo
            val coincideTipo = tipoSeleccionado == "Todos los tipos" || punto.tipo == tipoSeleccionado

            if (coincideBusqueda && coincideZona && coincideTipo) {
                puntosFiltrados.add(punto)
            }
        }

        // Actualizar vista
        if (puntosFiltrados.isEmpty()) {
            rvPuntos.visibility = View.GONE
            llSinDatos.visibility = View.VISIBLE
        } else {
            rvPuntos.visibility = View.VISIBLE
            llSinDatos.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    private fun cambiarEstadoPunto(punto: entPuntoRecoleccion, nuevoEstado: Boolean) {
        db.collection("puntos_recoleccion")
            .document(punto.id)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                val mensaje = if (nuevoEstado) "Punto activado" else "Punto desactivado"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cambiar estado: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                filtrarPuntos()
            }
    }

    private fun verificarPermisosYDescargar(punto: entPuntoRecoleccion) {
        if (punto.codigoQR.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Este punto no tiene código QR",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        puntoParaDescargar = punto

        // En Android 10+ no se necesita permiso para MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            descargarQR(punto)
        } else {
            // Para versiones anteriores, verificar permiso
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                descargarQR(punto)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun descargarQR(punto: entPuntoRecoleccion) {
        Toast.makeText(requireContext(), "Descargando código QR...", Toast.LENGTH_SHORT).show()

        // Usar Picasso para descargar la imagen
        Picasso.get()
            .load(punto.codigoQR)
            .into(object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if (bitmap != null) {
                        guardarImagenEnGaleria(bitmap, punto.nombre)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error al cargar la imagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Toast.makeText(
                        requireContext(),
                        "Error al descargar QR: ${e?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    // No hacer nada
                }
            })
    }

    private fun guardarImagenEnGaleria(bitmap: Bitmap, nombrePunto: String) {
        val nombreArchivo = "QR_${nombrePunto.replace(" ", "_")}_${System.currentTimeMillis()}.png"

        val fos: OutputStream?
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CodigosQR")
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                // Android 9 y anteriores
                val imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ).toString() + "/CodigosQR"
                val file = File(imagesDir)
                if (!file.exists()) {
                    file.mkdir()
                }
                val image = File(imagesDir, nombreArchivo)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(
                    requireContext(),
                    "Código QR guardado en Galería/CodigosQR",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al guardar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun abrirMapaPunto(punto: entPuntoRecoleccion) {
        if (punto.ubicacion != null) {
            // Abrir fragment con mapa integrado
            val fragment = MapaPunto.newInstance(punto)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("mapa_punto")
                .commit()
        } else {
            Toast.makeText(
                requireContext(),
                "Este punto no tiene ubicación registrada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun abrirMapaGeneral() {
        // Crear una lista de todos los puntos activos para mostrar en el mapa
        val puntosActivos = puntosFiltrados.filter { it.estado && it.ubicacion != null }

        if (puntosActivos.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay puntos activos con ubicación",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Abrir fragment con mapa general
        val fragment = MapaGeneral()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("mapa_general")
            .commit()
    }

    private fun abrirEdicion(punto: entPuntoRecoleccion) {
        val fragment = EditarPunto.newInstance(punto)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}