package com.example.kyro

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.content.Intent
import android.content.Context
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope // Para lanzar tareas en segundo plano
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch // Para usar corrutinas
import io.github.jan.supabase.gotrue.auth// Para acceder a la autenticación
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AjustesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        initListeners()
        setupFocusSwitch()
    }

    override fun onResume() {
        super.onResume()
        // Llama al Helper y dile que ilumine "nav_settings"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_settings)
        loadUserProfile()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val userProfile = SupabaseClient.client.from("profiles").select { filter { eq("id", userId) } }.decodeSingleOrNull<UserProfile>()

                withContext(Dispatchers.Main) {
                    userProfile?.let {
                        val tvUsername = findViewById<TextView>(R.id.tvUsername)
                        tvUsername.text = it.username
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun initListeners() {
        // Lógica de la flecha para volver atrás
        val btnAtras = findViewById<ImageView>(R.id.btnAtras)
        btnAtras.setOnClickListener {
            finish() // Cierra la pantalla, volviendo a la anterior
        }

        // Lógica de configuración de cuenta (Se profundizara en el futuro)
        val cardCuenta = findViewById<MaterialCardView>(R.id.cardCuenta)
        cardCuenta.setOnClickListener {
            val intent = Intent(this, UsuarioActivity::class.java)
            startActivity(intent)
        }

        // Botón de cambiar idioma, solo muestra "Seleccionar Idioma" se desarrollara proximamente
        val btnIdioma = findViewById<View>(R.id.btnIdioma)
        btnIdioma.setOnClickListener {
            val idiomas = arrayOf("Español", "English")
            AlertDialog.Builder(this)
                .setTitle("Selecciona Idioma / Select Language")
                .setItems(idiomas) { _, which ->
                    // 0 es Español, 1 es Ingles
                    val languageTag = if (which == 1) "en" else "es"

                    // Esta función cambia el idioma  sin reiniciar la app de forma bruta
                    val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(languageTag)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                }
                .show()
        }

        // Igual que el botón anterior, muestra el texto, se desarollara en el futuro
        val btnTema = findViewById<View>(R.id.btnTema)
        btnTema.setOnClickListener {
            // Comprueba si esta activado el modo oscuro
            val currentModeIsNight = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

            // Si esta en modo oscuro, pone el modo claro
            val newMode = if (currentModeIsNight == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            // Si esta en modo claro, pone el modo oscuro
        } else {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            }
            // Aplica los cambios y cambia el tema
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode)
        }

        // Muestra un diálogo de advertencia para confirmar si el usuario quiere cerrar sesión
        val btnCerrarSesion = findViewById<View>(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        // Muestra un dialogo de advertencia para confirmar si el usuario quiere eliminar su cuenta
        val btnEliminarCuenta = findViewById<View>(R.id.btnEliminarCuenta)
        btnEliminarCuenta.setOnClickListener {
            mostrarDialogoEliminarCuenta()
        }
    }

    // --- Métodos para mostrar los diálogos ---

    private fun mostrarDialogoCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar Sesión")
        builder.setMessage("¿Estás seguro de que quieres salir?")

        // Si el usuario presiona que sí
        builder.setPositiveButton("Sí, salir") { dialog, _ ->
            // Usamos lifecycleScope porque el cierre de sesión es una operación de red (suspendida)
            lifecycleScope.launch {
                try {
                    // Cerrar sesión en Supabase (Limpia el token local)
                    SupabaseClient.client.auth.signOut()

                    // Crear el Intent para ir al Login
                    val intent = Intent(this@AjustesActivity, LoginActivity::class.java)

                    // IMPORTANTE: Estas flags borran el historial de pantallas.
                    // Así el usuario no puede dar al botón "Atrás" y volver a entrar.
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish() // Destruye la actividad de Ajustes

                } catch (e: Exception) {
                    Toast.makeText(this@AjustesActivity, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Si el usuario presiona que no, cierra el diálogo
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun mostrarDialogoEliminarCuenta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar Cuenta")
        builder.setMessage("Esta acción es permanente y borrará todos tus datos. ¿Estás seguro?")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            lifecycleScope.launch {
                try {
                    // Borramos al usuario en la nube
                    SupabaseClient.client.postgrest.rpc("delete_user")

                    // Limpiando la sesión local.
                    try {
                        SupabaseClient.client.auth.signOut()
                    } catch (e: Exception) {}

                    // Limpiando la preferencia de "Mantener sesión iniciada"
                    val sharedPref = getSharedPreferences("PreferenciasKyro", android.content.Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        clear() // Borra todas las preferencias
                        apply()
                    }

                    // Navegamos al Login pase lo que pase
                    val intent = Intent(this@AjustesActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                    Toast.makeText(applicationContext, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    // Este catch captura si falla el RPC (Paso 1), por ejemplo, por falta de internet.
                    e.printStackTrace()
                    Toast.makeText(
                        this@AjustesActivity,
                        "Error de conexión: No se pudo eliminar la cuenta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }

    private fun setupFocusSwitch() {
        // Busca el interruptor en la vista
        val switchFocus = findViewById<SwitchCompat>(R.id.switchModoFocus)

        // Abre la memoria del móvil
        val sharedPref = getSharedPreferences("KyroPrefs", Context.MODE_PRIVATE)

        // Lee como estaba antes, por defecto estará activado
        val isFocusEnabled = sharedPref.getBoolean("FOCUS_ENABLED", true)
        switchFocus.isChecked = isFocusEnabled

        // Guarda los cambios al tocar el botón
        switchFocus.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("FOCUS_ENABLED", isChecked).apply()

            // Muestra al usuario visualmente si esta activado o desactivado
            val estado = if(isChecked) "Activado" else "Desactivado"
            showKyroToast("Modo Focus $estado")
        }
    }
}