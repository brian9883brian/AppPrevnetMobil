package com.example.appprevent

import java.net.URL
import java.net.HttpURLConnection
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson

// Clase para el payload JSON
data class EventoPayload(
    val evento: String,
    val latitud: Double,
    val longitud: Double
)

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var rvDatos: RecyclerView
    private lateinit var adapter: DatoAdapter
    private val datosRecibidos = mutableListOf<String>()
    private var guid: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var db: AppDatabase
    private lateinit var datoDao: DatoDao

    private lateinit var tvNombreUsuario: TextView

    private val bufferDatos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
        datoDao = db.datoDao()

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val nombre = prefs.getString("usuario_nombre", "Usuario") ?: "Usuario"

        guid = intent.getStringExtra("guid") ?: prefs.getString("usuario_guid", "") ?: ""

        with(prefs.edit()) {
            putString("usuario_guid", guid)
            apply()
        }

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvNombreUsuario.text = "Hola, $nombre"

        val tvGuid = findViewById<TextView?>(R.id.tvGuid)
        tvGuid?.text = "GUID: $guid"

        rvDatos = findViewById(R.id.rvDatos)
        adapter = DatoAdapter(datosRecibidos)
        rvDatos.layoutManager = LinearLayoutManager(this)
        rvDatos.adapter = adapter

        cargarDatosDesdeBD()

        val btnBano = findViewById<MaterialButton>(R.id.btnBano)
        val btnTerminar = findViewById<MaterialButton>(R.id.btnTerminar)
        val btnComida = findViewById<MaterialButton>(R.id.btnComida)

        btnBano.setOnClickListener { manejarClickBoton(btnBano.text.toString()) }
        btnTerminar.setOnClickListener { (btnTerminar.text.toString()) }
        btnComida.setOnClickListener { manejarClickBoton(btnComida.text.toString()) }

        Wearable.getMessageClient(this).addListener(this)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val tvNombreUsuarioHeader = headerView.findViewById<TextView>(R.id.tvNombreUsuario)
        tvNombreUsuarioHeader.text = nombre

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> startActivity(Intent(this, MainActivity::class.java))
                R.id.nav_vista1 -> startActivity(Intent(this, HistoricoActivity::class.java))
                R.id.nav_vista2 -> startActivity(Intent(this, UserDetailActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun manejarClickBoton(buttonText: String) {
        Toast.makeText(this, buttonText, Toast.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val ubicacion = if (location != null) {
                "Ubicación actual: lat=${location.latitude}, lon=${location.longitude}"
            } else {
                "Ubicación no disponible"
            }

            lifecycleScope.launch(Dispatchers.IO) {
                bufferDatos.add(buttonText)
                bufferDatos.add(ubicacion)

                val copiaBuffer = bufferDatos.toList()
                val bufferInsertExito = insertarBufferEnDB()

                var botonInsertExito = false
                try {
                    datoDao.insertarDato(DatoEntity(mensaje = buttonText))
                    botonInsertExito = true
                } catch (e: Exception) {
                    Log.e("DB", "Error al insertar dato botón: ${e.message}")
                }

                Log.d("API", "Buffer antes de enviar: $copiaBuffer")
                enviarDatosAlAPI(copiaBuffer)

                launch(Dispatchers.Main) {
                    if (bufferInsertExito && botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                        Log.d("DB", "Buffer y botón insertados correctamente")
                    } else if (!bufferInsertExito && !botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar datos (buffer y botón)", Toast.LENGTH_LONG).show()
                    } else if (!bufferInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar datos del buffer", Toast.LENGTH_LONG).show()
                    } else if (!botonInsertExito) {
                        Toast.makeText(this@MainActivity, "Error al guardar dato del botón", Toast.LENGTH_LONG).show()
                    }
                }
            }

            runOnUiThread {
                datosRecibidos.clear()
                datosRecibidos.add(buttonText)
                adapter.notifyDataSetChanged()
                rvDatos.scrollToPosition(datosRecibidos.size - 1)

                // 🔁 ENVÍA BROADCAST A HistoricoActivity
                val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
                intent.putExtra("mensaje", buttonText)
                sendBroadcast(intent)
            }

        }
    }

    private fun enviarDatosAlAPI(datos: List<String>) {
        Log.d("API", "Simulando envío de datos: $datos")

        if (datos.size < 2) {
            Log.e("API", "No hay suficientes datos para enviar")
            return
        }

        val evento = datos[0]
        val ubicacion = datos[1]

        // Extraer latitud y longitud de la cadena
        val latitudRegex = Regex("lat=([\\d.-]+)")
        val longitudRegex = Regex("lon=([\\d.-]+)")

        val lat = latitudRegex.find(ubicacion)?.groupValues?.get(1)?.toDoubleOrNull()
        val lon = longitudRegex.find(ubicacion)?.groupValues?.get(1)?.toDoubleOrNull()

        if (lat == null || lon == null) {
            Log.e("API", "Latitud o longitud no válida en la ubicación: $ubicacion")
            return
        }

        val payload = EventoPayload(
            evento = evento,
            latitud = lat,
            longitud = lon
        )

        val gson = Gson()
        val json = gson.toJson(payload)

        Log.d("API", "Simulando JSON a enviar: $json")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Thread.sleep(500)
                Log.d("API", "Simulación de envío completada exitosamente")
            } catch (e: InterruptedException) {
                Log.e("API", "Simulación interrumpida: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
        intent.putExtra("mensaje", mensaje)
        sendBroadcast(intent)

        bufferDatos.add(mensaje)

        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
            rvDatos.scrollToPosition(datosRecibidos.size - 1)
        }
    }

    private suspend fun insertarBufferEnDB(): Boolean {
        if (bufferDatos.isEmpty()) return true
        return try {
            val copiaBuffer = bufferDatos.toList()
            val listaEntidad = copiaBuffer.map { DatoEntity(mensaje = it) }
            datoDao.insertarDatos(listaEntidad)
            bufferDatos.clear()
            Log.d("DB", "Inserción batch exitosa, ${listaEntidad.size} elementos guardados")
            true
        } catch (e: Exception) {
            Log.e("DB", "Error al insertar datos: ${e.message}")
            false
        }
    }

    private fun cargarDatosDesdeBD() {
        lifecycleScope.launch(Dispatchers.IO) {
            val datosEnBD = datoDao.obtenerDatos()
            val mensajes = datosEnBD.map { it.mensaje }
            runOnUiThread {
                datosRecibidos.clear()
                datosRecibidos.addAll(mensajes)
                adapter.notifyDataSetChanged()
                if (mensajes.isNotEmpty()) {
                    rvDatos.scrollToPosition(mensajes.size - 1)
                }
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            insertarBufferEnDB()
        }
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
