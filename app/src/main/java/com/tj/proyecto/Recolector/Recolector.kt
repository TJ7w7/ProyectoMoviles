package com.tj.proyecto.Recolector

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.tj.proyecto.HomeDashboard
import com.tj.proyecto.MainActivity
import com.tj.proyecto.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Recolector : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

//        enableEdgeToEdge()
        setContentView(R.layout.activity_recolector)

        val btnNavView = findViewById<BottomNavigationView>(R.id.menuNavigationView)
        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        val fab: FloatingActionButton = findViewById(R.id.fab)

        btnNavView.background = null
        btnNavView.menu.getItem(1).isEnabled=false

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        drawer = findViewById(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        mostrarNombreEnDrawer()

        fab.setOnClickListener {
            verificarYAbrirRutaActual()
        }

        btnNavView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.home -> {
                    loadFragment(HomeDashboard())
                    true
                }
                R.id.asignaciones -> {
                    loadFragment(MisAsignaciones())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
//            loadFragment(HomeDashboard())
            verificarYAbrirRutaActual()
        }
    }
    private fun mostrarNombreEnDrawer() {
        val user = FirebaseAuth.getInstance().currentUser
        val headerView = navigationView.getHeaderView(0)
        val txtNombreUsuario = headerView.findViewById<TextView>(R.id.nav_header_textView)
        txtNombreUsuario.text = "Recolector"

        if (user != null) {
            val userId = user.uid
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombre = document.getString("nombres")
                        txtNombreUsuario.text = nombre ?: "Usuario"
                    } else {
                        txtNombreUsuario.text = "Usuario desconocido"
                    }
                }
                .addOnFailureListener {
                    txtNombreUsuario.text = "Error al cargar"
                }
        } else {
            txtNombreUsuario.text = "No autenticado"
        }
    }
    private fun verificarYAbrirRutaActual() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar asignación activa como conductor
        db.collection("asignaciones_rutas")
            .whereEqualTo("conductorId", userId)
            .whereIn("estado", listOf("Programada", "En Progreso"))
            .limit(1)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val asignacionId = snapshots.documents[0].id
                    abrirRutaActual(asignacionId)
                } else {
                    // Buscar como ayudante
                    buscarComoAyudante(userId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al buscar asignación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun buscarComoAyudante(userId: String) {
        db.collection("asignaciones_rutas")
            .whereArrayContains("ayudantesIds", userId)
            .whereIn("estado", listOf("Programada", "En Progreso"))
            .limit(1)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val asignacionId = snapshots.documents[0].id
                    abrirRutaActual(asignacionId)
                } else {
                    Toast.makeText(
                        this,
                        "No tienes ninguna ruta asignada actualmente",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al buscar asignación: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun abrirRutaActual(asignacionId: String) {
        val fragment = RecolectorRuta.newInstance(asignacionId)
        loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_perfil -> loadFragment(PerfilRecolector())
            R.id.nav_cerrar_sesion -> { mostrarDialogoCerrarSesion() }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun mostrarDialogoCerrarSesion() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { dialog, _ ->
                cerrarSesion()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun cerrarSesion() {
        // Cerrar sesión en Firebase
        auth.signOut()

        // Mostrar mensaje
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        // Redirigir al Login (ajusta el nombre de tu Activity de login)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}