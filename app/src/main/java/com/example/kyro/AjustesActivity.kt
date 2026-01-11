package com.example.kyro

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.content.Intent
import androidx.lifecycle.lifecycleScope // Para lanzar tareas en segundo plano
import kotlinx.coroutines.launch // Para usar corrutinas
import io.github.jan.supabase.gotrue.auth// Para acceder a la autenticación
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

class AjustesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        // Llama al Helper y dile que ilumine "nav_settings"
        NavigationHelper.setupBottomNavigation(this, R.id.nav_settings)

        initListeners()
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
            // Muestra el mensaje ir a perfil de usuario, ya que aun no esta creado.
            Toast.makeText(this, "Ir a Perfil de Usuario", Toast.LENGTH_SHORT).show()
        }

        // Botón de cambiar idioma, solo muestra "Seleccionar Idioma" se desarrollara proximamente
        val btnIdioma = findViewById<View>(R.id.btnIdioma)
        btnIdioma.setOnClickListener {
            Toast.makeText(this, "Seleccionar Idioma", Toast.LENGTH_SHORT).show()
        }

        // Igual que el botón anterior, muestra el texto, se desarollara en el futuro
        val btnTema = findViewById<View>(R.id.btnTema)
        btnTema.setOnClickListener {
            Toast.makeText(this, "Cambiar Tema Oscuro/Claro", Toast.LENGTH_SHORT).show()
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
            // Usamos una corrutina para la operación de red
            lifecycleScope.launch {
                try {
                    // 1. Llamamos a la función SQL que creamos en Supabase
                    // "delete_user" debe coincidir EXACTAMENTE con el nombre en SQL
                    SupabaseClient.client.postgrest.rpc("delete_user")

                    // 2. Limpiamos la sesión local en el móvil
                    SupabaseClient.client.auth.signOut()

                    // 3. Redirigimos al Login y borramos el historial
                    val intent = Intent(this@AjustesActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                    Toast.makeText(applicationContext, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@AjustesActivity,
                        "Error al eliminar cuenta: ${e.message}",
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

        // Poner el botón de eliminar en ROJO para avisar del peligro
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }
}