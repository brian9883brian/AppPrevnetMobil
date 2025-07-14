package com.example.appprevent

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoricoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        val contenedorA = findViewById<LinearLayout>(R.id.contenedorSensorA)
        val contenedorB = findViewById<LinearLayout>(R.id.contenedorSensorB)
        val contenedorC = findViewById<LinearLayout>(R.id.contenedorSensorC)
        val inflater = LayoutInflater.from(this)

        // Sensor A - ejemplo con dos tarjetas
        val cardA1 = inflater.inflate(R.layout.item_card_historico, contenedorA, false)
        cardA1.findViewById<TextView>(R.id.tvTituloSensor).text = "Sensor A"
        cardA1.findViewById<TextView>(R.id.tvFecha).text = "2025-07-01"
        cardA1.findViewById<TextView>(R.id.tvValorX).text = "100"
        cardA1.findViewById<TextView>(R.id.tvValorY).text = "200"
        cardA1.findViewById<TextView>(R.id.tvValorZ).text = "300"
        contenedorA.addView(cardA1)

        val cardA2 = inflater.inflate(R.layout.item_card_historico, contenedorA, false)
        cardA2.findViewById<TextView>(R.id.tvTituloSensor).text = "Sensor A"
        cardA2.findViewById<TextView>(R.id.tvFecha).text = "2025-07-05"
        cardA2.findViewById<TextView>(R.id.tvValorX).text = "150"
        cardA2.findViewById<TextView>(R.id.tvValorY).text = "250"
        cardA2.findViewById<TextView>(R.id.tvValorZ).text = "350"
        contenedorA.addView(cardA2)

        // Sensor B - ejemplo con una tarjeta
        val cardB1 = inflater.inflate(R.layout.item_card_historico, contenedorB, false)
        cardB1.findViewById<TextView>(R.id.tvTituloSensor).text = "Sensor B"
        cardB1.findViewById<TextView>(R.id.tvFecha).text = "2025-07-02"
        cardB1.findViewById<TextView>(R.id.tvValorX).text = "10"
        cardB1.findViewById<TextView>(R.id.tvValorY).text = "20"
        cardB1.findViewById<TextView>(R.id.tvValorZ).text = "30"
        contenedorB.addView(cardB1)

        // Sensor C - ejemplo con dos tarjetas
        val cardC1 = inflater.inflate(R.layout.item_card_historico, contenedorC, false)
        cardC1.findViewById<TextView>(R.id.tvTituloSensor).text = "Sensor C"
        cardC1.findViewById<TextView>(R.id.tvFecha).text = "2025-07-03"
        cardC1.findViewById<TextView>(R.id.tvValorX).text = "999"
        cardC1.findViewById<TextView>(R.id.tvValorY).text = "888"
        cardC1.findViewById<TextView>(R.id.tvValorZ).text = "777"
        contenedorC.addView(cardC1)

        val cardC2 = inflater.inflate(R.layout.item_card_historico, contenedorC, false)
        cardC2.findViewById<TextView>(R.id.tvTituloSensor).text = "Sensor C"
        cardC2.findViewById<TextView>(R.id.tvFecha).text = "2025-07-06"
        cardC2.findViewById<TextView>(R.id.tvValorX).text = "111"
        cardC2.findViewById<TextView>(R.id.tvValorY).text = "222"
        cardC2.findViewById<TextView>(R.id.tvValorZ).text = "333"
        contenedorC.addView(cardC2)
    }
}
