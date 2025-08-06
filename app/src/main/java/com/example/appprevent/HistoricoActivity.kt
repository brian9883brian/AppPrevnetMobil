package com.example.appprevent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class HistoricoActivity : AppCompatActivity() {

    private lateinit var contenedorA: LinearLayout
    private lateinit var contenedorB: LinearLayout
    private lateinit var contenedorC: LinearLayout
    private lateinit var inflater: LayoutInflater

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mensaje = intent?.getStringExtra("mensaje") ?: return
            runOnUiThread {
                agregarNuevoDato(mensaje)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        contenedorA = findViewById(R.id.contenedorSensorA)
        contenedorB = findViewById(R.id.contenedorSensorB)
        contenedorC = findViewById(R.id.contenedorSensorC)
        inflater = LayoutInflater.from(this)

        // Datos de ejemplo (opcional)
        // agregarNuevoDato("Velocidad alta detectada")
        // agregarNuevoDato("Mensaje de caída recibido")

        registerReceiver(broadcastReceiver, IntentFilter("com.example.appprevent.ACTUALIZAR_HISTORICO"))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun agregarNuevoDato(mensaje: String) {
        val contenedor = when {
            mensaje.contains("Velocidad", ignoreCase = true) -> contenedorA
            mensaje.contains("Caída", ignoreCase = true) -> contenedorB
            else -> contenedorC
        }

        val card = inflater.inflate(R.layout.item_card_historico, contenedor, false)
        card.findViewById<TextView>(R.id.tvTituloSensor).text = when (contenedor) {
            contenedorA -> "Sensor A"
            contenedorB -> "Sensor B"
            else -> "Sensor C"
        }
        card.findViewById<TextView>(R.id.tvFecha).text = obtenerFechaActual()
        card.findViewById<TextView>(R.id.tvValorX).text = mensaje
        card.findViewById<TextView>(R.id.tvValorY).text = "-"
        card.findViewById<TextView>(R.id.tvValorZ).text = "-"
        contenedor.addView(card, 0) // Agrega al inicio
    }

    private fun obtenerFechaActual(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
