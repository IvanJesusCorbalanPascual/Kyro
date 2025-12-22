package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Vinculacion de vistas
        // Guardando lo que escribe el usuario en los EditText
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        // 2. LÓGICA DE INICIO DE SESIÓN
        btnLogin.setOnClickListener {
            // Obtenemos el texto limpio (sin espacios al final)
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // A) Validación Local (Rápida)
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // B) Conexión con Supabase (Asíncrona)
            // Usamos IO porque es una operación de red
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Intentamos loguear al usuario
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // C) Éxito: Volvemos al hilo principal (Main) para cambiar la pantalla
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "¡Bienvenido al Nido!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        // Estas banderas (Flags) evitan que el usuario pueda volver al Login pulsando "Atrás"
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
                    // D) Error: Mostramos qué ha pasado
                    withContext(Dispatchers.Main) {
                        // Traducimos errores comunes de Supabase para que quede más profesional
                        val errorMessage = when {
                            e.message?.contains("Invalid login credentials") == true -> "Email o contraseña incorrectos"
                            e.message?.contains("Email not confirmed") == true -> "Debes confirmar tu correo electrónico"
                            e.message?.contains("network") == true -> "Error de conexión. Revisa tu internet."
                            else -> "Error: ${e.message}"
                        }
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // 3. NAVEGACIÓN AL REGISTRO
        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            // No usamos finish() aquí por si el usuario quiere volver atrás al Login
        }
    }
}