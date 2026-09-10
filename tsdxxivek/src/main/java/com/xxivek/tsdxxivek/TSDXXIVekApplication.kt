package com.xxivek.tsdxxivek

import android.app.Application
import com.xxivek.tsdxxivek.dataDB.ItemRoomDatabase

/**
 * Главный класс приложения. Содержит:
 * - singleton AppState для глобального состояния
 * - Room database
 */
class TSDXXIVekApplication: Application() {
    
    companion object {
        @Volatile
        private var INSTANCE: TSDXXIVekApplication? = null

        val instance: TSDXXIVekApplication?
            get() = INSTANCE
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    // Singleton для глобального состояния приложения
    val appState = AppState()
    
    //Использование с помощью lazy, поэтому база данных и репозиторий создаются только тогда,
    // когда они необходимы, а не при запуске приложения
    val database: ItemRoomDatabase by lazy { ItemRoomDatabase.getDatabase(this) }
    
    /**
     * Получить DAO интерфейс для работы с базой данных
     */
    fun getItemDao() = database.itemDao()
}