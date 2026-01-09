package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch


class ModificarTemarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_temario)

        // Conenexiones de elementos, los busca en el XML por id para usarlos en la lógica
        val etContenido = findViewById<EditText>(R.id.etContenidoApuntes)
        val btnAnalizar = findViewById<MaterialButton>(R.id.btnAnalizar)
        val btnAdjuntar = findViewById<TextView>(R.id.btnAdjuntar)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Lógica de los botones

        btnAdjuntar.setOnClickListener {
            mostrarMensaje("Funcionalidad para subir el PDF aún esta desarrollandose")
        }

        btnAnalizar.setOnClickListener {
            // Coge el texto y lo almacena quitando espacios sobrantes
            val contenidoTexto = etContenido.text.toString().trim()

            // Comprueba si esta vacio el edit text de los apuntes
            if (contenidoTexto.isEmpty()) {
                // Marca un error visual en la caja si no hay texto y avisa al usuario
                etContenido.error = "Debes pegar el texto del temario aquí"
                mostrarMensaje("El campo de texto está vacío")

            // Ha funcionado correctamente habiendo detectado que había texto
            } else {
                mostrarMensaje("Subiento apuntes a la nube...")

                // Bloquea el botón para evitar ser pulsado mientras carga
                btnAnalizar.isEnabled = false

                lifecycleScope.launch {
                    try {
                        // Empaqueta el texto en el objeto "ApunteUsuario" que esta dentro de Models.kt
                        val nuevoApunte = ApunteUsuario(contenido = contenidoTexto)

                        // Conexión con SupaBase que envia los datos a la tabla "apuntes_usuario"
                        SupabaseClient.client
                            .from("apuntes_usuario")
                            .insert(nuevoApunte)

                        mostrarMensaje("¡Guardado en la nube!")

                        // Vuelve a la pantalla anterior de temario seleccionado cuando ha terminado de guardar
                        val intent = Intent(this@ModificarTemarioActivity, TemarioSeleccionadoActivity::class.java)
                        startActivity(intent)
                        // Cierra la pantalla
                        finish()

                    } catch (e: Exception) {
                        // Si falla el internet, muestra el error y permite al usuario reintentarlo
                        e.printStackTrace()
                        mostrarMensaje("Error al subir: ${e.message}")

                        // Vuelve a activar el botón
                        btnAnalizar.isEnabled = true
                    }
                }
            }
        }

        // Mantiene seleccionado temario en la barra de navegación
        bottomNavigation.selectedItemId = R.id.nav_syllabus

        // Configuración de la barra de navegación
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_syllabus -> {
                    startActivity(Intent(this, TemarioActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarioActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    true
                }
                R.id.nav_ai_chat -> {
                    mostrarMensaje("IA: Próximamente ")
                    true
                }
                else -> false
            }
        }
    }
    // Función para mostrar los mensajes y no tener que escribir Toast.makeText constantemente
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}