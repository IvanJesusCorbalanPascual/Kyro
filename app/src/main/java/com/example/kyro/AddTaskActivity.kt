package com.example.kyro

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // --- Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Vistas ---
        val etAsignatura: EditText = findViewById(R.id.etAsignaturaTarea)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionTarea)
        val etFecha: EditText = findViewById(R.id.etFechaEntrega)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardarTarea)

        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L

        if (isEditMode) {
            // Modo Edición
            toolbar.title = "Editar Tarea"
            val title = intent.getStringExtra("title")
            val description = intent.getStringExtra("description")
            val date = intent.getStringExtra("date")
            val notif1 = intent.getStringExtra("notif1")
            val notif2 = intent.getStringExtra("notif2")

            etAsignatura.setText(title)
            etDescripcion.setText(description)
            etFecha.setText(date)

            val adapter = spinnerNotificacion1.adapter as? ArrayAdapter<String>
            if (adapter != null) {
                spinnerNotificacion1.setSelection(adapter.getPosition(notif1))
                spinnerNotificacion2.setSelection(adapter.getPosition(notif2))
            }
        }

        btnGuardar.setOnClickListener {
            val asignatura = etAsignatura.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val fecha = etFecha.text.toString().trim()
            val notificacion1 = spinnerNotificacion1.selectedItem.toString()
            val notificacion2 = spinnerNotificacion2.selectedItem.toString()

            if (asignatura.isEmpty() || descripcion.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idUsuarioActual = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (idUsuarioActual == null) {
                Toast.makeText(this, "Error: sesión no válida.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (isEditMode) {
                        // Actualizar
                        SupabaseClient.client.postgrest["tareas"].update({
                            set("nombre_asignatura", asignatura)
                            set("descripcion", descripcion)
                            set("fecha_entrega", fecha)
                            set("notificacion1", notificacion1)
                            set("notificacion2", notificacion2)
                        }) { filter { eq("id", eventId) } }
                    } else {
                        // Insertar
                        val nuevaTarea = Tarea(
                            id_usuario = idUsuarioActual,
                            nombre_asignatura = asignatura,
                            descripcion = descripcion,
                            fecha_entrega = fecha,
                            notificacion1 = notificacion1,
                            notificacion2 = notificacion2
                        )
                        SupabaseClient.client.postgrest["tareas"].insert(nuevaTarea)
                    }

                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "¡Tarea actualizada!" else "¡Tarea guardada!"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
