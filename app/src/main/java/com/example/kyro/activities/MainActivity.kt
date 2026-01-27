package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kyro.MonitorService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val monitorIntent = Intent(this, MonitorService::class.java)
        startService(monitorIntent)
        val intent = Intent(this, Primal1Activity::class.java)
        startActivity(intent)
        finish()
    }
}