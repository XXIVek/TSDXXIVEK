package com.xxivek.tsdxxivek

import android.app.Application
import com.xxivek.tsdxxivek.dataDB.ItemRoomDatabase

class TSDXXIVekApplication: Application() {
    //Использование с помощью lazy, поэтому база данных и репозиторий создаются только тогда,
    // когда они необходимы, а не при запуске приложения
    val database: ItemRoomDatabase by lazy { ItemRoomDatabase.getDatabase(this) }
}