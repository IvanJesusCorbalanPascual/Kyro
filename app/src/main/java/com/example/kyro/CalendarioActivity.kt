package com.example.kyro

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class CalendarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_tareas)
        // Llama al Helper y dile que ilumine "nav_calendar"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_calendar)
    }
}