package com.example.kyro.activities

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.example.kyro.showKyroToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class UsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuario)

        initUI()
    }

    private fun initUI() {
        val btnAtras = findViewById<ImageView>(R.id.btnAtras)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)

        // 1. Botón atrás
        btnAtras.setOnClickListener { finish() }

        // 2. Cargar datos actuales
        cargarDatosUsuario(etUsername, etEmail)

        // 3. Guardar cambios
        btnGuardar.setOnClickListener {
            val nuevoNombre = etUsername.text.toString().trim()
            if (nuevoNombre.isNotEmpty()) {
                actualizarPerfil(nuevoNombre)
            } else {
                showKyroToast("El nombre no puede estar vacío")
            }
        }
    }

    private fun cargarDatosUsuario(etName: TextInputEditText, etEmail: TextInputEditText) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = SupabaseClient.client.auth.currentUserOrNull()
            user?.let { u ->
                // Ponemos el email (viene de Auth)
                withContext(Dispatchers.Main) {
                    etEmail.setText(u.email)
                }

                // Buscamos el nombre en la tabla 'profiles'
                try {
                    val result = SupabaseClient.client
                        .from("profiles")
                        .select {
                            filter {
                                eq("id", u.id)
                            }
                        }.decodeSingle<JsonObject>() // Decodificamos como JSON genérico

                    // Extraemos el campo "username"
                    val nombre = result["username"]?.jsonPrimitive?.content ?: ""

                    withContext(Dispatchers.Main) {
                        etName.setText(nombre)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun actualizarPerfil(nuevoNombre: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@launch

            try {
                // Actualizamos la tabla 'profiles'
                SupabaseClient.client.from("profiles").update(
                    {
                        set("username", nuevoNombre)
                    }
                ) {
                    filter {
                        eq("id", user.id)
                    }
                }

                withContext(Dispatchers.Main) {
                    showKyroToast("Perfil actualizado correctamente")
                    finish() // Cerramos para volver a ajustes
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showKyroToast("Error al guardar: ${e.message}")
                }
            }
        }
    }
}