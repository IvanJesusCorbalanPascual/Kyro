package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.gotrue.providers.builtin.IDToken

class RegisterActivity : AppCompatActivity() {

    // ID DE CLIENTE WEB" DE GOOGLE CLOUD
    private val WEB_CLIENT_ID = "500842940773-ea8mcvvn13uqe773t99ql0p91ct41oj2.apps.googleusercontent.com"

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
        val btnGoogle = findViewById<Button>(R.id.btnGoogle) // El botón nuevo

        // Logica Google
        btnGoogle.setOnClickListener {
            registrarseConGoogle()
        }

        // Logica registro por email
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validaciones
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showKyroToast("Por favor, rellena todos los campos")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            // REGISTRO EN SUPABASE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Creando el usuario en Auth (Authentication).
                    // Un trigger en la base de datos de Supabase debería encargarse de crear el perfil.
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        this.data = buildJsonObject{
                            put("display_name", name)
                            put("username", name)
                        }
                    }

                    // Volver al hilo principal para cambiar de pantalla
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "¡Registro exitoso! Por favor confirma tu email.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Navegar al Login
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (e.message?.contains("already registered") == true)
                            "Este correo ya está en uso"
                        else "Error: ${e.message}"
                        showKyroToast(errorMsg)
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

    // --- FUNCIÓN PRIVADA PARA GOOGLE ---
    private fun registrarseConGoogle() {
        print("hola")
        lifecycleScope.launch {
            try {
                // 1. Configurar la petición a Google
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // false = mostrar todas las cuentas
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 2. Abrir la ventanita de selección de cuentas
                val credentialManager = CredentialManager.create(this@RegisterActivity)
                val result = credentialManager.getCredential(this@RegisterActivity, request)

                // 3. Procesar la respuesta
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleToken = googleIdTokenCredential.idToken

                    // 4. Enviar el token a Supabase
                    loginEnSupabaseConGoogle(googleToken)
                } else {
                    showKyroToast("Error: No se pudo obtener la credencial.")
                }

            } catch (e: Exception) {
                // Si el usuario cierra la ventana sin elegir cuenta, entra aquí.
                // No mostramos error para no molestar.
            }
        }
    }

    private suspend fun loginEnSupabaseConGoogle(googleToken: String) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signInWith(IDToken) {
                    this.idToken = googleToken
                    this.provider = Google // Especificamos que el token viene de Google
                }

                withContext(Dispatchers.Main) {
                    showKyroToast("¡Bienvenido!")
                    // Al ser Google, vamos directo al Home
                    val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showKyroToast("Error al conectar con Supabase: ${e.message}")
                }
            }
        }
    }
}