package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Llama al Helper y dile que ilumine "nav_home"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_home)

        setupQuickActions()
    }

    private fun setupQuickActions() {
        val btnQuickAI = findViewById<MaterialCardView>(R.id.btnQuickAI)
        btnQuickAI.setOnClickListener {
            Toast.makeText(this, "Abriendo Kyro IA...", Toast.LENGTH_SHORT).show()
        }

        val btnQuickSyllabus = findViewById<MaterialCardView>(R.id.btnQuickSyllabus)
        btnQuickSyllabus.setOnClickListener {
            startActivity(Intent(this, TemarioActivity::class.java))
        }
    }
}