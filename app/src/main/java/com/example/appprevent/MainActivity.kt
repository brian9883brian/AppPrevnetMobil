package com.example.appprevent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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

    private lateinit var api: ApiService // Solo API local

    // TextView para mostrar nombre usuario
    private lateinit var tvNombreUsuario: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar y mostrar nombre guardado en SharedPreferences
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val nombre = prefs.getString("usuario_nombre", "Usuario")
        tvNombreUsuario.text = "Hola, $nombre"

        // Inicializar RecyclerView
        rvDatos = findViewById(R.id.rvDatos)
        adapter = DatoAdapter(datosRecibidos)
        rvDatos.layoutManager = LinearLayoutManager(this)
        rvDatos.adapter = adapter

        // Retrofit para API local
        val retrofitLocal = Retrofit.Builder()
            .baseUrl("http://192.168.128.1:8000/") // Cambia según tu API
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofitLocal.create(ApiService::class.java)

        // Registrar listener para recibir mensajes Wear OS
        Wearable.getMessageClient(this).addListener(this)

        // Menú lateral
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0) // Obtiene el primer header
        val tvNombreUsuarioHeader = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
        tvNombreUsuarioHeader.text = nombre

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.nav_vista1 -> {
                    startActivity(Intent(this, HistoricoActivity::class.java))
                }
                R.id.nav_vista2 -> {
                    startActivity(Intent(this, UserDetailActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        // Enviar broadcast para que HistoricoActivity lo procese
        val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
        intent.putExtra("mensaje", mensaje)
        sendBroadcast(intent)

        // Actualizar recyclerView en MainActivity
        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
        }

        // Enviar dato a API local
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

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
