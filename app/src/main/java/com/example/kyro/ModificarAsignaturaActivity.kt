package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch


class ModificarAsignaturaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificar_asignatura)

        // Conenexiones de elementos, los busca en el XML por id para usarlos en la lógica
        val etContenido = findViewById<EditText>(R.id.etContenidoApuntes)
        val etTitulo = findViewById<EditText>(R.id.etTituloModificar)
        val btnAnalizar = findViewById<MaterialButton>(R.id.btnAnalizar)
        val btnEliminar = findViewById<MaterialButton>(R.id.btnEliminar)
        val btnAdjuntar = findViewById<TextView>(R.id.btnAdjuntar)

        // Vincula el título de la tarjeta
        val tvTituloTarjeta = findViewById<TextView>(R.id.tvNombreAsignatura)

        // Recibe los datos de la pantalla anterior
        val contenidoOriginal = intent.getStringExtra("EXTRA_CONTENIDO") ?: ""
        val tituloOriginal = intent.getStringExtra("EXTRA_TITULO") ?: ""
        val idRecibido = intent.getLongExtra("EXTRA_ID", -1)

        // Rellena la caja de texto con lo que hay en la BD
        etContenido.setText(contenidoOriginal)

        // Pone el título en su sitio
        etTitulo.setText(tituloOriginal)


        // Actualiza el título visual de la tarjeta en la parte superior
        tvTituloTarjeta.text = tituloOriginal

        // Lógica de los botones
        btnAdjuntar.setOnClickListener {
            mostrarMensaje("Funcionalidad para subir el PDF aún esta desarrollandose")
        }

        btnAnalizar.setOnClickListener {
            // Coge el texto y lo almacena quitando espacios sobrantes
            val contenidoTexto = etContenido.text.toString().trim()
            val tituloTexto = etTitulo.text.toString().trim()

            // Comprueba si esta vacio el edit text de los apuntes, título y contenido
            if (tituloTexto.isEmpty()) {
                etTitulo.error = "El título es obligatorio"
                mostrarMensaje("Falta el título")
            } else if (contenidoTexto.isEmpty()) {
                // Marca un error visual en la caja si no hay texto y avisa al usuario
                etContenido.error = "Debes pegar el texto de la asignatura aquí"
                mostrarMensaje("El campo de texto está vacío")
            // Ha funcionado correctamente habiendo detectado que había texto
            } else {
                mostrarMensaje("Actualizando apuntes en la nube...")

                // Bloquea el botón para evitar ser pulsado mientras carga
                btnAnalizar.isEnabled = false
                // Indica visualmente al usuario que se esta guardando
                btnAnalizar.text = "Guardando..."

                lifecycleScope.launch {
                    try {
                        // Lógica para actualizar el contenido en la tabla "asignaturas"
                        SupabaseClient.client
                            .from("asignaturas")
                            .update(
                                {
                                    // Que columna cambia y con que valor
                                    set("contenido", contenidoTexto)
                                    set("titulo", tituloTexto)
                                }
                            ) {
                                // Filtra para cambiar solo el que tenga este ID
                                filter {
                                    eq("id", idRecibido)
                                }
                            }

                        mostrarMensaje("¡Asignatura actualizada!")

                        volverALaLista()

                    } catch (e: Exception) {
                        // Si falla el internet, muestra el error y permite al usuario reintentarlo
                        e.printStackTrace()
                        mostrarMensaje("Error al actualizar: ${e.message}")

                        // Vuelve a activar el botón
                        btnAnalizar.isEnabled = true
                        // Restaura el texto original del botón
                        btnAnalizar.text = "Guardar Cambios"
                    }
                }
            }
        }

        // Lógica del botón eliminar asignatura
        btnEliminar.setOnClickListener {
            confirmarEliminacion(idRecibido)
        }

        // Ilumina el icono de "Asignatura" indicando que estamos aquí
        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)

    }

    // Abre una ventana de confirmación para que el usuario afirme si esta seguro
    private fun confirmarEliminacion(id: Long) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Eliminar asignatura?")
        builder.setMessage("Esta acción borrará la asignatura permanentemente. ¿Estás seguro?")

        // En caso de elegir eliminar, borra la asignatura de Supabase y se cierra el diálogo
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            borrarAsignaturaDeSupabase(id)
            dialog.dismiss()
        }

        // En caos de elegir cancelar, cierra el diálogo
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()

        // Pone el botón de eliminar de color rojo
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
    }

    // Conexión con supabase para borrar la asignatura
    private fun borrarAsignaturaDeSupabase(id: Long) {
        val btnEliminar = findViewById<MaterialButton>(R.id.btnEliminar)
        // Se desactiva el botón para evitar presionarlo más veces
        btnEliminar.isEnabled = false
        // Establece el texto del botón
        btnEliminar.text = "Borrando..."

        lifecycleScope.launch {
            try {
                // Lo elimina en Supabase
                SupabaseClient.client
                    .from("asignaturas")
                    .delete {
                        filter {
                            // Filtra y solo borra este ID
                            eq("id", id)
                        }
                    }

                mostrarMensaje("Asignatura eliminada")
                volverALaLista()

            } catch (e: Exception) {
                mostrarMensaje("Error al eliminar: ${e.message}")
                btnEliminar.isEnabled = true
                btnEliminar.text = "Eliminar Asignatura"
            }
        }
    }

    // Ayuda a volver a la lista evitando errores
    private fun volverALaLista() {
        // Prepara la navegación a la pantalla principal de asignatura
        val intent = Intent(this, AsignaturaActivity::class.java)
        // Elimina el historial de navegación y refresca la pantalla principal
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        // Inicia la actividad, recargando los datos
        startActivity(intent)

        // Elimina la animación para que sea más fluido
        overridePendingTransition(0, 0)

        // Termina la actividad actual para ahorrar memoria
        finish()
    }

    // Función para mostrar los mensajes y no tener que escribir Toast.makeText constantemente
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}