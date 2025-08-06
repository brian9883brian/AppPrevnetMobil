package com.example.appprevent
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [DatoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun datoDao(): DatoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mi_basededatos"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
// comentario para subir xd