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

class AsignaturaActivity : AppCompatActivity() {

    // Variables globales, todas las funciones pueden acceder a ellas
    private lateinit var etTituloNuevo: EditText
    private lateinit var etContenidoNuevo: EditText
    private lateinit var btnGenerar: MaterialButton
    private lateinit var rvAsignaturas: RecyclerView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var btnAdjuntar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignatura)

        // Variables vinculadas con los IDs del XML
        etTituloNuevo = findViewById(R.id.etTituloNuevoAsignatura)
        etContenidoNuevo = findViewById(R.id.etContenidoNuevoAsignatura)
        btnGenerar = findViewById(R.id.btnGenerar)
        rvAsignaturas = findViewById(R.id.rvAsignaturas)
        tvVacio = findViewById(R.id.tvVacio)
        barraProgreso = findViewById(R.id.barraProgreso)

        val btnAdjuntarClick = findViewById<View>(R.id.btnAdjuntarArchivo)

        btnAdjuntarClick.setOnClickListener {
            showKyroToast("Funcionalidad de adjuntar PDF esta en desarrollo")
        }

        // Carga la lista al entrar
        cargarAsignaturas()

        // Configura el botón que permite cargar nuevos temas
        setupGenerarButton()
    }

    override fun onResume() {
        super.onResume()
        // Llama al Helper y le dice que ilumine asignatura
        NavigationHelper.setupBottomNavigation(this, R.id.nav_asignatura)
    }

    // Carga los datos desde Supabase filtrando por usuario
    private fun cargarAsignaturas() {
        // Muestra la rueda de carga y oculta el mensaje vacio
        barraProgreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Obtiene el usuario conectado en la sesion actual
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                // Pide los datos de la tabla "asignaturas" del usuario actual
                if (usuarioActual != null) {
                    // Pide las asignaturas aplicando el filtro de usuario
                    val listaAsignaturas = SupabaseClient.client
                        .from("asignaturas")
                        .select {
                            // Solo trae el user_id que es igual al id
                            filter {
                                eq("user_id", usuarioActual.id)
                            }
                        }
                        // Convierte a objetos Kotlin el JSON
                        .decodeList<Asignatura>()
                        // Lo más nuevo aparece primero
                        .sortedByDescending { it.id }

                    // Oculta la carga al terminar
                    barraProgreso.visibility = View.GONE

                    // Comprueba si esta vacia o llena la lista
                    if (listaAsignaturas.isEmpty()) {
                        // Muestra el aviso de que no hay temas, si esta vacio
                        tvVacio.visibility = View.VISIBLE
                        rvAsignaturas.visibility = View.GONE
                    } else {
                        tvVacio.visibility = View.GONE
                        rvAsignaturas.visibility = View.VISIBLE

                        // Configura el layout del RecyclerView
                        rvAsignaturas.layoutManager = LinearLayoutManager(this@AsignaturaActivity)

                        // Conecta el adaptador con la lista de datos
                        rvAsignaturas.adapter = AsignaturaAdapter(listaAsignaturas) { asignatura ->
                            // Se ejecuta al pulsar una tarjeta, abriendo el detalle
                            abrirDetalle(asignatura)
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
                Log.e("AsignaturaActivity", "Error al cargar" , e)
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
            subirNuevaAsignaturaYGenerar(titulo, contenido)
        }
    }

    // Función que envia los datos a la nube
    private fun subirNuevaAsignaturaYGenerar(titulo: String, contenido: String) {
        btnGenerar.isEnabled = false
        btnGenerar.text = "Guardando y Generando..."

        lifecycleScope.launch {
            try {
                val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()

                if (usuarioActual == null) {
                    showKyroToast("Error, La sesión no es válida")
                    return@launch
                }

                // 1. CREAMOS LA ASIGNATURA (Esto sigue igual)
                val nuevaAsignatura = Asignatura(
                    titulo = titulo,
                    contenido = contenido,
                    user_id = usuarioActual.id
                )

                // Guardamos la asignatura y RECUPERAMOS SU ID (select())
                val asignaturaGuardada = SupabaseClient.client
                    .from("asignaturas")
                    .insert(nuevaAsignatura) {
                        select()
                    }.decodeSingle<Asignatura>()

                showKyroToast("¡Asignatura guardada! Consultando a la IA...")

                // Limpiamos campos
                etTituloNuevo.text.clear()
                etContenidoNuevo.text.clear()
                etTituloNuevo.clearFocus()
                etContenidoNuevo.clearFocus()

                // 2. GENERAMOS EL TEST CON IA (Esto sigue igual, la IA ya funciona bien)
                val servicioIA = GeminiService()
                val preguntasGeneradas = servicioIA.generarTestDeApuntes(contenido)

                if (preguntasGeneradas.isNotEmpty()) {

                    val gson = com.google.gson.Gson()
                    val preguntasJson = gson.toJson(preguntasGeneradas)

                    // --- CAMBIO IMPORTANTE: AHORA GUARDAMOS EN LA TABLA 'ejercicios' ---

                    // Creamos el objeto para la nueva tabla
                    val nuevoEjercicio = EjercicioIA(
                        asignaturaId = asignaturaGuardada.id, // Aquí vinculamos con la asignatura que acabamos de crear
                        nombre = "Test Generado con IA",
                        preguntasJson = preguntasJson
                    )

                    // Insertamos en 'ejercicios' en lugar de hacer update en 'asignaturas'
                    SupabaseClient.client
                        .from("ejercicios")
                        .insert(nuevoEjercicio)

                    // ------------------------------------------------------------------

                    // Vamos al juego
                    val intent = Intent(this@AsignaturaActivity, QuizActivity::class.java)
                    intent.putExtra("EXTRA_PREGUNTAS", ArrayList(preguntasGeneradas))
                    startActivity(intent)

                } else {
                    showKyroToast("La IA no pudo generar preguntas. Revisa el texto.")
                }

                // Recargamos la lista
                delay(500)
                cargarAsignaturas()

            } catch (e: Exception) {
                Log.e("AsignaturaActivity", "Error al subir", e)
                showKyroToast("Error: ${e.message}")
            } finally {
                btnGenerar.isEnabled = true
                btnGenerar.text = "Generar Ejercicios con IA ✨"
            }
        }
    }

    // Función que permite ir a la vista detallada pasando datos
    private fun abrirDetalle(asignatura: Asignatura) {
        val intent = Intent(this, AsignaturaSeleccionadaActivity::class.java)

        // Mete el contenido en el intent antes de enviarlo a la siguiente pantalla (AsignaturaSeleccionadoActivity)
        intent.putExtra("EXTRA_TITULO", asignatura.titulo)
        intent.putExtra("EXTRA_CONTENIDO", asignatura.contenido)
        intent.putExtra("EXTRA_ID", asignatura.id)
        intent.putExtra("EXTRA_JSON_PREGUNTAS", asignatura.preguntas_json)

        startActivity(intent)
    }
}