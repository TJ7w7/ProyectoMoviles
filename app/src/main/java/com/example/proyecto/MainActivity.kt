package com.example.proyecto

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btnIniciarSecion = findViewById<MaterialButton>(R.id.btnIniciarSecion)


        btnIniciarSecion.setOnClickListener {

            val intent = Intent(this, IniciarSecion::class.java)
            startActivity(intent)
        }
    }
}