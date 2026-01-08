package com.example.kyro

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // 1. Vinculación de vistas
        val etAsignatura: EditText = findViewById(R.id.etAsignaturaTarea)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionTarea)
        val etFecha: EditText = findViewById(R.id.etFechaEntrega)
        val btnGuardar: Button = findViewById(R.id.btnGuardarTarea)

        // 2. Lógica de guardado
        btnGuardar.setOnClickListener {
            val asignatura = etAsignatura.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val fecha = etFecha.text.toString().trim()

            // Validación rápida
            if (asignatura.isEmpty() || descripcion.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Obtenemos el ID del usuario actual. Si no está logueado, no hacemos nada.
            val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (idUsuarioActual == null) {
                Toast.makeText(this, "Error: sesión no válida. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Creamos el objeto Tarea
            val nuevaTarea = Tarea(
                id_usuario = idUsuarioActual,
                nombre_asignatura = asignatura,
                descripcion = descripcion,
                fecha_entrega = fecha
            )

            // 3. Conexión con Supabase (similar al Login)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Usamos postgrest para interactuar con la tabla 'tareas'
                    SupabaseClient.client.postgrest["tareas"].insert(nuevaTarea)

                    // Éxito: volvemos al hilo principal para notificar y cerrar
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "¡Tarea guardada con éxito!", Toast.LENGTH_SHORT).show()
                        finish() // Cierra esta pantalla y vuelve al calendario
                    }

                } catch (e: Exception) {
                    // Error: mostramos qué ha pasado
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
