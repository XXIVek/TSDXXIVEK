package com.xxivek.tsdxxivek.dataDB

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс доступа к базе данных
 */

@Dao
interface ItemDao {

    //Room также поддерживает Kotlin Flow, который может автоматически обновлять
    // пользовательский интерфейс каждый раз, когда мы вносим изменения в список.
    // Другими словами, мы наблюдаем за изменениями.
    //Мы используем его здесь, обернув Item, например: Flow<List<Item>>.

    @Query("SELECT * from item ORDER BY name ASC")
    fun getItems(): Flow<List<Item>>

    @Query("SELECT * from item WHERE shtrihkod = :itemshtrih")
    fun getItem(itemshtrih: String): Flow<Item>
    @Query("SELECT * from item WHERE shtrihkod = :itemshtrih")
    fun getItem2(itemshtrih: String): Item

    @Query("SELECT * from item WHERE quantity>0")
    fun getItemNotEmpty(): Flow<List<Item>>
    @Query("SELECT * from item WHERE quantity>0")
    fun getItemNotEmpty2(): List<Item>

    @Query("SELECT * from item WHERE quantity<>quantityInStock")
    fun getItemEmpty(): Flow<List<Item>>
    @Query("SELECT * from item WHERE quantity<>quantityInStock")
    fun getItemEmpty2(): List<Item>

    @Query("SELECT COUNT(shtrihkod) FROM item")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(shtrihkod) FROM item WHERE quantity>0")
    suspend fun getCountNotEmpty(): Int

    // Укажите стратегию конфликта как ИГНОРИРОВАТЬ,
    // когда пользователь пытается добавить существующий элемент в базу данных.
    // suspend - Чтобы создать функцию приостановки. Используется в корутинах.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(itemList: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)
}