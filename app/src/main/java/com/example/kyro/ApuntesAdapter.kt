package com.example.kyro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Conecta la lista de datos con el diseño visual en cada fila
class ApuntesAdapter(
    // Lista de temarios
    private val lista: List<ApunteUsuario>,
    // Al pulsar una tarjeta
    private val onClick: (ApunteUsuario) -> Unit
) : RecyclerView.Adapter<ApuntesAdapter.ViewHolder>() {

    // Guarda la referencia de los elementos visuales para no buscarlos continuamente
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloTema)
    }

    // Crea una copia de "item_temario.xml" cuando hace falta nuevas filas en la pantalla
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_temario, parent, false)
        return ViewHolder(view)
    }

    // Rellena con datos reales la tarjeta
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Coge el apunte correspondiente
        val apunte = lista[position]

        // Pone el texto del temario en la tarjeta
        holder.tvTitulo.text = apunte.contenido

        // Avisa a la Activity principal si el usuario toca la tarjeta
        holder.itemView.setOnClickListener { onClick(apunte) }
    }

    // Dice a la lista la cantidad de elementos
    override fun getItemCount() = lista.size
}