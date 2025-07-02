package com.example.appprevent

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var rvDatos: RecyclerView
    private lateinit var adapter: DatoAdapter
    private val datosRecibidos = mutableListOf<String>()
    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar RecyclerView
        rvDatos = findViewById(R.id.rvDatos)
        adapter = DatoAdapter(datosRecibidos)
        rvDatos.layoutManager = LinearLayoutManager(this)
        rvDatos.adapter = adapter

        // Inicializar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.137.166:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)

        // Registrar listener
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
        }

        // Enviar mensaje al microservicio (nivel niebla)
        val dato = DatoRequest(mensaje)
        api.enviarDato(dato).enqueue(object : Callback<DatoRequest> {
            override fun onResponse(call: Call<DatoRequest>, response: Response<DatoRequest>) {
                Log.d("API", "Dato enviado correctamente: ${response.body()?.mensaje}")
            }

            override fun onFailure(call: Call<DatoRequest>, t: Throwable) {
                Log.e("API", "Error al enviar dato", t)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
