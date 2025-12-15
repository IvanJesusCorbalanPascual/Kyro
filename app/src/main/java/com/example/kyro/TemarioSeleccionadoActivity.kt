package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TemarioSeleccionadoActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario_seleccionado)

        // Al pulsar "Modificar", desplaza a la siguiente pantalla
        val btnModificar = findViewById<TextView>(R.id.btnModificar)

        btnModificar.setOnClickListener {
            val intent = Intent(this, ModificarTemarioActivity::class.java)
            startActivity(intent)
        }
    }
}