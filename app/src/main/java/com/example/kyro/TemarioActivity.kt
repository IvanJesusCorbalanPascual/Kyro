package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TemarioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario)

        // Al pulsar "estudiar" en el primer elemento, desplaza a la vista temarioseleccionado
        val btnEstudiar = findViewById<TextView>(R.id.btnEstudiar1)

        btnEstudiar.setOnClickListener {
            // Saltamos a la siguiente pantalla de tu flujo
            val intent = Intent(this, TemarioSeleccionadoActivity::class.java)
            startActivity(intent)
        }
    }

}