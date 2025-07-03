package com.example.appprevent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoricoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        val contenedor = findViewById<LinearLayout>(R.id.contenedorHistorial)

        // Simulamos un único dato
        val fecha = "02/07/2025"
        val dato = "Presión: 130/85"

        // Inflamos la tarjeta desde item_card_historico.xml
        val card = LayoutInflater.from(this).inflate(R.layout.item_card_historico, contenedor, false)

        // Asignamos texto
        card.findViewById<TextView>(R.id.tvFecha).text = fecha
        card.findViewById<TextView>(R.id.tvDato).text = dato

        // Aplicamos animación
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
        card.startAnimation(anim)

        // Agregamos la tarjeta al contenedor
        contenedor.addView(card)
    }
}