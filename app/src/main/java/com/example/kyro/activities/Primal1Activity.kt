package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout // Importante importar esto
import androidx.appcompat.app.AppCompatActivity
import com.example.kyro.R

class Primal1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_primal_1)

        // Leemos la preferencia antes de cargar el diseño
        val sharedPref = getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
        val yaVisto = sharedPref.getBoolean("intro_completada", false) // false es el valor por defecto

        // Referenciando los elementos de la UI
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val layoutDots = findViewById<LinearLayout>(R.id.layoutDots)

        // Lógica de los Puntos (Indicadores)
        // Obtenemos cada punto por su posición (índice 0, 1, 2)
        val punto1 = layoutDots.getChildAt(0)
        val punto2 = layoutDots.getChildAt(1)
        val punto3 = layoutDots.getChildAt(2)

        // Lógica de Puntos
        punto1.isSelected = true
        punto2.isSelected = false
        punto3.isSelected = false

        // Si el usuario ya ha visto las pantallas primales, las salta
        if (yaVisto) {
            val intent = Intent(this, LoginActivity::class.java) // A donde quieras ir
            startActivity(intent)
            finish() // Cierra esta actividad para que no se vea
            return // Detiene la ejecución para no cargar el diseño de abajo
        }

        // Lógica del Botón Siguiente
        btnNext.setOnClickListener {
            val intent = Intent(this, Primal2Activity::class.java)
            startActivity(intent)

            // Animación suave (mantiene el "Flow" visual)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            // Para que el usuario vuelva atrás con el botón 'back' del móvil
            finish()
        }
    }
}