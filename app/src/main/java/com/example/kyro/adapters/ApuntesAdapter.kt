package com.example.kyro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kyro.entities.Asignatura
import com.example.kyro.R

// Conecta la lista de datos con el diseño visual en cada fila
class ApuntesAdapter(
    // Lista de asignaturas
    private val lista: List<Asignatura>,
    // Al pulsar una tarjeta
    private val onClick: (Asignatura) -> Unit
) : RecyclerView.Adapter<ApuntesAdapter.ViewHolder>() {

    // Guarda la referencia de los elementos visuales para no buscarlos continuamente
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloAsignatura)
    }

    // Crea una copia de "item_asignatura.xml" cuando hace falta nuevas filas en la pantalla
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asignatura, parent, false)
        return ViewHolder(view)
    }

    // Rellena con datos reales la tarjeta
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Coge el apunte correspondiente
        val apunte = lista[position]

        // Pone el texto del asignatura en la tarjeta
        holder.tvTitulo.text = if (apunte.titulo.isNotEmpty()) apunte.titulo else "Sin Título"

        // Avisa a la Activity principal si el usuario toca la tarjeta
        holder.itemView.setOnClickListener { onClick(apunte) }
    }

    // Dice a la lista la cantidad de elementos
    override fun getItemCount() = lista.size
}