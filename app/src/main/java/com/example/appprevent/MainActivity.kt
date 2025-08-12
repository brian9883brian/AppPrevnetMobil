// MainActivity.kt
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
import kotlinx.coroutines.tasks.await // 📌 Import necesario para usar await()
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.gson.Gson

// Estructura nueva para el buffer
data class RegistroBuffer(
    val dato: String,
    val ubicacion: String?,
    val guid: String
)

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
    private var contadorVelocidadSuperada = 0
    private lateinit var db: AppDatabase
    private lateinit var datoDao: DatoDao

    private lateinit var tvNombreUsuario: TextView

    private val bufferDatos = mutableListOf<RegistroBuffer>()

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
        val btnIniciar = findViewById<MaterialButton>(R.id.btnIniciar)
        btnIniciar.setOnClickListener {
            enviarMensajeStart()
            Toast.makeText(this, "Se ha enviado señal para iniciar", Toast.LENGTH_SHORT).show()
        }

        btnBano.setOnClickListener { manejarClickBoton(btnBano.text.toString()) }
        btnTerminar.setOnClickListener { manejarClickBoton(btnTerminar.text.toString()) }
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

    private fun enviarMensajeDetener() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodeList = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                for (node in nodeList) {
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/detener_envio", "STOP".toByteArray())
                        .await()
                    Log.d("Wear", "Mensaje DETENER enviado a ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e("Wear", "Error enviando mensaje DETENER: ${e.message}")
            }
        }
    }
    fun enviarMensajeStart() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                Log.d("PhoneApp", "Nodos conectados: ${nodes.map { it.displayName }}")
                for (node in nodes) {
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/detener_envio", "START".toByteArray())
                        .await()
                    Log.d("PhoneApp", "Mensaje START enviado a ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e("PhoneApp", "Error enviando mensaje START: ${e.message}")
            }
        }
    }



    private fun crearRegistroDesdeTexto(texto: String, ubicacionActual: String?): RegistroBuffer {
        // Regex para caídas
        val caidaRegex = Regex("(POSIBLE CAIDA|CAIDA) en lat:([\\d.-]+), lon:([\\d.-]+)", RegexOption.IGNORE_CASE)
        // Regex para velocidad
        val velocidadRegex = Regex("(VELOCIDAD SUPERADA) en lat:([\\d.-]+), lon:([\\d.-]+)", RegexOption.IGNORE_CASE)

        val matchCaida = caidaRegex.find(texto)
        val matchVelocidad = velocidadRegex.find(texto)

        return when {
            matchCaida != null -> {
                val tipoCaida = matchCaida.groupValues[1].uppercase()
                val lat = matchCaida.groupValues[2]
                val lon = matchCaida.groupValues[3]
                RegistroBuffer(
                    dato = tipoCaida,
                    ubicacion = "lat=$lat, lon=$lon",
                    guid = guid
                )
            }
            matchVelocidad != null -> {
                val tipoEvento = matchVelocidad.groupValues[1].uppercase()
                val lat = matchVelocidad.groupValues[2]
                val lon = matchVelocidad.groupValues[3]
                RegistroBuffer(
                    dato = tipoEvento,
                    ubicacion = "lat=$lat, lon=$lon",
                    guid = guid
                )
            }
            else -> {
                RegistroBuffer(
                    dato = texto,
                    ubicacion = ubicacionActual,
                    guid = guid
                )
            }
        }
    }


    private fun manejarClickBoton(buttonText: String) {
        Toast.makeText(this, buttonText, Toast.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val ubicacionStr = location?.let { "lat=${it.latitude}, lon=${it.longitude}" }

            val registro = crearRegistroDesdeTexto(buttonText, ubicacionStr)

            lifecycleScope.launch(Dispatchers.IO) {
                // 1. Agregar registro al buffer y a la BD local
                bufferDatos.add(registro)
                insertarBufferEnDB()

                try {
                    datoDao.insertarDato(DatoEntity(mensaje = registro.dato))
                } catch (e: Exception) {
                    Log.e("DB", "Error al insertar dato del botón: ${e.message}")
                }

                // 2. Enviar datos al API
                enviarDatosAlAPI(listOf(registro))

                // 3. Enviar mensaje detener al reloj
                enviarMensajeDetener()

                // 4. Mostrar Toast en UI thread
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                }
            }

            runOnUiThread {
                datosRecibidos.clear()
                datosRecibidos.add(buttonText)
                adapter.notifyDataSetChanged()
                rvDatos.scrollToPosition(datosRecibidos.size - 1)

                val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
                intent.putExtra("mensaje", buttonText)
                sendBroadcast(intent)
            }
        }
    }


    private fun enviarDatosAlAPI(datos: List<RegistroBuffer>) {
        val gson = Gson()
        val json = gson.toJson(datos)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://lanube-280581492272.us-central1.run.app/api/alertas/integradas")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    val input = json.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    Log.d("API", "Datos enviados correctamente")
                    limpiarBufferCompleto()
                } else {
                    Log.e("API", "Error al enviar datos, código: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("API", "Excepción al enviar datos: ${e.message}")
            }
        }
    }

    private fun limpiarBufferCompleto() {
        lifecycleScope.launch(Dispatchers.IO) {
            bufferDatos.clear()
            datoDao.deleteAll()
            Log.d("DB", "Buffer y base de datos limpiados correctamente")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Buffer y base de datos limpiados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val mensaje = String(event.data)
        Log.d("PhoneApp", "Mensaje recibido: $mensaje")

        val intent = Intent("com.example.appprevent.ACTUALIZAR_HISTORICO")
        intent.putExtra("mensaje", mensaje)
        sendBroadcast(intent)

        val registro = crearRegistroDesdeTexto(mensaje, null)
        bufferDatos.add(registro)

        // 📌 Lógica para eventos
        when {
            mensaje.contains("CAIDA", ignoreCase = true) -> {
                Log.d("PhoneApp", "CAIDA detectada, enviando datos a la API inmediatamente...")
                lifecycleScope.launch(Dispatchers.IO) {
                    enviarDatosAlAPI(bufferDatos.toList())
                    contadorVelocidadSuperada = 0 // Reiniciar contador
                }
            }

            mensaje.contains("VELOCIDAD SUPERADA", ignoreCase = true) ||
                    mensaje.contains("POSIBLE CAIDA", ignoreCase = true) -> {
                contadorVelocidadSuperada++
                Log.d("PhoneApp", "Contador eventos críticos: $contadorVelocidadSuperada")

                if (contadorVelocidadSuperada >= 5) {
                    Log.d("PhoneApp", "Se alcanzaron 5 eventos, enviando datos a la API...")
                    lifecycleScope.launch(Dispatchers.IO) {
                        enviarDatosAlAPI(bufferDatos.toList())
                        contadorVelocidadSuperada = 0
                    }
                }
            }
        }

        runOnUiThread {
            datosRecibidos.add(mensaje)
            adapter.notifyItemInserted(datosRecibidos.size - 1)
            rvDatos.scrollToPosition(datosRecibidos.size - 1)
        }
    }



    private suspend fun insertarBufferEnDB(): Boolean {
        if (bufferDatos.isEmpty()) return true
        return try {
            val listaEntidad = bufferDatos.map { DatoEntity(mensaje = it.dato) }
            datoDao.insertarDatos(listaEntidad)
            bufferDatos.clear()
            Log.d("DB", "Inserción batch exitosa")
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
        lifecycleScope.launch(Dispatchers.IO) { insertarBufferEnDB() }
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }
}
