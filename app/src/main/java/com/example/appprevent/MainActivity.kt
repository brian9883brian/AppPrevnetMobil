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
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var rvDatos: RecyclerView
    private lateinit var adapter: DatoAdapter
    private val datosRecibidos = mutableListOf<String>()

    private lateinit var api: ApiService        // API local
    private lateinit var apiNube: ApiServiceNube // API nube

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar RecyclerView
        rvDatos = findViewById(R.id.rvDatos)
        adapter = DatoAdapter(datosRecibidos)
        rvDatos.layoutManager = LinearLayoutManager(this)
        rvDatos.adapter = adapter

        // Retrofit para API local
        val retrofitLocal = Retrofit.Builder()
            .baseUrl("http://192.168.137.240:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofitLocal.create(ApiService::class.java)

        // Retrofit para API nube
        val retrofitNube = Retrofit.Builder()
            .baseUrl("http://192.168.137.240:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiNube = retrofitNube.create(ApiServiceNube::class.java)

        // Registrar listener para recibir mensajes Wear OS
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
        }

        if (mensaje == "Posible caída detectada") {
            enviarTodosLosRegistros()
        } else {
            val dato = DatoRequest(mensaje)
            api.enviarDato(dato).enqueue(object : Callback<DatoRequest> {
                override fun onResponse(call: Call<DatoRequest>, response: Response<DatoRequest>) {
                    Log.d("API Local", "Dato enviado correctamente: ${response.body()?.mensaje}")
                }

                override fun onFailure(call: Call<DatoRequest>, t: Throwable) {
                    Log.e("API Local", "Error al enviar dato", t)
                }
            })
        }
    }

    private fun enviarTodosLosRegistros() {
        apiNube.enviarTodosRegistros().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("API Nube", "Todos los registros enviados correctamente")
                } else {
                    Log.e("API Nube", "Error al enviar todos los registros: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API Nube", "Fallo al enviar todos los registros", t)
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
