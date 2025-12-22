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
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Vinculamos las vistas
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        // Lógica del Botón Registrar
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // --- Validaciones ---
            // Campos en blanco
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Contraseñas que no coinciden
            if (password != confirmPassword) {
                etConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // Contraseña minimo 6 caracteres
            if (password.length < 6) {
                etPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            // REGISTRO EN SUPABASE (Asíncrono)
            // Usamos lifecycleScope para lanzar una tarea en segundo plano, esto se encarga de evitar errores si el usuario cierra la pantalla
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // A) Crear el usuario en Auth (Authentication)
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // B) Obtener el usuario recién creado
                    val user = SupabaseClient.client.auth.currentUserOrNull()

                    if (user != null) {
                        // C) Crear la fila en la tabla 'profiles' (Database)
                        val newProfile = UserProfile(
                            id = user.id, // Vinculamos Auth ID con Profile ID
                            username = name
                        )

                        SupabaseClient.client.from("profiles").insert(newProfile)

                        // D) Volver al hilo principal para cambiar de pantalla
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "¡Registro exitoso! Por favor confirma tu email.", Toast.LENGTH_LONG).show()

                            // Navegar al Login o directamente al Home
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                } catch (e: Exception) {
                    // Manejo de errores (ej: email ya existe, sin internet)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Navegación al Login
        tvGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}