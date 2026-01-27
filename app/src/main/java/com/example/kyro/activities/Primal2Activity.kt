package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.kyro.R

class Primal2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_primal_2)

        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val layoutDots = findViewById<LinearLayout>(R.id.layoutDots)

        val punto1 = layoutDots.getChildAt(0)
        val punto2 = layoutDots.getChildAt(1)
        val punto3 = layoutDots.getChildAt(2)

        // Lógica de Puntos
        punto1.isSelected = false
        punto2.isSelected = true
        punto3.isSelected = false

        btnNext.setOnClickListener {
            // Ahora vamos a la Primal3Activity
            val intent = Intent(this, Primal3Activity::class.java)
            startActivity(intent)

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            // Para que el usuario no pueda voler atrás con el botón 'back' del móvil
            finish()
        }
    }
}