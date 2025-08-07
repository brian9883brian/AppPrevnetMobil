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
// Limpiar el buffer al iniciar sesión

        cargarDatosDesdeBD()

        val btnBano = findViewById<MaterialButton>(R.id.btnBano)
        val btnTerminar = findViewById<MaterialButton>(R.id.btnTerminar)
        val btnComida = findViewById<MaterialButton>(R.id.btnComida)

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

    private fun crearRegistroDesdeTexto(texto: String, ubicacionActual: String?): RegistroBuffer {
        val caidaRegex = Regex("(POSIBLE CAIDA|CAIDA) en lat:([\\d.-]+), lon:([\\d.-]+)", RegexOption.IGNORE_CASE)
        val match = caidaRegex.find(texto)
        return if (match != null) {
            val tipoCaida = match.groupValues[1].uppercase()
            val lat = match.groupValues[2]
            val lon = match.groupValues[3]
            RegistroBuffer(
                dato = tipoCaida,
                ubicacion = "lat=$lat, lon=$lon",
                guid = guid
            )
        } else {
            RegistroBuffer(
                dato = texto,
                ubicacion = ubicacionActual,
                guid = guid
            )
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
                bufferDatos.add(registro)
                val copiaBuffer = bufferDatos.toList()
                insertarBufferEnDB()

                try {
                    datoDao.insertarDato(DatoEntity(mensaje = registro.dato))
                } catch (e: Exception) {
                    Log.e("DB", "Error al insertar dato del botón: ${e.message}")
                }

                Log.d("API", "Buffer antes de enviar: $copiaBuffer")
                enviarDatosAlAPI(copiaBuffer)

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
        Log.d("API", "Enviando datos: $json")

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
                    Log.d("API", "Datos enviados correctamente, código: $responseCode")
                } else {
                    Log.e("API", "Error al enviar datos, código: $responseCode")
                }
                limpiarBufferCompleto()
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

        val registro = crearRegistroDesdeTexto(mensaje, null) // No ubicación para mensajes wearable
        bufferDatos.add(registro)

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
