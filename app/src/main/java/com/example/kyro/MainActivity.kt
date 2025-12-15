package com.example.kyro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crea un intento temporal para ir a mi actividad principal
        val intent = Intent(this, TemarioActivity::class.java)

        // Inicia
        startActivity(intent)

        // Cierra esta pantalla para que no moleste
        finish()
    }
}