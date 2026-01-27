package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.example.kyro.showKyroToast
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val WEB_CLIENT_ID = "500842940773-ea8mcvvn13uqe773t99ql0p91ct41oj2.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificación automática de Sesión
        verificarSesionExistente()

        setContentView(R.layout.activity_login)

        // --- VINCULACIÓN DE VISTAS ---
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle) // Nuevo botón
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val cbMantenerSesion = findViewById<CheckBox>(R.id.cbMantenerSesion)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Login con Email y Contraseña
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showKyroToast("Por favor, rellena todos los campos")
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // Guardar preferencia
                    guardarPreferenciaSesion(cbMantenerSesion.isChecked)

                    withContext(Dispatchers.Main) {
                        showKyroToast("¡Bienvenido al Nido!")
                        irAHome()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = when {
                            e.message?.contains("Invalid login credentials") == true -> "Email o contraseña incorrectos"
                            e.message?.contains("Email not confirmed") == true -> "Debes confirmar tu correo electrónico"
                            e.message?.contains("network") == true -> "Error de conexión. Revisa tu internet."
                            else -> "Error: ${e.message}"
                        }
                        showKyroToast(errorMessage)
                    }
                }
            }
        }

        // --- LOGIN CON GOOGLE ---
        btnGoogle.setOnClickListener {
            iniciarLoginGoogle(cbMantenerSesion.isChecked)
        }

        // Restablecer contraseña
        tvForgotPassword.setOnClickListener {
            val emailActual = etEmail.text.toString().trim()
            mostrarDialogoRecuperarContrasena(emailActual)
        }

        // Registro
        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // Cerramos Login para no volver atrás
        }
    }

    private fun iniciarLoginGoogle(mantenerSesion: Boolean) {
        lifecycleScope.launch {
            try {
                // Configurar la petición a Google
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Lanzar la ventana de cuentas
                val credentialManager = CredentialManager.create(this@LoginActivity)
                val result = credentialManager.getCredential(this@LoginActivity, request)

                // Procesar el resultado
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleToken = googleIdTokenCredential.idToken

                    // Enviar token a Supabase
                    loginEnSupabaseConTokenGoogle(googleToken, mantenerSesion)
                } else {
                    showKyroToast("Error: Tipo de credencial desconocida")
                }
            } catch (e: Exception) {
                // Usuario canceló o error de Google (ya manejado en logs internos si fuera necesario)
                e.printStackTrace()

            }
        }
    }

    private suspend fun loginEnSupabaseConTokenGoogle(token: String, mantenerSesion: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                // Usamos IDToken con provider = Google
                SupabaseClient.client.auth.signInWith(IDToken) {
                    this.idToken = token
                    this.provider = Google
                }

                guardarPreferenciaSesion(mantenerSesion)

                withContext(Dispatchers.Main) {
                    showKyroToast("¡Bienvenido al Nido!")
                    irAHome()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showKyroToast("Error de conexión con Supabase: ${e.message}")
                }
            }
        }
    }

    private fun guardarPreferenciaSesion(mantener: Boolean) {
        val sharedPref = getSharedPreferences("PreferenciasKyro", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("mantener_sesion_activa", mantener)
            apply()
        }
    }

    private fun verificarSesionExistente() {
        val sharedPref = getSharedPreferences("PreferenciasKyro", MODE_PRIVATE)
        val quiereMantenerSesion = sharedPref.getBoolean("mantener_sesion_activa", false)

        lifecycleScope.launch {
            try {
                // Cargar sesión guardada si existe
                SupabaseClient.client.auth.loadFromStorage()
            } catch (e: Exception) {
                // Ignorar si no hay archivo de sesión
            }

            val session = SupabaseClient.client.auth.currentSessionOrNull()

            // Si hay sesión y el usuario dijo "Mantener sesión" -> Entrar
            if (session != null && quiereMantenerSesion) {
                withContext(Dispatchers.Main) {
                    irAHome()
                }
            }
        }
    }

    private fun irAHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoRecuperarContrasena(emailPrevio: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar Contraseña")
        builder.setMessage("Introduce tu correo y te enviaremos un enlace mágico.")

        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "tu@email.com"
        input.setText(emailPrevio)

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

        // La _ avisa al programa de que va a recibir un valor pero no lo vamos a usar
        builder.setPositiveButton("Enviar") { _, _ -> // Serían dialog e id pero no los necesitamos
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                enviarCorreoRecuperacion(email)
            } else {
                showKyroToast("Escribe un correo válido")
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }

    private fun enviarCorreoRecuperacion(email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                withContext(Dispatchers.Main) {
                    showKyroToast("¡Correo enviado! Revisa tu bandeja.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = if (e.message?.contains("Too many requests") == true)
                        "Espera un poco antes de volver a intentarlo."
                    else "Error: ${e.message}"
                    showKyroToast(errorMsg)
                }
            }
        }
    }
}