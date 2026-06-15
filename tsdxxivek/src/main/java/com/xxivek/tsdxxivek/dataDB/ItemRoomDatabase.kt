package com.xxivek.tsdxxivek.dataDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xxivek.tsdxxivek.utilAPP.appendLog

/**
 * Класс базы данных с одноэлементным объектом INSTANCE.
 */

//То, что мы делаем здесь, выше abstract class ItemRoomDatabase : RoomDatabase() {
// мы добавляем базу данных, установив сущность Item внутри массива
// (вы можете добавить несколько сущностей),
// номер версии (вы должны увеличить число при внесении изменений в схему базы данных
// при следующем обновлении приложения) и exportSchema набор true.
//Примечание: Вы должны установить exportScheme в значение true,
// чтобы иметь возможность использовать автоматическую миграцию в будущих обновлениях.
//Внутри abstract class мы создаем экземпляр ItemRoomDatabase и устанавливаем его как volatile
// с помощью аннотации @Volatile, что означает, что результаты будут видны другим потокам.

@Database(entities = [Item::class], version = 2, exportSchema = true)
// Класс базы данных ItemRoomDatabase
abstract class ItemRoomDatabase:RoomDatabase() {
    //Определяем Dao для базы данных
    abstract fun itemDao(): ItemDao

    // Внутри abstract class мы создаем экземпляр ItemRoomDatabase и устанавливаем его как volatile
    // с помощью аннотации @Volatile, что означает, что результаты будут видны другим потокам.
    companion object {
        @Volatile
        private var INSTANCE: ItemRoomDatabase? = null

        fun getDatabase(context: Context): ItemRoomDatabase {
            // Если INSTANCE не null, тогда возвращаем ее,
            // иначе, создаем базу данных
            INSTANCE ?: synchronized(this) {
                       INSTANCE = buildDatabase(context)
                    }
            // Возвращаем базу данных.
            return INSTANCE!!
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Следующий запрос добавит новый столбец с именем lastUpdate таблицы item
                // в базу данных
                //database.execSQL("ALTER TABLE item ADD COLUMN lastUpdate INTEGER NOT NULL DEFAULT 0")
            }
        }

//        private fun buildDatabase(context: Context): ItemRoomDatabase {
//            //Создаем базу данных с именем item_database
//            return Room.databaseBuilder(
//                context.applicationContext,
//                ItemRoomDatabase::class.java,
//                "item_database"
//            )
//                    // Если нужна миграция
//                //.addMigrations(MIGRATION_1_2)
//                .build()
//        }
        private fun buildDatabase(context: Context): ItemRoomDatabase {
            //Создаем базу данных с именем item_database в памяти
//            return Room.inMemoryDatabaseBuilder(
//                context.applicationContext,
//                ItemRoomDatabase::class.java
//            )
            //Создаем базу данных с именем item_database на устройстве
            appendLog("Создание класса БД","Создаем базу на диске - item_database")
            return Room.databaseBuilder(
                context.applicationContext,
                ItemRoomDatabase::class.java,
                "item_database"
            )
                // Если нужна миграция
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}