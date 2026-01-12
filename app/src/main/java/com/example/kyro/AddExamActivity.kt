package com.example.kyro

import android.os.Bundle
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
                Toast.makeText(this, "Error: sesión no válida. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val nuevoExamen = Examen(
                id_usuario = idUsuarioActual,
                nombre_asignatura = asignatura,
                descripcion = descripcion,
                fecha_examen = fecha,
                notificacion1 = notificacion1,
                notificacion2 = notificacion2
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    SupabaseClient.client.postgrest["examenes"].insert(nuevoExamen)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "¡Examen guardado con éxito!", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
