package com.example.appprevent
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DatoDao {

    @Insert
    suspend fun insertarDato(dato: DatoEntity)

    @Insert
    suspend fun insertarDatos(datos: List<DatoEntity>)  // inserción múltiple

    @Query("SELECT * FROM datos")
    suspend fun obtenerDatos(): List<DatoEntity>
}
// comentario para subir