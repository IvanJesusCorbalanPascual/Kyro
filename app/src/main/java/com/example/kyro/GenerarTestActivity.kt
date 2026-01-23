package com.example.kyro

import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clase que se encarga de recolectar los archivos seleccionados por el usuario, pasarselo a la clase GeminiService
 * para que esta genere el prompt y se lo pase a la IA para que esta genera las preguntas tipo test.
 * Luego recoge estas preguntas y genera el tipo test con una vista didactica para el usuario.
 */
class GenerarTestActivity : AppCompatActivity() {

    private var asignaturaId: Long = -1
    private var contenidoBaseAsignatura: String = ""

    // UI Elements
    private lateinit var etNombreTest: TextInputEditText
    private lateinit var tilNombreTest: TextInputLayout
    private lateinit var sliderPreguntas: Slider
    private lateinit var tvLabelPreguntas: TextView
    private lateinit var chipGroupDificultad: ChipGroup
    private lateinit var rvArchivos: RecyclerView
    private lateinit var btnGenerar: MaterialButton
    private lateinit var cbContenidoBase: CheckBox

    private lateinit var adapter: SeleccionArchivosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generar_test)

        asignaturaId = intent.getLongExtra("ASIGNATURA_ID", -1)
        contenidoBaseAsignatura = intent.getStringExtra("CONTENIDO_BASE") ?: ""

        initViews()
        setupListeners()
        cargarArchivosDeAsignatura()
    }

    private fun initViews() {
        etNombreTest = findViewById(R.id.etNombreTest)
        tilNombreTest = findViewById(R.id.tilNombreTest)
        sliderPreguntas = findViewById(R.id.sliderPreguntas)
        tvLabelPreguntas = findViewById(R.id.tvLabelPreguntas)
        chipGroupDificultad = findViewById(R.id.chipGroupDificultad)
        rvArchivos = findViewById(R.id.rvSeleccionArchivos)
        btnGenerar = findViewById(R.id.btnGenerarTestFinal)
        cbContenidoBase = findViewById(R.id.cbContenidoBase)

        rvArchivos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        // Actualizar etiqueta del slider al moverlo
        sliderPreguntas.addOnChangeListener { _, value, _ ->
            tvLabelPreguntas.text = "Número de preguntas: ${value.toInt()}"
        }

        btnGenerar.setOnClickListener {
            validarYGenerar()
        }
    }

    private fun cargarArchivosDeAsignatura() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val listaArchivos = SupabaseClient.client
                    .from("archivos")
                    .select { filter { eq("asignatura_id", asignaturaId) } }
                    .decodeList<Archivo>()

                withContext(Dispatchers.Main) {
                    if (listaArchivos.isNotEmpty()) {
                        adapter = SeleccionArchivosAdapter(listaArchivos)
                        rvArchivos.adapter = adapter
                    } else {
                        findViewById<TextView>(R.id.tvSinArchivos).visibility =
                            android.view.View.VISIBLE
                        rvArchivos.visibility = android.view.View.GONE
                        adapter = SeleccionArchivosAdapter(emptyList())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validarYGenerar() {
        val nombreTest = etNombreTest.text.toString().trim()
        val usarBase = cbContenidoBase.isChecked
        val archivosSeleccionados = adapter.seleccionados.toList()

        // Validar campos vacíos
        if (nombreTest.isEmpty()) {
            tilNombreTest.error = "Escribe un nombre para el test"
            return
        } else {
            tilNombreTest.error = null
        }

        if (!usarBase && archivosSeleccionados.isEmpty()) {
            showKyroToast("Selecciona al menos una fuente (apuntes o archivos)")
            return
        }

        btnGenerar.isEnabled = false
        btnGenerar.text = "Verificando nombre..."

        // Validar nombre duplicado en BD
        lifecycleScope.launch(Dispatchers.IO) {
            val existe = nombreYaExiste(nombreTest)

            withContext(Dispatchers.Main) {
                if (existe) {
                    tilNombreTest.error = "Ya existe un test con este nombre en esta asignatura"
                    btnGenerar.isEnabled = true
                    btnGenerar.text = "Generar Test ✨"
                } else {
                    // Si el nombre está libre, procedemos a generar
                    generarTestConIA(nombreTest, archivosSeleccionados, usarBase)
                }
            }
        }
    }

    // Consulta a Supabase si ya existe el nombre
    private suspend fun nombreYaExiste(nombre: String): Boolean {
        return try {
            val count = SupabaseClient.client
                .from("ejercicios")
                .select {
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                    filter {
                        eq("asignatura_id", asignaturaId)
                        eq("nombre", nombre) // Asumiendo que la columna en BD es 'nombre'
                    }
                }.countOrNull() ?: 0

            count > 0
        } catch (e: Exception) {
            false // Ante la duda, dejamos pasar (o podrías bloquear)
        }
    }

    private fun generarTestConIA(nombreTest: String, archivos: List<Archivo>, usarBase: Boolean) {
        val numPreguntas = sliderPreguntas.value.toInt()

        val dificultad = when (chipGroupDificultad.checkedChipId) {
            R.id.chipFacil -> "Fácil (principiante)"
            R.id.chipMedio -> "Media (estándar universitario)"
            R.id.chipDificil -> "Difícil (análisis complejo)"
            else -> "Media"
        }

        runOnUiThread { btnGenerar.text = "KyroIA está pensando... 🤖" }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Preparando el contenido de estudio con el que hacer las preguntas
                val contenidoBuilder = StringBuilder()

                if (usarBase && contenidoBaseAsignatura.isNotEmpty()) {
                    contenidoBuilder.append("APUNTES DE LA ASIGNATURA:\n$contenidoBaseAsignatura\n\n")
                }

                if (archivos.isNotEmpty()) {
                    contenidoBuilder.append("TEMAS ADICIONALES:\n")
                    archivos.forEach { archivo ->
                        // --- POR ARREGLAR ---
                        // OJO: Aquí solo estamos pasando el nombre del archivo.
                        // Si no tienes OCR o lectura de PDF, la IA inventará preguntas basándose solo en el título.
                        contenidoBuilder.append("- TEMA/ARCHIVO: ${archivo.nombre}\n")
                    }
                }

                // LLamar a GeminiService la cual se encargará de hacer la consulta con la IA
                val servicioIA = GeminiService()

                val preguntasGeneradas = servicioIA.generarTestDeApuntes(
                    textoApuntes = contenidoBuilder.toString(),
                    dificultad = dificultad,
                    cantidad = numPreguntas
                )

                if (preguntasGeneradas.isNotEmpty()) {
                    // Guardar en Supabase igual que antes
                    val gson = Gson()
                    val preguntasJson = gson.toJson(preguntasGeneradas)

                    val nuevoEjercicio = EjercicioIA(
                        asignatura_id = asignaturaId,
                        nombre = nombreTest,
                        preguntas_json = preguntasJson
                    )
                    SupabaseClient.client.from("ejercicios").insert(nuevoEjercicio)

                    withContext(Dispatchers.Main) {
                        showKyroToast("¡Test creado!")
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showKyroToast("La IA no generó preguntas válidas")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showKyroToast("Error: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnGenerar.isEnabled = true
                    btnGenerar.text = "Generar Test ✨"
                }
            }
        }
    }
}