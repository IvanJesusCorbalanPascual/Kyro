package com.example.kyro.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterActivity : AppCompatActivity() {

    // ID DE CLIENTE WEB DE GOOGLE CLOUD
    private val WEB_CLIENT_ID = "500842940773-ea8mcvvn13uqe773t99ql0p91ct41oj2.apps.googleusercontent.com"

    private lateinit var progressBar: ProgressBar
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Vinculamos las vistas
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        progressBar = findViewById(R.id.progressBarRegistro)

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

            // UI Carga
            setLoading(true)

            // REGISTRO EN SUPABASE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Creando el usuario en Auth
                    // Esto envía el correo de confirmación automáticamente (si activaste el paso 1)
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        this.data = buildJsonObject{
                            put("display_name", name)
                            put("username", name)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        // Mostramos el aviso de éxito
                        mostrarDialogoCorreoEnviado()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        setLoading(false)
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

    private fun setLoading(loading: Boolean) {
        if (loading) {
            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnRegister.isEnabled = true
        }
    }

    private fun mostrarDialogoCorreoEnviado() {
        AlertDialog.Builder(this)
            .setTitle("¡Casi listo!")
            .setMessage("Hemos enviado un correo de confirmación a tu email. Por favor, verifica tu cuenta antes de iniciar sesión.")
            .setPositiveButton("Ir al Login") { _, _ ->
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showKyroToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // --- FUNCIÓN PRIVADA PARA GOOGLE ---
    private fun registrarseConGoogle() {
        lifecycleScope.launch {
            try {
                // Configurar la petición a Google
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Cambiado a TRUE
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false) // Cambiado a TRUE para mejor UX
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Abrir la ventanita de selección de cuentas
                val credentialManager = CredentialManager.create(this@RegisterActivity)
                val result = credentialManager.getCredential(
                    this@RegisterActivity,
                    request
                )

                // Procesar la respuesta
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleToken = googleIdTokenCredential.idToken

                    // Enviar el token a Supabase
                    loginEnSupabaseConGoogle(googleToken)
                } else {
                    showKyroToast("Error: No se pudo obtener la credencial.")
                }

            } catch (e: GetCredentialException) {
                // Manejar específicamente "No credentials available"
                when {
                    e.message?.contains("No credentials available") == true -> {
                        showKyroToast("No hay cuentas de Google disponibles. " +
                                "Por favor, agrega una cuenta en Configuración del dispositivo.")
                    }
                    e.message?.contains("cancelled", ignoreCase = true) == true -> {
                        // Usuario canceló, no mostrar error
                        Log.d("GoogleAuth", "Cancelado por el usuario")
                    }
                    else -> {
                        showKyroToast("Error al autenticar con Google: ${e.message}")
                    }
                }
                Log.e("GoogleAuth", "GetCredentialException", e)
            } catch (e: Exception) {
                showKyroToast("Error inesperado: ${e.message}")
                Log.e("GoogleAuth", "Exception inesperada", e)
            }
        }
    }

    private suspend fun loginEnSupabaseConGoogle(googleToken: String) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signInWith(IDToken) {
                    this.idToken = googleToken
                    this.provider = Google
                }

                withContext(Dispatchers.Main) {
                    showKyroToast("¡Bienvenido!")
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