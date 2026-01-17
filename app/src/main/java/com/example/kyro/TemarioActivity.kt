package com.example.kyro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayList // Necesario para pasar la lista

class TemarioActivity : AppCompatActivity() {

    // Variables globales, todas las funciones pueden acceder a ellas
    private lateinit var etTituloNuevo: EditText
    private lateinit var etContenidoNuevo: EditText
    private lateinit var btnGenerar: MaterialButton
    private lateinit var rvTemarios: RecyclerView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var btnAdjuntar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario)

        // Variables vinculadas con los IDs del XML
        etTituloNuevo = findViewById(R.id.etTituloNuevoTemario)
        etContenidoNuevo = findViewById(R.id.etContenidoNuevoTemario)
        btnGenerar = findViewById(R.id.btnGenerar)
        rvTemarios = findViewById(R.id.rvTemarios)
        tvVacio = findViewById(R.id.tvVacio)
        barraProgreso = findViewById(R.id.barraProgreso)

        val btnAdjuntarClick = findViewById<View>(R.id.btnAdjuntarArchivo)

        btnAdjuntarClick.setOnClickListener {
            showKyroToast("Funcionalidad de adjuntar PDF esta en desarrollo")
        }

        // Carga la lista al entrar
        cargarTemarios()

        // Configura el botón que permite cargar nuevos temas
        setupGenerarButton()
    }

    override fun onResume() {
        super.onResume()
        // Llama al Helper y le dice que ilumine temario
        NavigationHelper.setupBottomNavigation(this, R.id.nav_syllabus)
    }

    // Carga los datos desde Supabase filtrando por usuario
    private fun cargarTemarios() {
        // Muestra la rueda de carga y oculta el mensaje vacio
        barraProgreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Obtiene el usuario conectado en la sesion actual
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                // Pide los datos de la tabla "apuntes_usuario" del usuario actual
                if (usuarioActual != null) {
                    // Pide los apuntes aplicando el filtro de usuario
                    val listaApuntes = SupabaseClient.client
                        .from("apuntes_usuario")
                        .select {
                            // Solo trae el user_id que es igual al id
                            filter {
                                eq("user_id", usuarioActual.id)
                            }
                        }
                        // Convierte a objetos Kotlin el JSON
                        .decodeList<ApunteUsuario>()
                        // Lo más nuevo aparece primero
                        .sortedByDescending { it.id }

                    // Oculta la carga al terminar
                    barraProgreso.visibility = View.GONE

                    // Comprueba si esta vacia o llena la lista
                    if (listaApuntes.isEmpty()) {
                        // Muestra el aviso de que no hay temas, si esta vacio
                        tvVacio.visibility = View.VISIBLE
                        rvTemarios.visibility = View.GONE
                    } else {
                        tvVacio.visibility = View.GONE
                        rvTemarios.visibility = View.VISIBLE

                        // Configura el layout del RecyclerView
                        rvTemarios.layoutManager = LinearLayoutManager(this@TemarioActivity)

                        // Conecta el adaptador con la lista de datos
                        rvTemarios.adapter = ApuntesAdapter(listaApuntes) { apunte ->
                            // Se ejecuta al pulsar una tarjeta, abriendo el detalle
                            abrirDetalle(apunte)
                        }
                    }
                } else {
                    // En el caso improbable de que no haya usuario logeado, oculta
                    barraProgreso.visibility = View.GONE
                    tvVacio.text = "Error de sesión."
                    tvVacio.visibility = View.VISIBLE
                }

            }catch (e: Exception) {
                // Si falla por falta de internet, etc, quita la carga y avisa del error
                barraProgreso.visibility = View.GONE
                Log.e("TemarioActivity", "Error al cargar" , e)
                showKyroToast("Error de conexión")
            }
        }
    }

    // Configura el botón para generar ejercicios
    private fun setupGenerarButton() {
        btnGenerar.setOnClickListener {
            val titulo = etTituloNuevo.text.toString().trim()
            val contenido = etContenidoNuevo.text.toString().trim()

            // Si no hay texto, avisa al usuario
            if (titulo.isEmpty()) {
                etTituloNuevo.error = "Escribe el título, es obligatorio"
                return@setOnClickListener
            }

            // Estableciendo un minimo de 50 caracteres para evitar fallos o respuestas muy pobres
            if (contenido.isEmpty() || contenido.length < 50) {
                etContenidoNuevo.error = "Escribe al menos 50 caracteres para generar ejercicios"
                Toast.makeText(this, "El contenido es muy corto para la IA", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Si están ambos datos, llama a la función para subirlo y procesarlo con IA
            subirNuevoTemarioYGenerar(titulo, contenido)
        }
    }

    // Función que envia los datos a la nube
    private fun subirNuevoTemarioYGenerar(titulo: String, contenido: String) {
        // Bloquea el botón para no pulsarse más veces
        btnGenerar.isEnabled = false
        btnGenerar.text = "Guardando y Generando..."

        lifecycleScope.launch {
            try {
                // Identifica el usuario actual en la sesión
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                // Si no hay usuario por alguna razón, corta sesión para evitar errores
                if (usuarioActual == null) {
                    Toast.makeText(this@TemarioActivity, "Error, La sesión no es valida", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Crea el objeto que se debe subir pasandole el id del usuarios
                val nuevoApunte = ApunteUsuario(titulo = titulo, contenido = contenido, user_id = usuarioActual.id)

                // Lo inserta en Supabase (y recuperamos el objeto guardado para tener su ID)
                val apunteGuardado = SupabaseClient.client
                    .from("apuntes_usuario")
                    .insert(nuevoApunte) {
                        select() // Importante: esto nos devuelve el ID generado
                    }.decodeSingle<ApunteUsuario>()

                // Si no hay problemas, muestra al usuario que se ha guardado correctamente
                Toast.makeText(this@TemarioActivity, "¡Guardado! Consultando a la IA...", Toast.LENGTH_SHORT).show()

                // Limpia ambos campos
                etTituloNuevo.text.clear()
                etContenidoNuevo.text.clear()

                // Quita el foco, hace que baje el teclado
                etTituloNuevo.clearFocus()
                etContenidoNuevo.clearFocus()

                // --- LÓGICA DE IA ---
                val servicioIA = GeminiService()
                val preguntasGeneradas = servicioIA.generarTestDeApuntes(contenido)

                if (preguntasGeneradas.isNotEmpty()) {

                    // Guardamos las preguntas en Supabase para que no se pierdan
                    val gson = com.google.gson.Gson()
                    val preguntasJson = gson.toJson(preguntasGeneradas)

                    SupabaseClient.client
                        .from("apuntes_usuario")
                        .update({
                            set("preguntas_json", preguntasJson)
                        }) {
                            filter {
                                eq("id", apunteGuardado.id)
                            }
                        }

                    // Si la IA funcionó, vamos directos al Quiz
                    val intent = Intent(this@TemarioActivity, QuizActivity::class.java)
                    intent.putExtra("EXTRA_PREGUNTAS", ArrayList(preguntasGeneradas))
                    startActivity(intent)
                } else {
                    showKyroToast("La IA no pudo generar preguntas. Revisa el texto.")
                }

                // Espera medio segundo para que la BD pueda procesar los datos
                delay(500)
                // Recarga la lista para que se actualice sola
                cargarTemarios()

            } catch (e: Exception) {
                Log.e("TemarioActivity", "Error al subir", e)
               showKyroToast("Error al guardar o generar")
            } finally {
                // Siempre, haya error o no, reactiva el botón y el texto del botón
                btnGenerar.isEnabled = true
                btnGenerar.text = "Generar Ejercicios con IA ✨"
            }
        }
    }

    // Función que permite ir a la vista detallada pasando datos
    private fun abrirDetalle(apunte: ApunteUsuario) {
        val intent = Intent(this, TemarioSeleccionadoActivity::class.java)

        // Mete el contenido en el intent antes de enviarlo a la siguiente pantalla (TemarioSeleccionadoActivity)
        intent.putExtra("EXTRA_TITULO", apunte.titulo)
        intent.putExtra("EXTRA_CONTENIDO", apunte.contenido)
        intent.putExtra("EXTRA_ID", apunte.id)
        intent.putExtra("EXTRA_JSON_PREGUNTAS", apunte.preguntas_json)

        startActivity(intent)
    }
}