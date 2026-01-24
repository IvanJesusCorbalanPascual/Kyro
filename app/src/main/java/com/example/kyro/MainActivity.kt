package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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