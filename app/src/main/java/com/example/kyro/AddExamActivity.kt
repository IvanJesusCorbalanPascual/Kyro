package com.example.kyro

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exam)

        val etAsignatura: EditText = findViewById(R.id.etAsignaturaExamen)
        val etDescripcion: EditText = findViewById(R.id.etDescripcionExamen)
        val etFecha: EditText = findViewById(R.id.etFechaExamen)
        val spinnerNotificacion1: Spinner = findViewById(R.id.spinnerNotificacion1)
        val spinnerNotificacion2: Spinner = findViewById(R.id.spinnerNotificacion2)
        val btnGuardar: Button = findViewById(R.id.btnGuardarExamen)

        val eventId = intent.getLongExtra("id", -1)
        val isEditMode = eventId != -1L

        if (isEditMode) {
            // Modo Edición
            val title = intent.getStringExtra("title")
            val description = intent.getStringExtra("description")
            val date = intent.getStringExtra("date")
            val notif1 = intent.getStringExtra("notif1")
            val notif2 = intent.getStringExtra("notif2")

            etAsignatura.setText(title)
            etDescripcion.setText(description)
            etFecha.setText(date)

            // Asegurarse de que el adapter no es nulo
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
                        // Actualizar examen existente
                        SupabaseClient.client.postgrest["examenes"].update({
                            set("nombre_asignatura", asignatura)
                            set("descripcion", descripcion)
                            set("fecha_examen", fecha)
                            set("notificacion1", notificacion1)
                            set("notificacion2", notificacion2)
                        }) { filter { eq("id", eventId) } }
                    } else {
                        // Insertar nuevo examen
                        val nuevoExamen = Examen(
                            id_usuario = idUsuarioActual,
                            nombre_asignatura = asignatura,
                            descripcion = descripcion,
                            fecha_examen = fecha,
                            notificacion1 = notificacion1,
                            notificacion2 = notificacion2
                        )
                        SupabaseClient.client.postgrest["examenes"].insert(nuevoExamen)
                    }

                    withContext(Dispatchers.Main) {
                        val message = if (isEditMode) "¡Examen actualizado!" else "¡Examen guardado!"
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
