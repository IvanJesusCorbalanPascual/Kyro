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
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TemarioActivity : AppCompatActivity() {

    // Variables globales, todas las funciones pueden acceder a ellas
    private lateinit var etNuevoTemario: EditText
    private lateinit var btnGenerar: MaterialButton
    private lateinit var rvTemarios: RecyclerView
    private lateinit var barraProgreso: ProgressBar
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temario)

        // Variables vinculadas con los IDs del XML
        etNuevoTemario = findViewById(R.id.etNuevoTemario)
        btnGenerar = findViewById(R.id.btnGenerar)
        rvTemarios = findViewById(R.id.rvTemarios)
        tvVacio = findViewById(R.id.tvVacio)
        barraProgreso = findViewById(R.id.barraProgreso)

        // Configurar menú inferior
        setupBottomNavigation()

        // Carga la lista al entrar
        cargarTemarios()

        // Configura el botón que permite cargar nuevos temas
        setupGenerarButton()
    }

    // Carga los datos desde Supabase
    private fun cargarTemarios() {
        // Muestra la rueda de carga y oculta el mensaje vacio
        barraProgreso.visibility = View.VISIBLE
        tvVacio.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Pide los datos de la tabla "apuntes_usuario"
                val listaApuntes = SupabaseClient.client
                    .from("apuntes_usuario")
                    .select()
                    // Convierte a objetos Kotlin el JSON
                    .decodeList<ApunteUsuario>()

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
            } catch (e: Exception) {
                // Si falla por falta de internet, etc, quita la carga y avisa del error
                barraProgreso.visibility = View.GONE
                Log.e("TemarioActivity", "Error al cargar" , e)
                Toast.makeText(this@TemarioActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Configura el botón para generar ejercicios
    private fun setupGenerarButton() {
        btnGenerar.setOnClickListener {
            val texto = etNuevoTemario.text.toString().trim()

            // Si no hay texto, avisa al usuario
            if (texto.isEmpty()) {
                etNuevoTemario.error = "Escribe algo primero"
            } else {
                // Si hay texto, llama a la función para subirlo
                subirNuevoTemario(texto)
            }
        }
    }

    // Función que envia los datos a la nube
    private fun subirNuevoTemario(contenido: String) {
        // Bloquea el botón para no pulsarse más veces
        btnGenerar.isEnabled = false
        btnGenerar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                // Crea el objeto que se debe subir
                val nuevoApunte = ApunteUsuario(contenido = contenido)

                // Lo inserta en Supabase
                SupabaseClient.client
                    .from("apuntes_usuario")
                    .insert(nuevoApunte)

                // Si no hay problemas, muestra al usuario que se ha guardado correctamente
                Toast.makeText(this@TemarioActivity, "¡Guardado!", Toast.LENGTH_SHORT).show()
                // Borra el campo con el texto
                etNuevoTemario.text.clear()

                // Espera medio segundo para que la BD pueda procesar el dato
                delay(500)
                // Recarga la lista para que se actualice sola
                cargarTemarios()

            } catch (e: Exception) {
                Log.e("TemarioActivity", "Error al subir", e)
                Toast.makeText(this@TemarioActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
            } finally {
                // Siempre, haya error o no, reactiva el botón y el texto del botón
                btnGenerar.isEnabled = true
                btnGenerar.text = "Generar Ejercicios"
            }
        }
    }

    // Función que permite ir a la vista detallada pasando datos
    private fun abrirDetalle(apunte: ApunteUsuario) {
        val intent = Intent(this, TemarioSeleccionadoActivity::class.java)

        // Mete el contenido en el intent
        intent.putExtra("EXTRA_CONTENIDO", apunte.contenido)
        intent.putExtra("EXTRA_ID", apunte.id)

        startActivity(intent)
    }

    // Navegación de la parte inferior de la aplicación
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return
        // Marca "Temario" como seleccionado
        bottomNav.selectedItemId = R.id.nav_syllabus

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Navegación a Home
                R.id.nav_home -> {
                        startActivity(Intent(this, MainActivity::class.java))
                        true
                }
                // Ya estamos en Temario
                R.id.nav_syllabus -> true

                // Navegación a calendario
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarioActivity::class.java))
                    true
                }

                // Navegación a ajustes
                R.id.nav_settings -> {
                startActivity(Intent(this, AjustesActivity::class.java))
                true
            }
                else -> false
            }
        }
    }
}
