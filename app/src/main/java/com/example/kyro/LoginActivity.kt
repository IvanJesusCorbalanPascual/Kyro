package com.example.kyro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // 1. VERIFICACIÓN AUTOMÁTICA: ¿Ya hay sesión y el usuario la guardó?
        // Esto se hace antes de cargar la vista para que el salto sea rápido
        verificarSesionExistente()

        setContentView(R.layout.activity_login)

        // --- VINCULACIÓN DE VISTAS ---
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        // Nuevos elementos
        val cbMantenerSesion = findViewById<CheckBox>(R.id.cbMantenerSesion)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // --- 2. LÓGICA DE INICIO DE SESIÓN ---
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // A) Validación Local
            if (email.isEmpty() || password.isEmpty()) {
                showKyroToast("Porfavor, rellena todos los campos")
                return@setOnClickListener
            }

            // B) Conexión con Supabase
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Intentamos loguear al usuario
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // --- NUEVO: GUARDAR PREFERENCIA DEL CHECKBOX ---
                    // Guardamos en la memoria del teléfono si el usuario quiere mantener la sesión
                    val sharedPref = getSharedPreferences("PreferenciasKyro", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean("mantener_sesion_activa", cbMantenerSesion.isChecked)
                        apply()
                    }
                    // -----------------------------------------------

                    // C) Éxito: Vamos al Home
                    withContext(Dispatchers.Main) {
                        showKyroToast("Bienvenido al Nido!")
                        // Toast.makeText(applicationContext, "¡Bienvenido al Nido!", Toast.LENGTH_SHORT).show()
                        irAHome()
                    }

                } catch (e: Exception) {
                    // D) Error
                    withContext(Dispatchers.Main) {
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

        // --- 3. LÓGICA DE RECUPERAR CONTRASEÑA ---
        tvForgotPassword.setOnClickListener {
            val emailActual = etEmail.text.toString().trim()
            mostrarDialogoRecuperarContrasena(emailActual)
        }

        // --- 4. NAVEGACIÓN AL REGISTRO ---
        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODOS AUXILIARES
    // ------------------------------------------------------------------------

    private fun verificarSesionExistente() {
        val sharedPref = getSharedPreferences("PreferenciasKyro", Context.MODE_PRIVATE)
        val quiereMantenerSesion = sharedPref.getBoolean("mantener_sesion_activa", false)

        lifecycleScope.launch {
            // Intentamos cargar la sesión desde la memoria interna de Supabase
            try {
                SupabaseClient.client.auth.loadFromStorage()
            } catch (e: Exception) {
                // Si falla la carga, no pasa nada, pedirá login
            }

            val session = SupabaseClient.client.auth.currentSessionOrNull()

            // Si hay sesión válida Y el usuario marcó el checkbox anteriormente -> Entrar directo
            if (session != null && quiereMantenerSesion) {
                withContext(Dispatchers.Main) {
                    irAHome()
                }
            }
        }
    }

    private fun irAHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Estas banderas evitan volver atrás al login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoRecuperarContrasena(emailPrevio: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar Contraseña")
        builder.setMessage("Introduce tu correo y te enviaremos un enlace mágico.")

        // Input de texto
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "tu@email.com"
        input.setText(emailPrevio)

        // Contenedor para darle márgenes
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 60
        params.rightMargin = 60
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Enviar") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                enviarCorreoRecuperacion(email)
            } else {
                Toast.makeText(this, "Escribe un correo válido", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun enviarCorreoRecuperacion(email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "¡Correo enviado! Revisa tu bandeja.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = if (e.message?.contains("Too many requests") == true)
                        "Espera un poco antes de volver a intentarlo."
                    else
                        "Error: ${e.message}"
                    Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}