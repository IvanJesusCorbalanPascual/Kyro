package com.example.kyro.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope // Para lanzar tareas en segundo plano
import com.example.kyro.NavigationHelper
import com.example.kyro.R
import com.example.kyro.SupabaseClient
import com.example.kyro.entities.UserProfile
import com.example.kyro.showKyroToast
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
                // Manejo de errores silencioso
            }
        }
    }

    private fun initListeners() {
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
                .setTitle(getString(R.string.ajustes_idioma_dialogo))
                .setItems(idiomas) { _, which ->
                    // 0 es Español, 1 es Ingles
                    val languageTag = if (which == 1) "en" else "es"

                    // Esta función cambia el idioma  sin reiniciar la app de forma bruta
                    val appLocale = LocaleListCompat.forLanguageTags(languageTag)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
                .show()
        }

        // Igual que el botón anterior, muestra el texto, se desarollara en el futuro
        val btnTema = findViewById<View>(R.id.btnTema)
        btnTema.setOnClickListener {
            // Comprueba si esta activado el modo oscuro
            val currentModeIsNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Si esta en modo oscuro, pone el modo claro
            val newMode = if (currentModeIsNight == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
                // Si esta en modo claro, pone el modo oscuro
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
            // Aplica los cambios y cambia el tema
            AppCompatDelegate.setDefaultNightMode(newMode)
        }

        // Muestra un diálogo de advertencia para confirmar si el usuario quiere eliminar todos los datos de su cuenta
        val btnBorrarDatos = findViewById<View>(R.id.btnBorrarDatos)
        btnBorrarDatos.setOnClickListener {
            mostrarDialogoBorrarDatos()
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

    private fun mostrarDialogoBorrarDatos() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_borrar_datos_titulo))
        builder.setMessage(getString(R.string.dialog_borrar_datos_msg))

        builder.setPositiveButton(getString(R.string.btn_borrar_todo)) { _, _ ->
            lifecycleScope.launch {
                try {
                    val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        showKyroToast(getString(R.string.error_no_autenticado))
                        return@launch
                    }

                    // Borramos datos de las tablas principales asociadas al usuario.
                    // borramos primero las dependencias (Archivos, Tareas, Exámenes) y al final Asignaturas.

                    withContext(Dispatchers.IO) {
                        // Borrar Archivos
                        SupabaseClient.client.from("archivos").delete {
                            filter { eq("user_id", userId) }
                        }
                        // Borrar Tareas
                        SupabaseClient.client.from("tareas").delete {
                            filter { eq("id_usuario", userId) }
                        }
                        // Borrar Exámenes
                        SupabaseClient.client.from("examenes").delete {
                            filter { eq("id_usuario", userId) }
                        }
                        // Borrar Asignaturas
                        SupabaseClient.client.from("asignaturas").delete {
                            filter { eq("user_id", userId) }
                        }
                    }

                    // Limpiar preferencias locales (Modo focus, etc)
                    val sharedPref = getSharedPreferences("KyroPrefs", MODE_PRIVATE)
                    sharedPref.edit().clear().apply()

                    val intent = Intent(this@AjustesActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                    // Feedback visual y reiniciar interruptores visuales
                    showKyroToast(getString(R.string.toast_datos_borrados))
                    setupFocusSwitch() // Reiniciamos el switch visualmente

                } catch (e: Exception) {
                    e.printStackTrace()
                    showKyroToast(getString(R.string.error_borrar_datos_generico, e.message))
                }
            }
        }

        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        // Poner el botón en rojo para indicar peligro
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
    }

    private fun mostrarDialogoCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.cerrar_sesion))
        builder.setMessage(getString(R.string.dialog_cerrar_sesion_msg))

        // Si el usuario presiona que sí
        builder.setPositiveButton(getString(R.string.btn_si_salir)) { dialog, _ ->
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
                   showKyroToast(getString(R.string.error_cerrar_sesion))
                }
            }
        }

        // Si el usuario presiona que no, cierra el diálogo
        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun mostrarDialogoEliminarCuenta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.eliminar_cuenta))
        builder.setMessage(getString(R.string.dialog_eliminar_cuenta_msg))

        builder.setPositiveButton(getString(R.string.btn_eliminar)) { dialog, _ ->
            lifecycleScope.launch {
                try {
                    // Borramos al usuario en la nube
                    SupabaseClient.client.postgrest.rpc("delete_user")

                    // Limpiando la sesión local.
                    try {
                        SupabaseClient.client.auth.signOut()
                    } catch (e: Exception) {}

                    // Limpiando la preferencia de "Mantener sesión iniciada"
                    val sharedPref = getSharedPreferences("PreferenciasKyro", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        clear() // Borra todas las preferencias
                        apply()
                    }

                    // Navegamos al Login pase lo que pase
                    val intent = Intent(this@AjustesActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                 showKyroToast( getString(R.string.toast_cuenta_eliminada))

                } catch (e: Exception) {
                    // Este catch captura si falla el RPC (Paso 1), por ejemplo, por falta de internet.
                    e.printStackTrace()
                    showKyroToast(getString(R.string.error_eliminar_cuenta))
                }
            }
        }

        builder.setNegativeButton(getString(R.string.btn_cancelar)) { dialog, _ ->
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
        val sharedPref = getSharedPreferences("KyroPrefs", MODE_PRIVATE)

        // Lee como estaba antes, por defecto estará activado
        val isFocusEnabled = sharedPref.getBoolean("FOCUS_ENABLED", true)
        switchFocus.isChecked = isFocusEnabled

        // Guarda los cambios al tocar el botón
        switchFocus.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("FOCUS_ENABLED", isChecked).apply()

            // Muestra al usuario visualmente si esta activado o desactivado
            val estado = if(isChecked) getString(R.string.estado_activado) else getString(R.string.estado_desactivado)
            showKyroToast(getString(R.string.toast_focus_estado, estado))
        }
    }
}